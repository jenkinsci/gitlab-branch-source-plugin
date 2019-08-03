package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.webhook.NoteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitLabMergeRequestSCMCommentEvent extends AbstractGitLabSCMHeadEvent<NoteEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabSCMSource.class);

    public static String TRIGGER_COMMENT = "jenkins rebuild";

    public GitLabMergeRequestSCMCommentEvent(NoteEvent noteEvent, String origin) {
        super(Type.UPDATED, noteEvent, origin);
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMNavigator navigator) {
        return navigator.getNavigatorProjects().contains(getPayload().getProject().getPathWithNamespace());
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMSource source) {
        return getPayload().getMergeRequest().getTargetProjectId().equals(source.getProjectId());
    }

    @NonNull
    @Override
    protected Map<SCMHead, SCMRevision> headsFor(GitLabSCMSource source) {
        Map<SCMHead, SCMRevision> result = new HashMap<>();
        try (GitLabSCMSourceRequest request = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits(source.getTraits())
                .newRequest(source, null)) {
            GitLabApi gitLabApi = GitLabHelper.apiBuilder(source.getServerName());
            Map<Boolean, Set<ChangeRequestCheckoutStrategy>> strategies = request.getMRStrategies();
            boolean fork = !getPayload().getObjectAttributes().getProjectId().equals(getPayload().getProjectId());
            String originOwner = gitLabApi.getUserApi().getUser(getPayload().getMergeRequest().getAuthorId()).getUsername();
            String originProjectPath = gitLabApi.getProjectApi()
                    .getProject(getPayload().getMergeRequest().getSourceProjectId()).getPathWithNamespace();
            for (ChangeRequestCheckoutStrategy strategy : strategies.get(fork)) {
                MergeRequestSCMHead h = new MergeRequestSCMHead(
                        "MR-" + getPayload().getMergeRequest().getIid() + (strategies.size() > 1 ? "-" + strategy.name()
                                .toLowerCase(Locale.ENGLISH) : ""),
                        getPayload().getMergeRequest().getIid() ,
                        new BranchSCMHead(getPayload().getMergeRequest().getTargetBranch()),
                        ChangeRequestCheckoutStrategy.MERGE,
                        fork
                                ? new SCMHeadOrigin.Fork(originProjectPath)
                                : SCMHeadOrigin.DEFAULT,
                        originOwner,
                        originProjectPath,
                        getPayload().getMergeRequest().getSourceBranch()
                );
                result.put(h, new MergeRequestSCMRevision(
                        h,
                        new BranchSCMRevision(
                                h.getTarget(),
                                "HEAD"
                        ),
                        new BranchSCMRevision(
                                new BranchSCMHead(h.getOriginName()),
                                getPayload().getMergeRequest().getLastCommit().getId()
                        )
                ));
            }
        } catch (IOException | NoSuchFieldException | GitLabApiException e) {
            e.printStackTrace();
        }
        return result;
    }

    @NonNull
    @Override
    public String getSourceName() {
        return getPayload().getProject().getPathWithNamespace();
    }
}
