package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatar;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabLink;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitTagSCMRevision;
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
import jenkins.scm.api.metadata.ContributorMetadataAction;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMNavigatorTraitDescriptor;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMTraitDescriptor;
import jenkins.scm.impl.ChangeRequestSCMHeadCategory;
import jenkins.scm.impl.TagSCMHeadCategory;
import jenkins.scm.impl.UncategorizedSCMHeadCategory;
import jenkins.scm.impl.form.NamedArrayList;
import jenkins.scm.impl.trait.Discovery;
import jenkins.scm.impl.trait.Selection;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.Tag;
import org.jenkins.ui.icon.IconSpec;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.apiBuilder;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.branchUriTemplate;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.commitUriTemplate;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.getServerUrlFromName;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.mergeRequestUriTemplate;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.splitPath;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.tagUriTemplate;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabIcons.ICON_GITLAB;

public class GitLabSCMSource extends AbstractGitSCMSource {

    private static final Logger LOGGER = Logger.getLogger(GitLabSCMSource.class.getName());
    private final String serverName;
    private final String projectOwner;
    private final String projectPath;
    private String projectName;
    private String credentialsId;
    private List<SCMSourceTrait> traits = new ArrayList<>();
    private String sshRemote;
    private String httpRemote;
    private transient Project gitlabProject;
    private long projectId;

    /**
     * The cache of {@link ObjectMetadataAction} instances for each open MR.
     */
    @NonNull
    private transient /* effectively final */ Map<Long, ObjectMetadataAction> mergeRequestMetadataCache = new ConcurrentHashMap<>();
    /**
     * The cache of {@link ObjectMetadataAction} instances for each open MR.
     */
    @NonNull
    private transient /* effectively final */ Map<Long, ContributorMetadataAction> mergeRequestContributorCache = new ConcurrentHashMap<>();

    @DataBoundConstructor
    public GitLabSCMSource(String serverName, String projectOwner, String projectPath) {
        this.serverName = serverName;
        this.projectOwner = projectOwner;
        this.projectPath = projectPath;
    }

