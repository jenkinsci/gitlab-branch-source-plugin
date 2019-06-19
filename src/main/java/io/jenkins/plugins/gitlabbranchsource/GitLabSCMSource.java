package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.damnhandy.uri.template.UriTemplate;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.TaskListener;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
                                                .literal("/blob")
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
}
