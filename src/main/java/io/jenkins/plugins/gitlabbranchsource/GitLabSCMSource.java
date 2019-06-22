package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabLink;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.traits.GitBrowserSCMSourceTrait;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMProbe;
import jenkins.scm.api.SCMProbeStat;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMTraitDescriptor;
import jenkins.scm.impl.ChangeRequestSCMHeadCategory;
import jenkins.scm.impl.UncategorizedSCMHeadCategory;
import jenkins.scm.impl.form.NamedArrayList;
import jenkins.scm.impl.trait.Discovery;
import jenkins.scm.impl.trait.Selection;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.Visibility;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;

public class GitLabSCMSource extends AbstractGitSCMSource {
    private final String serverUrl;
    private final String projectOwner;
    private final String project;
    private String credentialsId;
    private List<SCMSourceTrait> traits = new ArrayList<>();
    private transient String sshRemote;
    private transient Project gitlabProject;

    @DataBoundConstructor
    public GitLabSCMSource(String serverUrl, String projectOwner, String project) {
        this.serverUrl = serverUrl;
        this.projectOwner = projectOwner;
        this.project = project;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public String getProject() {
        return project;
    }

    public String getSshRemote() {
        return sshRemote;
    }

    public void setSshRemote(String sshRemote) {
        this.sshRemote = sshRemote;
    }

    @Override
    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Override
    public String getRemote() {
        return GitLabSCMBuilder.checkoutUriTemplate(getOwner(), serverUrl, getSshRemote(), getCredentialsId())
                .set("owner", projectOwner).set("project", project).expand();
    }

    @NonNull
    @Override
    public List<SCMSourceTrait> getTraits() {
        return Collections.unmodifiableList(traits);
    }

    @DataBoundSetter
    public void setTraits(List<SCMSourceTrait> traits) {
        this.traits = new ArrayList<>(Util.fixNull(traits));
    }

    @Override
    protected SCMRevision retrieve(@NonNull SCMHead head, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        try {
            GitLabApi gitLabApi = apiBuilder();
            // To verify the supplied PAT is valid
            if(!gitLabApi.getAuthToken().equals("")) {
                gitLabApi.getUserApi().getCurrentUser();
            }
            // Tests if GitLab API is valid
            gitLabApi.getProjectApi().getProjects(1, 1);
            gitlabProject = gitLabApi.getProjectApi().getProject(projectOwner+"/"+project);
            if(head instanceof BranchSCMHead) {
                listener.getLogger().format("Querying the current revision of branch %s...%n", head.getName());
                String revision = gitLabApi.getRepositoryApi().getBranch(gitlabProject, head.getName()).getCommit().getId();
                listener.getLogger().format("Current revision of branch %s is %s%n", head.getName(), revision);
                return new BranchSCMRevision((BranchSCMHead) head, revision);
            } else if(head instanceof MergeRequestSCMHead) {
                MergeRequestSCMHead h = (MergeRequestSCMHead) head;
                listener.getLogger().format("Querying the current revision of merge request #%s...%n", h.getId());
                MergeRequest mr =
                        gitLabApi.getMergeRequestApi().getMergeRequest(gitlabProject, Integer.parseInt(h.getId()));
                if(mr.getState().equals(Constants.MergeRequestState.OPENED.toString())) {
                    listener.getLogger().format("Current revision of merge request #%s is %s%n",
                            h.getId(), mr.getDiffRefs().getHeadSha());
                    return new MergeRequestSCMRevision(
                            h,
                            new BranchSCMRevision(
                                    h.getTarget(),
                                    mr.getDiffRefs().getBaseSha()
                            ),
                            new BranchSCMRevision(
                                    new BranchSCMHead(h.getOriginName()),
                                    mr.getDiffRefs().getHeadSha()
                            )
                    );
                } else {
                    listener.getLogger().format("Merge request #%s is CLOSED%n", h.getId());
                    return null;
                }
            } else {
                listener.getLogger().format("Unknown head: %s of type %s%n", head.getName(), head.getClass().getName());
                return null;
            }
        } catch (GitLabApiException e) {
            e.printStackTrace();
        }
        return super.retrieve(head, listener);
    }

    @Override
    protected void retrieve(SCMSourceCriteria criteria, @NonNull SCMHeadObserver observer, SCMHeadEvent<?> event,
                            @NonNull TaskListener listener) throws IOException, InterruptedException {
        try {
            GitLabApi gitLabApi = apiBuilder();
            // To verify the supplied PAT is valid
            if (!gitLabApi.getAuthToken().equals("")) {
                gitLabApi.getUserApi().getCurrentUser();
            }
            gitLabApi.getProjectApi().getProjects(1, 1);
            listener.getLogger().format("Looking up project %s/%s%n", projectOwner, project);
            // as "path to the project" is required
            gitlabProject = gitLabApi.getProjectApi().getProject(projectOwner+"/"+project);
            sshRemote = gitlabProject.getSshUrlToRepo();
            try (GitLabSCMSourceRequest request = new GitLabSCMSourceContext(criteria, observer)
                    .withTraits(getTraits())
                    .newRequest(this, listener)) {
                request.setGitLabApi(gitLabApi);
                if (request.isFetchBranches()) {
                    request.setBranches(gitLabApi.getRepositoryApi().getBranches(gitlabProject));
                }
                if (request.isFetchMRs()) {
                    // If not authenticated GitLabApi cannot detect if it is a fork
                    // If `forkedFromProject` is null it doesn't mean anything
                    if (gitlabProject.getForkedFromProject() == null) {
                        listener.getLogger()
                                .format("%n  Unable to detect if it is a mirror or not still fetching MRs anyway...%n");
                        // TODO Fix this: cannot call getDiffRefs on requests since it will always return null
                        request.setMergeRequests(gitLabApi.getMergeRequestApi().getMergeRequests(gitlabProject));
                    }
                    else {
                        listener.getLogger().format("%n  Ignoring merge requests as project is a mirror...%n");
                    }
                }
                // TODO if (request.isFetchTags()) { ... }

                if (request.isFetchBranches()) {
                    int count = 0;
                    listener.getLogger().format("%n Checking branches.. %n");
                    for (final Branch b : gitLabApi.getRepositoryApi().getBranches(gitlabProject)) {
                        count++;
                        listener.getLogger().format("%n Checking branch %s%n",
                                HyperlinkNote.encodeTo(
                                        UriTemplate.buildFromTemplate(gitlabProject.getWebUrl())
                                                .literal("/tree")
                                                .path("branch")
                                                .build()
                                                .set("branch", b.getName())
                                                .expand(),
                                        b.getName()
                                )
                        );
                        if (request.process(new BranchSCMHead(b.getName()),
                                new SCMSourceRequest.RevisionLambda<BranchSCMHead, BranchSCMRevision>() {
                                    @NonNull
                                    @Override
                                    public BranchSCMRevision create(@NonNull BranchSCMHead head)
                                            throws IOException, InterruptedException {
                                        return new BranchSCMRevision(head, b.getCommit().getId());
                                    }
                                }, new SCMSourceRequest.ProbeLambda<BranchSCMHead, BranchSCMRevision>() {
                                    @NonNull
                                    @Override
                                    public SCMSourceCriteria.Probe create(@NonNull BranchSCMHead head,
                                                                          @Nullable BranchSCMRevision revision)
                                            throws IOException, InterruptedException {
                                        return createProbe(head, revision);
                                    }
                                }, new SCMSourceRequest.Witness() {
                                    @Override
                                    public void record(@NonNull SCMHead head, SCMRevision revision, boolean isMatch) {
                                        if (isMatch) {
                                            listener.getLogger().format(" Met criteria%n");
                                        } else {
                                            listener.getLogger().format(" Does not meet criteria%n");
                                        }
                                    }
                                })) {
                            listener.getLogger().format("%n  %d branches were processed (query completed)%n", count);
                            return;
                        }
                    }
                    listener.getLogger().format("%n  %d branches were processed%n", count);
                }
                if(request.isFetchMRs() && gitlabProject.getForkedFromProject() == null && !(request.getForkMRStrategies().isEmpty()
                        && request.getOriginMRStrategies().isEmpty())) {
                    int count = 0;
                    listener.getLogger().format("%n  Checking merge requests...%n");
                    List<MergeRequest> mrs = gitLabApi.getMergeRequestApi().getMergeRequests(gitlabProject, Constants.MergeRequestState.OPENED);
                    for (MergeRequest mr : mrs) {
                        // Since by default GitLab4j do not populate DiffRefs for a list of Merge Requests
                        // It is required to get the individual diffRef using the Iid.
                        final MergeRequest m = gitLabApi.getMergeRequestApi().getMergeRequest(gitlabProject, mr.getIid());
                        count++;
                        listener.getLogger().format("%n  Checking pull request %s%n",
                                HyperlinkNote.encodeTo(
                                        UriTemplate.buildFromTemplate(gitlabProject.getWebUrl())
                                                .literal("/merge_requests")
                                                .path("iid")
                                                .build()
                                                .set("iid", m.getIid())
                                                .expand(),
                                        "!" + m.getIid()
                                )
                        );
                        String originOwner = m.getAuthor().getUsername();
                        // Origin project name will always the same as the source project name
                        String originProject = project;
                        Set<ChangeRequestCheckoutStrategy> strategies = request.getMRStrategies(
                                projectOwner.equalsIgnoreCase(originOwner)
                                        && project.equalsIgnoreCase(originProject)
                        );
                        for (ChangeRequestCheckoutStrategy strategy : strategies) {
                            if (request.process(new MergeRequestSCMHead(
                                            "MR-" + m.getIid() + (strategies.size() > 1 ? "-" + strategy.name()
                                                    .toLowerCase(Locale.ENGLISH) : ""),
                                            m.getIid(),
                                            new BranchSCMHead(m.getSourceBranch()),
                                            ChangeRequestCheckoutStrategy.MERGE,
                                            originOwner.equalsIgnoreCase(projectOwner) && originProject
                                                    .equalsIgnoreCase(project)
                                                    ? SCMHeadOrigin.DEFAULT
                                                    : new SCMHeadOrigin.Fork(originOwner + "/" + originProject),
                                            originOwner,
                                            originProject,
                                            m.getTargetBranch()),
                                    new SCMSourceRequest.RevisionLambda<MergeRequestSCMHead, MergeRequestSCMRevision>() {
                                        @NonNull
                                        @Override
                                        public MergeRequestSCMRevision create(@NonNull MergeRequestSCMHead head)
                                                throws IOException, InterruptedException {
                                            return new MergeRequestSCMRevision(
                                                    head,
                                                    new BranchSCMRevision(
                                                            head.getTarget(),
                                                            m.getDiffRefs().getBaseSha()
                                                    ),
                                                    new BranchSCMRevision(
                                                            new BranchSCMHead(head.getOriginName()),
                                                            m.getDiffRefs().getHeadSha()
                                                    )
                                            );
                                        }
                                    },
                                    new SCMSourceRequest.ProbeLambda<MergeRequestSCMHead, MergeRequestSCMRevision>() {
                                        @NonNull
                                        @Override
                                        public SCMSourceCriteria.Probe create(@NonNull MergeRequestSCMHead h,
                                                                              @Nullable MergeRequestSCMRevision r)
                                                throws IOException, InterruptedException {
                                            return createProbe(h, r);
                                        }
                                    }, new SCMSourceRequest.Witness() {
                                        @Override
                                        public void record(@NonNull SCMHead head, SCMRevision revision,
                                                           boolean isMatch) {
                                            if (isMatch) {
                                                listener.getLogger().format(" Met criteria%n");
                                            } else {
                                                listener.getLogger().format(" Does not meet criteria%n");
                                            }
                                        }
                                    }
                            )) {
                                listener.getLogger()
                                        .format("%n  %d pull requests were processed (query completed)%n", count);
                                return;
                            }

                        }
                    }
                    listener.getLogger().format("%n  %d pull requests were processed%n", count);
                }
            }
        } catch (GitLabApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected SCMRevision retrieve(@NonNull String thingName, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        SCMHeadObserver.Named baptist = SCMHeadObserver.named(thingName);
        retrieve(null, baptist, null, listener);
        return baptist.result();
    }

    @NonNull
    @Override
    protected Set<String> retrieveRevisions(@NonNull TaskListener listener) throws IOException, InterruptedException {
        // don't pass through to git, instead use the super.super behaviour
        Set<String> revisions = new HashSet<String>();
        for (SCMHead head : retrieve(listener)) {
            revisions.add(head.getName());
        }
        return revisions;
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(SCMSourceEvent event, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        if (gitlabProject == null) {
            try {
                GitLabApi gitLabApi = apiBuilder();
                listener.getLogger().format("Looking up repository %s/%s%n", projectOwner, project);
                gitlabProject = gitLabApi.getProjectApi().getProject(projectOwner+"/"+project);
            } catch (GitLabApiException e) {
                e.printStackTrace();
            }
        }
        List<Action> result = new ArrayList<>();
        result.add(new ObjectMetadataAction(null, gitlabProject.getDescription(), gitlabProject.getWebUrl()));
        result.add(new GitLabLink("icon-project", UriTemplate.buildFromTemplate(serverUrl)
                .path(UriTemplateBuilder.var("owner"))
                .path(UriTemplateBuilder.var("project"))
                .build()
                .set("owner", projectOwner)
                .set("project", project)
                .expand()
        ));
        return result;
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMHead head, SCMHeadEvent event, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        if (gitlabProject == null) {
            try {
                GitLabApi gitLabApi = apiBuilder();
                listener.getLogger().format("Looking up repository %s/%s%n", projectOwner, project);
                gitlabProject = gitLabApi.getProjectApi().getProject(projectOwner+"/"+project);
            } catch (GitLabApiException e) {
                e.printStackTrace();
            }
        }
        List<Action> result = new ArrayList<>();
        if (head instanceof BranchSCMHead) {
            String branchUrl = UriTemplate.buildFromTemplate(serverUrl)
                    .path(UriTemplateBuilder.var("owner"))
                    .path(UriTemplateBuilder.var("project"))
                    .path("tree")
                    .path(UriTemplateBuilder.var("branch"))
                    .build()
                    .set("owner", projectOwner)
                    .set("project", project)
                    .set("branch", head.getName())
                    .expand();
            result.add(new ObjectMetadataAction(
                    null,
                    null,
                    branchUrl
            ));
            result.add(new GitLabLink("icon-branch", branchUrl));
            if (head.getName().equals(gitlabProject.getDefaultBranch())) {
                result.add(new PrimaryInstanceMetadataAction());
            }
        } else if (head instanceof MergeRequestSCMHead) {
            String mergeUrl = UriTemplate.buildFromTemplate(serverUrl)
                    .path(UriTemplateBuilder.var("owner"))
                    .path(UriTemplateBuilder.var("project"))
                    .path("merge_requests")
                    .path(UriTemplateBuilder.var("iid"))
                    .build()
                    .set("owner", projectOwner)
                    .set("project", project)
                    .set("iid", ((MergeRequestSCMHead) head).getId())
                    .expand();
            result.add(new ObjectMetadataAction(
                    null,
                    null,
                    mergeUrl
            ));
            result.add(new GitLabLink("icon-branch", mergeUrl));
        }
        return result;
    }

    @NonNull
    @Override
    public SCM build(@NonNull SCMHead head, SCMRevision revision) {
        return new GitLabSCMBuilder(this, head, revision).withTraits(traits).build();
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMRevision revision, SCMHeadEvent event,
                                           @NonNull TaskListener listener) throws IOException, InterruptedException {
        return super.retrieveActions(revision, event, listener);
    }

    @NonNull
    @Override
    protected SCMProbe createProbe(@NonNull final SCMHead head, SCMRevision revision) throws IOException {
        try {
            GitLabSCMFileSystem.BuilderImpl builder =
                    ExtensionList.lookup(SCMFileSystem.Builder.class).get(GitLabSCMFileSystem.BuilderImpl.class);
            if (builder == null) {
                throw new AssertionError();
            }
            final SCMFileSystem fs = builder.build(this, head, revision);
            return new SCMProbe() {
                @NonNull
                @Override
                public SCMProbeStat stat(@NonNull String path) throws IOException {
                    try {
                        return SCMProbeStat.fromType(fs.child(path).getType());
                    } catch (InterruptedException e) {
                        throw new IOException("Interrupted", e);
                    }
                }

                @Override
                public void close() throws IOException {
                    Objects.requireNonNull(fs).close();
                }

                @Override
                public String name() {
                    return head.getName();
                }

                @Override
                public long lastModified() {
                    try {
                        return fs != null ? fs.lastModified() : 0;
                    } catch (IOException | InterruptedException e) {
                        return 0L;
                    }
                }

                @Override
                public SCMFile getRoot() {
                    return fs != null ? fs.getRoot() : null;
                }
            };
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void afterSave() {
        WebhookRegistration mode = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits(new GitLabSCMNavigatorContext().withTraits(traits).traits())
                .webhookRegistration();
        GitLabWebhookListener.register(getOwner(), this, mode, credentialsId);
    }

    private GitLabApi apiBuilder() throws AbortException {
        GitLabServer server = GitLabServers.get().findServer(serverUrl);
        if (server == null) {
            throw new AbortException("Unknown server: " + serverUrl);
        }
        PersonalAccessToken credentials = credentials();
        SCMSourceOwner owner = getOwner();
        if (owner != null) {
            CredentialsProvider.track(owner, credentials);
        }
        if(credentials != null) {
            return new GitLabApi(serverUrl, credentials.getToken().getPlainText());
        }
        return new GitLabApi(serverUrl, "");
    }

    public PersonalAccessToken credentials() {
        return CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        PersonalAccessToken.class,
                        getOwner(),
                        Jenkins.getAuthentication(),
                        fromUri(serverUrl).build()),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        static {
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab icon-sm",
                            "plugin/gitlab-scm/images/16x16/gitlab.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab icon-md",
                            "plugin/gitlab-scm/images/24x24/gitlab.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab icon-lg",
                            "plugin/gitlab-scm/images/32x32/gitlab.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab icon-xlg",
                            "plugin/gitlab-scm/images/48x48/gitlab.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-logo icon-sm",
                            "plugin/gitlab-scm/images/16x16/gitlab.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-logo icon-md",
                            "plugin/gitlab-scm/images/24x24/gitlab.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-logo icon-lg",
                            "plugin/gitlab-scm/images/32x32/gitlab.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-logo icon-xlg",
                            "plugin/gitlab-scm/images/48x48/gitlab.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-project icon-sm",
                            "plugin/gitlab-scm/images/16x16/gitlab-project.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-project icon-md",
                            "plugin/gitlab-scm/images/24x24/gitlab-project.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-project icon-lg",
                            "plugin/gitlab-scm/images/32x32/gitlab-project.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-project icon-xlg",
                            "plugin/gitlab-scm/images/48x48/gitlab-project.png",
                            Icon.ICON_XLARGE_STYLE));

            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-branch icon-sm",
                            "plugin/gitlab-scm/images/16x16/gitlab-branch.png",
                            Icon.ICON_SMALL_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-branch icon-md",
                            "plugin/gitlab-scm/images/24x24/gitlab-branch.png",
                            Icon.ICON_MEDIUM_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-branch icon-lg",
                            "plugin/gitlab-scm/images/32x32/gitlab-branch.png",
                            Icon.ICON_LARGE_STYLE));
            IconSet.icons.addIcon(
                    new Icon("icon-gitlab-branch icon-xlg",
                            "plugin/gitlab-scm/images/48x48/gitlab-branch.png",
                            Icon.ICON_XLARGE_STYLE));
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "GitLab";
        }

        public ListBoxModel doFillServerUrlItems(@AncestorInPath SCMSourceOwner context,
                                                 @QueryParameter String serverUrl) {
            if (context == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    // must have admin if you want the list without a context
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverUrl);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)) {
                    // must be able to read the configuration the list
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverUrl);
                    return result;
                }
            }
            return GitLabServers.get().getServerItems();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context,
                                                     @QueryParameter String serverUrl,
                                                     @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (context == null) {
                // must have admin if you want the list without a context
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)
                        && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    // must be able to read the configuration or use the item credentials if you want the list
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            }
            result.includeEmptyValue();
            result.includeMatchingAs(
                    context instanceof Queue.Task ?
                            Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                            : ACL.SYSTEM,
                    context,
                    StandardCredentials.class,
                    URIRequirementBuilder.fromUri(serverUrl).build(),
                    CredentialsMatchers.allOf(
                            CredentialsMatchers.withId(credentialsId),
                            CredentialsMatchers.instanceOf(StandardCredentials.class)
                    )
            );
            return result;
        }

        public ListBoxModel doFillProjectItems(@AncestorInPath SCMSourceOwner context,
                                                  @QueryParameter String serverUrl,
                                                  @QueryParameter String credentialsId,
                                                  @QueryParameter String projectOwner,
                                                  @QueryParameter String project) {
            ListBoxModel result = new ListBoxModel();
            if (context == null) {
                // must have admin if you want the list without a context
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    result.add(project);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)
                        && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    // must be able to read the configuration or use the item credentials if you want the list
                    result.add(project);
                    return result;
                }
            }
            GitLabServer server = GitLabServers.get().findServer(serverUrl);
            if (server == null) {
                // you can only get the list for registered servers
                result.add(project);
                return result;
            }
            PersonalAccessToken credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            PersonalAccessToken.class,
                            context,
                            context instanceof Queue.Task ?
                                    Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                                    : ACL.SYSTEM,
                            URIRequirementBuilder.fromUri(serverUrl).build()),
                            CredentialsMatchers.withId(credentialsId)
            );
            try {
                GitLabApi gitLabApi;
                if(credentials != null) {
                     gitLabApi = new GitLabApi(serverUrl, credentials.getToken().getPlainText());
                } else {
                    gitLabApi =  new GitLabApi(serverUrl, "");
                }
                for (Project p : gitLabApi.getProjectApi().getUserProjects(projectOwner, new ProjectFilter().withVisibility(
                        Visibility.PUBLIC))) {
                    result.add(p.getName());
                }
                return result;
            } catch (GitLabApiException e) {
                e.printStackTrace();
                return new StandardListBoxModel()
                        .includeEmptyValue();
            }
        }

        public List<NamedArrayList<? extends SCMTraitDescriptor<?>>> getTraitsDescriptorLists() {
            //all.addAll(SCMSourceTrait._for(this, GitHubSCMSourceContext.class, null));
            List<SCMTraitDescriptor<?>> all = new ArrayList<>(SCMSourceTrait._for(this, null, GitLabSCMBuilder.class));
            Set<SCMTraitDescriptor<?>> dedup = new HashSet<>();
            for (Iterator<SCMTraitDescriptor<?>> iterator = all.iterator(); iterator.hasNext(); ) {
                SCMTraitDescriptor<?> d = iterator.next();
                if (dedup.contains(d)
                        || d instanceof GitBrowserSCMSourceTrait.DescriptorImpl) {
                    // remove any we have seen already and ban the browser configuration as it will always be github
                    iterator.remove();
                } else {
                    dedup.add(d);
                }
            }
            List<NamedArrayList<? extends SCMTraitDescriptor<?>>> result = new ArrayList<>();
            NamedArrayList.select(all, "Within repository", NamedArrayList
                            .anyOf(NamedArrayList.withAnnotation(Discovery.class),
                                    NamedArrayList.withAnnotation(Selection.class)),
                    true, result);
            NamedArrayList.select(all, "Additional", null, true, result);
            return result;
        }

        public List<SCMSourceTrait> getTraitsDefaults() {
            return Arrays.<SCMSourceTrait>asList( // TODO finalize
                    new BranchDiscoveryTrait(true, false),
                    new OriginMergeRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE)),
                    new ForkMergeRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE),
                            new ForkMergeRequestDiscoveryTrait.TrustContributors())
            );
        }

        @Override
        public String getIconClassName() {
            return "icon-gitlab-project";
        }

        @NonNull
        @Override
        protected SCMHeadCategory[] createCategories() {
            return new SCMHeadCategory[]{
                    new UncategorizedSCMHeadCategory(Messages._GitLabSCMSource_UncategorizedCategory()),
                    new ChangeRequestSCMHeadCategory(Messages._GitLabSCMSource_ChangeRequestCategory())
                    // TODO add support for tags and maybe feature branch identification
            };
        }
    }

}