    public String getServerName() {
        return serverName;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getHttpRemote() {
        return httpRemote;
    }

    public void setHttpRemote(String httpRemote) {
        this.httpRemote = httpRemote;
    }

    public String getSshRemote() {
        return sshRemote;
    }

    public void setSshRemote(String sshRemote) {
        this.sshRemote = sshRemote;
    }

    public String getProjectName() {
        return projectName;
    }

    /**
     * Only used for saving project name for {@link GitLabSCMNavigator}
     *
     * @param projectName the name of the project
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
        return GitLabSCMBuilder.checkoutUriTemplate(getOwner(), getServerUrlFromName(serverName), getHttpRemote(),
                getSshRemote(), getCredentialsId(), projectPath).expand();
    }

    protected Project getGitlabProject() {
        if (gitlabProject == null) {
            getGitlabProject(apiBuilder(this.getOwner(), serverName));
        }
        return gitlabProject;
    }

    protected Project getGitlabProject(GitLabApi gitLabApi) {
        if (gitlabProject == null) {
            try {
                gitlabProject = gitLabApi.getProjectApi().getProject(projectPath);
                sshRemote = gitlabProject.getSshUrlToRepo();
                httpRemote = gitlabProject.getHttpUrlToRepo();
                projectId = gitlabProject.getId();
            } catch (GitLabApiException e) {
                throw new IllegalStateException("Failed to retrieve project " + projectPath, e);
            }
        }
        return gitlabProject;
    }

    // This method always returns the latest list of members of the project
    public HashMap<String, AccessLevel> getMembers() {
        HashMap<String, AccessLevel> members = new HashMap<>();
        try {
            GitLabApi gitLabApi = apiBuilder(this.getOwner(), serverName);
            for (Member m : gitLabApi.getProjectApi().getAllMembers(projectPath)) {
                members.put(m.getUsername(), m.getAccessLevel());
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING, "Exception while fetching members" + e, e);
            return new HashMap<>();
        }
        return members;
    }

    public long getProjectId() {
        return projectId;
    }

    @DataBoundSetter
    public void setProjectId(long projectId) {
        this.projectId = projectId;
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
            GitLabApi gitLabApi = apiBuilder(this.getOwner(), serverName);
            getGitlabProject(gitLabApi);
            if (head instanceof BranchSCMHead) {
                listener.getLogger().format("Querying the current revision of branch %s...%n", head.getName());
                String revision = gitLabApi.getRepositoryApi().getBranch(gitlabProject, head.getName()).getCommit()
                        .getId();
                listener.getLogger().format("Current revision of branch %s is %s%n", head.getName(), revision);
                return new BranchSCMRevision((BranchSCMHead) head, revision);
            } else if (head instanceof MergeRequestSCMHead) {
                MergeRequestSCMHead h = (MergeRequestSCMHead) head;
                listener.getLogger().format("Querying the current revision of merge request #%s...%n", h.getId());
                MergeRequest mr = gitLabApi.getMergeRequestApi().getMergeRequest(gitlabProject,
                        Long.parseLong(h.getId()));
                String targetSha = gitLabApi.getRepositoryApi().getBranch(mr.getTargetProjectId(), mr.getTargetBranch())
                        .getCommit().getId();
                if (mr.getState().equals(Constants.MergeRequestState.OPENED.toString())) {
                    listener.getLogger().format("Current revision of merge request #%s is %s%n",
                        h.getId(), mr.getSha());
                    return new MergeRequestSCMRevision(
                        h,
                        new BranchSCMRevision(
                            h.getTarget(),
                            targetSha
                        ),
                        new BranchSCMRevision(
                            new BranchSCMHead(h.getOriginName()),
                            mr.getSha()
                        )
                    );
                } else {
                    listener.getLogger().format("Merge request #%s is CLOSED%n", h.getId());
                    return null;
                }
            } else if (head instanceof GitLabTagSCMHead) {
                listener.getLogger().format("Querying the current revision of tag %s...%n", head.getName());
                String revision = gitLabApi.getTagsApi().getTag(gitlabProject, head.getName()).getCommit().getId();
                listener.getLogger().format("Current revision of tag %s is %s%n", head.getName(), revision);
                return new GitTagSCMRevision((GitLabTagSCMHead) head, revision);
            } else {
                listener.getLogger().format("Unknown head: %s of type %s%n", head.getName(), head.getClass().getName());
                return null;
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING, "Exception caught:" + e, e);
            throw new IOException("Failed to retrieve the SCM revision for " + head.getName(), e);
        }
    }

    @Override
    protected void retrieve(SCMSourceCriteria criteria, @NonNull SCMHeadObserver observer,
        SCMHeadEvent<?> event,
        @NonNull TaskListener listener) throws IOException, InterruptedException {
        try {
            GitLabApi gitLabApi = apiBuilder(this.getOwner(), serverName);
            getGitlabProject(gitLabApi);
            setProjectId(gitlabProject.getId());
            sshRemote = gitlabProject.getSshUrlToRepo();
            httpRemote = gitlabProject.getHttpUrlToRepo();
            try (GitLabSCMSourceRequest request = new GitLabSCMSourceContext(criteria, observer)
                .withTraits(getTraits())
                .newRequest(this, listener)) {
                request.setGitLabApi(gitLabApi);
                request.setProject(gitlabProject);
                request.setMembers(getMembers());
                if (request.isFetchBranches()) {
                    request.setBranches(gitLabApi.getRepositoryApi().getBranches(gitlabProject));
                }
                if (request.isFetchMRs()) {
                    // If not authenticated GitLabApi cannot detect if it is a fork
                    // If `forkedFromProject` is null it doesn't mean anything
                    if (gitlabProject.getForkedFromProject() == null) {
                        listener.getLogger()
                            .format(
                                "%nUnable to detect if it is a mirror or not still fetching MRs anyway...%n");
                        List<MergeRequest> mrs = gitLabApi.getMergeRequestApi()
                            .getMergeRequests(gitlabProject, Constants.MergeRequestState.OPENED);
                        mrs = mrs.stream().filter(mr -> mr.getSourceProjectId() != null)
                            .collect(Collectors.toList());
                        request.setMergeRequests(mrs);
                    } else {
                        listener.getLogger()
                            .format("%nIgnoring merge requests as project is a mirror...%n");
                    }
                }
                if (request.isFetchTags()) {
                    request.setTags(gitLabApi.getTagsApi().getTags(gitlabProject));
                }
                if (request.isFetchBranches()) {
                    int count = 0;
                    listener.getLogger().format("%nChecking branches.. %n");
                    Iterable<Branch> branches = request.getBranches();
                    for (final Branch branch : branches) {
                        count++;
                        String branchName = branch.getName();
                        String sha = branch.getCommit().getId();
                        listener.getLogger().format("%nChecking branch %s%n",
                            HyperlinkNote.encodeTo(
                                branchUriTemplate(gitlabProject.getWebUrl())
                                    .set("branch", splitPath(branchName))
                                    .expand(),
                                branchName
                            )
                        );
                        if (request.process(new BranchSCMHead(branchName),
                            (SCMSourceRequest.RevisionLambda<BranchSCMHead, BranchSCMRevision>) head ->
                                new BranchSCMRevision(head, sha),
                            new SCMSourceRequest.ProbeLambda<BranchSCMHead, BranchSCMRevision>() {
                                @NonNull
                                @Override
                                public SCMSourceCriteria.Probe create(@NonNull BranchSCMHead head,
                                    @Nullable BranchSCMRevision revision)
                                    throws IOException {
                                    return createProbe(head, revision);
                                }
                            },
                            (SCMSourceRequest.Witness) (head, revision, isMatch) -> {
                                if (isMatch) {
                                    listener.getLogger().format("Met criteria%n");
                                } else {
                                    listener.getLogger().format("Does not meet criteria%n");
                                }
                            })) {
                            listener.getLogger()
                                .format("%n%d branches were processed (query completed)%n", count);
                            return;
                        }
                    }
                    listener.getLogger().format("%n%d branches were processed%n", count);
                }
                if (request.isFetchMRs() && !request.isComplete()) {
                    int count = 0;
                    listener.getLogger().format("%nChecking merge requests..%n");
                    HashMap<Long, String> forkMrSources = new HashMap<>();
                    for (MergeRequest mr : request.getMergeRequests()) {
                        mergeRequestContributorCache.put(mr.getIid(),
                            new ContributorMetadataAction(
                                mr.getAuthor().getUsername(),
                                mr.getAuthor().getName(),
                                mr.getAuthor().getEmail()
                            ));
                        mergeRequestMetadataCache.put(mr.getIid(),
                            new ObjectMetadataAction(
                                mr.getTitle(),
                                mr.getDescription(),
                                mr.getWebUrl()
                            ));
                        count++;
                        listener.getLogger().format("%nChecking merge request %s%n",
                            HyperlinkNote.encodeTo(
                                mergeRequestUriTemplate(gitlabProject.getWebUrl())
                                    .set("iid", mr.getIid())
                                    .expand(),
                                "!" + mr.getIid()
                            )
                        );
                        Map<Boolean, Set<ChangeRequestCheckoutStrategy>> strategies = request
                            .getMRStrategies();
                        boolean fork = !mr.getSourceProjectId().equals(mr.getTargetProjectId());
                        String originOwner = mr.getAuthor().getUsername();
                        String originProjectPath = projectPath;
                        if (fork && !forkMrSources.containsKey(mr.getSourceProjectId())) {
                            // This is a hack to get the path with namespace of source project for forked mrs
                            originProjectPath = gitLabApi.getProjectApi()
                                .getProject(mr.getSourceProjectId()).getPathWithNamespace();
                            forkMrSources.put(mr.getSourceProjectId(), originProjectPath);
                        } else if (fork) {
                            originProjectPath = forkMrSources.get(mr.getSourceProjectId());
                        }
                        String targetSha;
                        try {
                            targetSha = gitLabApi.getRepositoryApi().getBranch(mr.getTargetProjectId(), mr.getTargetBranch()).getCommit().getId();
                        } catch (Exception e) {
                            listener.getLogger().format("Failed getting TargetBranch from Merge Request: " + mr.getIid() + " (" + mr.getTitle() + ")%n%s", e);
                            continue;
                        }
                        LOGGER.log(Level.FINE, String.format("%s -> %s", originOwner, (request.isMember(originOwner) ? "Trusted"
                            : "Untrusted")));
                        for (ChangeRequestCheckoutStrategy strategy : strategies.get(fork)) {
                            if (request.process(new MergeRequestSCMHead(
                                    "MR-" + mr.getIid() + (strategies.get(fork).size() > 1 ? "-" + strategy.name()
                                        .toLowerCase(Locale.ENGLISH) : ""),
                                    mr.getIid(),
                                    new BranchSCMHead(mr.getTargetBranch()),
                                    strategy,
                                    fork
                                        ? new SCMHeadOrigin.Fork(originProjectPath)
                                        : SCMHeadOrigin.DEFAULT,
                                    originOwner,
                                    originProjectPath,
                                    mr.getSourceBranch(),
                                    mr.getTitle()
                                ),
                                (SCMSourceRequest.RevisionLambda<MergeRequestSCMHead, MergeRequestSCMRevision>) head ->
                                    new MergeRequestSCMRevision(
                                        head,
                                        new BranchSCMRevision(
                                            head.getTarget(),
                                            targetSha // Latest revision of target branch
                                        ),
                                        new BranchSCMRevision(
                                            new BranchSCMHead(head.getOriginName()),
                                            mr.getSha()
                                        )
                                    ),
                                new SCMSourceRequest.ProbeLambda<MergeRequestSCMHead, MergeRequestSCMRevision>() {
                                    @NonNull
                                    @Override
                                    public SCMSourceCriteria.Probe create(
                                        @NonNull MergeRequestSCMHead head,
                                        @Nullable MergeRequestSCMRevision revision)
                                        throws IOException, InterruptedException {
                                        boolean isTrusted = request.isTrusted(head);
                                        if (!isTrusted) {
                                            listener.getLogger()
                                                .format("(not from a trusted source)%n");
                                        }
                                        return createProbe(isTrusted ? head : head.getTarget(),
                                            revision);
                                    }
                                },
                                (SCMSourceRequest.Witness) (head, revision, isMatch) -> {
                                    if (isMatch) {
                                        listener.getLogger().format("Met criteria%n");
                                    } else {
                                        listener.getLogger().format("Does not meet criteria%n");
                                    }
                                }
                            )) {
                                listener.getLogger()
                                    .format(
                                        "%n%d merge requests were processed (query completed)%n",
                                        count);
                                return;
                            }
                        }
                    }
                    listener.getLogger().format("%n%d merge requests were processed%n", count);
                }
                if (request.isFetchTags()) {
                    int count = 0;
                    listener.getLogger().format("%nChecking tags..%n");
                    Iterable<Tag> tags = request.getTags();
                    for (Tag tag : tags) {
                        count++;
                        String tagName = tag.getName();
                        Long tagDate = tag.getCommit().getCommittedDate().getTime();
                        String sha = tag.getCommit().getId();
                        listener.getLogger().format("%nChecking tag %s%n",
                            HyperlinkNote.encodeTo(
                                tagUriTemplate(gitlabProject.getWebUrl())
                                    .set("tag", splitPath(tag.getName()))
                                    .expand(),
                                tag.getName()
                            )
                        );
                        GitLabTagSCMHead head = new GitLabTagSCMHead(tagName, tagDate);
                        if (request.process(head, new GitTagSCMRevision(head, sha),
                            new SCMSourceRequest.ProbeLambda<GitLabTagSCMHead, GitTagSCMRevision>() {
                                @NonNull
                                @Override
                                public SCMSourceCriteria.Probe create(
                                    @NonNull GitLabTagSCMHead head,
                                    @Nullable GitTagSCMRevision revision)
                                    throws IOException {
                                    return createProbe(head, revision);
                                }
                            }, (SCMSourceRequest.Witness) (head1, revision, isMatch) -> {
                                if (isMatch) {
                                    listener.getLogger().format("Met criteria%n");
                                } else {
                                    listener.getLogger().format("Does not meet criteria%n");
                                }
                            })) {
                            listener.getLogger()
                                .format("%n%d tags were processed (query completed)%n", count);
                            return;
                        }
                    }
                    listener.getLogger()
                        .format("%n%d tags were processed (query completed)%n", count);
                }
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING, "Exception caught:" + e, e);
            throw new IOException("Failed to fetch latest heads", e);
        } finally {
            SCMSourceOwner owner = this.getOwner();
            if (owner != null) {
                owner.save();
            }
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
        Set<String> revisions = new HashSet<>();
        for (SCMHead head : retrieve(listener)) {
            revisions.add(head.getName());
        }
        return revisions;
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(SCMSourceEvent event, @NonNull TaskListener listener) {
        List<Action> result = new ArrayList<>();
        getGitlabProject();
        GitLabSCMSourceContext ctx = new GitLabSCMSourceContext(null, SCMHeadObserver.none()).withTraits(traits);
        String projectUrl = gitlabProject.getWebUrl();
        String name = StringUtils.isBlank(projectName) ? gitlabProject.getNameWithNamespace() : projectName;
        result.add(new ObjectMetadataAction(name, gitlabProject.getDescription(), projectUrl));
        String avatarUrl = gitlabProject.getAvatarUrl();
        if (!ctx.projectAvatarDisabled() && StringUtils.isNotBlank(avatarUrl)) {
            result.add(new GitLabAvatar(avatarUrl));
        }
        result.add(GitLabLink.toProject(projectUrl));
        return result;
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMHead head, SCMHeadEvent event, @NonNull TaskListener listener) {
        getGitlabProject();
        List<Action> result = new ArrayList<>();
        if (head instanceof BranchSCMHead) {
            String branchUrl = branchUriTemplate(serverName).set("project", splitPath(projectPath))
                    .set("branch", head.getName()).expand();
            result.add(new ObjectMetadataAction(null, null, branchUrl));
            result.add(GitLabLink.toBranch(branchUrl));
            if (head.getName().equals(gitlabProject.getDefaultBranch())) {
                result.add(new PrimaryInstanceMetadataAction());
            }
        } else if (head instanceof MergeRequestSCMHead) {
            long iid = Long.parseLong(((MergeRequestSCMHead) head).getId());
            String mergeUrl = mergeRequestUriTemplate(serverName).set("project", splitPath(projectPath)).set("iid", iid)
                    .expand();
            ObjectMetadataAction metadataAction = mergeRequestMetadataCache.get(iid);
            if (metadataAction == null) {
                // best effort
                metadataAction = new ObjectMetadataAction(null, null, mergeUrl);
            }
            result.add(metadataAction);
            ContributorMetadataAction contributor = mergeRequestContributorCache.get(iid);
            if (contributor != null) {
                result.add(contributor);
            }
            result.add(GitLabLink.toMergeRequest(mergeUrl));
        } else if (head instanceof GitLabTagSCMHead) {
            String tagUrl = tagUriTemplate(serverName).set("project", splitPath(projectPath)).set("tag", head.getName())
                    .expand();
            result.add(new ObjectMetadataAction(null, null, tagUrl));
            result.add(GitLabLink.toTag(tagUrl));
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
    public SCMRevision getTrustedRevision(@NonNull SCMRevision revision, @NonNull TaskListener listener) {
        if (revision instanceof MergeRequestSCMRevision) {
            MergeRequestSCMHead head = (MergeRequestSCMHead) revision.getHead();
            try (GitLabSCMSourceRequest request = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                    .withTraits(traits).newRequest(this, listener)) {
                request.setMembers(getMembers());
                boolean isTrusted = request.isTrusted(head);
                LOGGER.log(Level.FINEST, String.format("Trusted Revision: %s -> %s", head.getOriginOwner(), isTrusted));
                if (isTrusted) {
                    return revision;
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Exception caught: " + e, e);
            }
            MergeRequestSCMRevision rev = (MergeRequestSCMRevision) revision;
            listener.getLogger().format("Loading trusted files from target branch %s at %s rather than %s%n",
                    head.getTarget().getName(), rev.getBaseHash(), rev.getHeadHash());
            return new SCMRevisionImpl(head.getTarget(), rev.getBaseHash());
        }
        return revision;
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMRevision revision, SCMHeadEvent event,
            @NonNull TaskListener listener) throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();
        if (revision instanceof SCMRevisionImpl) {
            String hash = ((SCMRevisionImpl) revision).getHash();
            String commitUrl = commitUriTemplate(serverName).set("project", splitPath(projectPath)).set("hash", hash)
                    .expand();
            actions.add(GitLabLink.toCommit(commitUrl));
        }

        if (event instanceof AbstractGitLabSCMHeadEvent) {
            actions.add(new GitLabSCMCauseAction(((AbstractGitLabSCMHeadEvent) event).getCause()));
        }

        return actions;
    }

    @NonNull
    @Override
    protected SCMProbe createProbe(@NonNull final SCMHead head, SCMRevision revision) throws IOException {
        try {
            GitLabSCMFileSystem.BuilderImpl builder = ExtensionList.lookup(SCMFileSystem.Builder.class)
                    .get(GitLabSCMFileSystem.BuilderImpl.class);
            if (builder == null) {
                throw new AssertionError();
            }
            GitLabApi gitLabApi = apiBuilder(this.getOwner(), serverName);
            getGitlabProject(gitLabApi);
            final SCMFileSystem fs = builder.build(head, revision, gitLabApi, projectPath);
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
        GitLabServer server = GitLabServers.get().findServer(getServerName());
        // Only register webhooks in the case webhooks wants to be managed in
        // the jenkins instance.
        if (server != null && server.isManageWebHooks()) {
            GitLabSCMSourceContext ctx = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits(new GitLabSCMNavigatorContext().withTraits(traits).traits());
            GitLabHookRegistration webhookMode = ctx.webhookRegistration();
            GitLabHookRegistration systemhookMode = ctx.systemhookRegistration();
            GitLabHookCreator.register(this, webhookMode, systemhookMode);
        }
    }

    public PersonalAccessToken credentials() {
        return CredentialsMatchers.firstOrNull(lookupCredentials(PersonalAccessToken.class, getOwner(),
                Jenkins.getAuthentication(), fromUri(getServerUrlFromName(serverName)).build()),
                GitLabServer.CREDENTIALS_MATCHER);
    }

    @Symbol("gitlab")
    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor implements IconSpec {

        @Override
        public String getIconClassName() {
            return ICON_GITLAB;
        }

        @NonNull
        public String getDisplayName() {
            return Messages.GitLabSCMSource_DisplayName();
        }

        @Override
        public String getPronoun() {
            return Messages.GitLabSCMSource_Pronoun();
        }

        public String getSelectedServer(@QueryParameter String serverName) {
            return serverName;
        }

        public ListBoxModel doFillServerNameItems(@AncestorInPath SCMSourceOwner context,
                @QueryParameter String serverName) {
            if (context == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    // must have admin if you want the list without a context
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverName);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)) {
                    // must be able to read the configuration the list
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverName);
                    return result;
                }
            }
            return GitLabServers.get().getServerItems();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context,
                @QueryParameter String serverName, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (context == null) {
                // must have admin if you want the list without a context
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)
                        && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    // must be able to read the configuration or use the item credentials if you
                    // want the list
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            }
            result.includeEmptyValue();
            result.includeMatchingAs(
                    context instanceof Queue.Task ? ((Queue.Task) context).getDefaultAuthentication() : ACL.SYSTEM,
                    context, StandardUsernameCredentials.class, fromUri(getServerUrlFromName(serverName)).build(),
                    GitClient.CREDENTIALS_MATCHER);
            return result;
        }

        public long getProjectId(@AncestorInPath SCMSourceOwner context, @QueryParameter String projectPath, @QueryParameter String serverName) {
            List<GitLabServer> gitLabServers = GitLabServers.get().getServers();
            if (gitLabServers.size() == 0) {
                return -1;
            }
            try {
                GitLabApi gitLabApi;
                if (StringUtils.isBlank(serverName)) {
                    gitLabApi = apiBuilder(context, gitLabServers.get(0).getName());
                } else {
                    gitLabApi = apiBuilder(context, serverName);
                }
                if (StringUtils.isNotBlank(projectPath)) {
                    return gitLabApi.getProjectApi().getProject(projectPath).getId();
                }
            } catch (GitLabApiException e) {
                return -1;
            }
            return -1;
        }

        public ListBoxModel doFillProjectPathItems(@AncestorInPath SCMSourceOwner context,
                @QueryParameter String serverName, @QueryParameter String projectOwner) {
            List<GitLabServer> gitLabServers = GitLabServers.get().getServers();
            if (gitLabServers.size() == 0) {
                return new StandardListBoxModel().includeEmptyValue();
            }
            ListBoxModel result = new ListBoxModel();
            try {
                GitLabApi gitLabApi;
                if (serverName.equals("")) {
                    gitLabApi = apiBuilder(context, gitLabServers.get(0).getName());
                } else {
                    gitLabApi = apiBuilder(context, serverName);
                }

                if (projectOwner.equals("")) {
                    return new StandardListBoxModel().includeEmptyValue();
                }
                try {
                    for (Project p : gitLabApi.getProjectApi().getUserProjects(projectOwner,
                            new ProjectFilter().withOwned(true))) {
                        result.add(p.getPathWithNamespace());
                    }
                } catch (GitLabApiException e) {
                    for (Project p : gitLabApi.getGroupApi().getProjects(projectOwner)) {
                        result.add(p.getPathWithNamespace());
                    }
                }
                return result;
            } catch (GitLabApiException e) {
                LOGGER.log(Level.WARNING, "Exception caught:" + e, e);
                return new StandardListBoxModel().includeEmptyValue();
            }
        }

        public List<NamedArrayList<? extends SCMTraitDescriptor<?>>> getTraitsDescriptorLists() {
            List<SCMTraitDescriptor<?>> all = new ArrayList<>();
            all.addAll(SCMSourceTrait._for(this, GitLabSCMSourceContext.class, null));
            all.addAll(SCMSourceTrait._for(this, null, GitLabSCMBuilder.class));
            Set<SCMTraitDescriptor<?>> dedup = new HashSet<>();
            for (Iterator<SCMTraitDescriptor<?>> iterator = all.iterator(); iterator.hasNext();) {
                SCMTraitDescriptor<?> d = iterator.next();
                if (dedup.contains(d) || d instanceof GitBrowserSCMSourceTrait.DescriptorImpl) {
                    // remove any we have seen already and ban the browser configuration as it will
                    // always be github
                    iterator.remove();
                } else {
                    dedup.add(d);
                }
            }
            List<NamedArrayList<? extends SCMTraitDescriptor<?>>> result = new ArrayList<>();
            NamedArrayList.select(all, "Projects", new NamedArrayList.Predicate<SCMTraitDescriptor<?>>() {
                @Override
                public boolean test(SCMTraitDescriptor<?> scmTraitDescriptor) {
                    return scmTraitDescriptor instanceof SCMNavigatorTraitDescriptor;
                }
            }, true, result);
            NamedArrayList.select(all, "Within project",
                    NamedArrayList.anyOf(NamedArrayList.withAnnotation(Discovery.class),
                            NamedArrayList.withAnnotation(Selection.class)),
                    true, result);
            NamedArrayList.select(all, "Additional", null, true, result);
            return result;
        }

        @NonNull
        public List<SCMSourceTrait> getTraitsDefaults() {
            return Arrays.<SCMSourceTrait>asList( // TODO finalize
                    new BranchDiscoveryTrait(true, false),
                    new OriginMergeRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE)),
                    new ForkMergeRequestDiscoveryTrait(EnumSet.of(ChangeRequestCheckoutStrategy.MERGE),
                    new ForkMergeRequestDiscoveryTrait.TrustPermission()),
                    new WebhookListenerBuildConditionsTrait());
        }

        @NonNull
        @Override
        protected SCMHeadCategory[] createCategories() {
            return new SCMHeadCategory[] {
                    new UncategorizedSCMHeadCategory(Messages._GitLabSCMSource_UncategorizedCategory()),
                    new ChangeRequestSCMHeadCategory(Messages._GitLabSCMSource_ChangeRequestCategory()),
                    new TagSCMHeadCategory(Messages._GitLabSCMSource_TagCategory()) };
        }
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Known to be null during deserialization")
    private Object readResolve() {
        if (mergeRequestMetadataCache == null) {
            mergeRequestMetadataCache = new ConcurrentHashMap<>();
        }
        if (mergeRequestContributorCache == null) {
            mergeRequestContributorCache = new ConcurrentHashMap<>();
        }
        return this;
    }
}
