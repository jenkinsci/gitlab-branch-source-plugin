package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.gitlab4j.api.webhook.MergeRequestEvent;

public class GitLabMergeRequestSCMEvent extends AbstractGitLabSCMHeadEvent<MergeRequestEvent> {

    public GitLabMergeRequestSCMEvent(MergeRequestEvent mrEvent, String origin) {
        super(typeOf(mrEvent), mrEvent, origin);
    }

    // TODO: Take care of "locked" state
    private static Type typeOf(MergeRequestEvent mrEvent) {
        switch (mrEvent.getObjectAttributes().getState()) {
            case "opened":
                return Type.CREATED;
            case "closed":
                return Type.REMOVED;
            case "reopened":
            default:
                return Type.UPDATED;
        }
    }

    @Override
    public String descriptionFor(@NonNull SCMNavigator navigator) {
        String state = getPayload().getObjectAttributes().getState();
        if (state != null) {
            switch (state) {
                case "opened":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " opened in project " + getPayload()
                            .getProject().getName();
                case "reopened":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " reopened in project " + getPayload()
                            .getProject().getName();
                case "closed":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " closed in project " + getPayload()
                            .getProject().getName();
            }
        }
        return "Merge request !" + getPayload().getObjectAttributes().getIid()+ " event in project " + getPayload().getProject()
                .getName();
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMNavigator navigator) {
        return navigator.getNavigatorProjects().contains(getPayload().getProject().getPathWithNamespace());
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMSource source) {
        return getPayload().getObjectAttributes().getTargetProjectId().equals(source.getProjectId());
    }

    @NonNull
    @Override
    public String getSourceName() {
        return getPayload().getProject().getPathWithNamespace();
    }

    @Override
    public String descriptionFor(@NonNull SCMSource source) {
        String state = getPayload().getObjectAttributes().getState();
        if (state != null) {
            switch (state) {
                case "opened":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " opened";
                case "reopened":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " reopened";
                case "closed":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " closed";
            }
        }
        return "Merge request !" + getPayload().getObjectAttributes() .getIid()+ " event";
    }

    @Override
    public String description() {
        String state = getPayload().getObjectAttributes().getState();
        if (state != null) {
            switch (state) {
                case "opened":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " opened in project " + getPayload()
                            .getProject().getPathWithNamespace();
                case "reopened":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " reopened in project " + getPayload()
                            .getProject().getPathWithNamespace();
                case "closed":
                    return "Merge request !" + getPayload().getObjectAttributes().getIid() + " closed in project " + getPayload()
                            .getProject().getPathWithNamespace();
            }
        }
        return "Merge request !" + getPayload().getObjectAttributes() .getIid()+ " event";
    }

    @NonNull
    @Override
    public Map<SCMHead, SCMRevision> headsFor(GitLabSCMSource source) {
        Map<SCMHead, SCMRevision> result = new HashMap<>();
        try (GitLabSCMSourceRequest request = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits(source.getTraits())
                .newRequest(source, null)) {
            MergeRequestEvent.ObjectAttributes m = getPayload().getObjectAttributes();
            String originOwner = getPayload().getUser().getUsername();
            String originProjectPath = m.getSource().getPathWithNamespace();
            Map<Boolean, Set<ChangeRequestCheckoutStrategy>> strategies = request.getMRStrategies();
            boolean fork = !getPayload().getObjectAttributes().getSourceProjectId().equals(getPayload().getObjectAttributes().getTargetProjectId());
            for (ChangeRequestCheckoutStrategy strategy : strategies.get(fork)) {
               MergeRequestSCMHead h = new MergeRequestSCMHead(
                                "MR-" + m.getIid() + (strategies.size() > 1 ? "-" + strategy.name()
                                        .toLowerCase(Locale.ENGLISH) : ""),
                                m.getIid(),
                                new BranchSCMHead(m.getTargetBranch()),
                                ChangeRequestCheckoutStrategy.MERGE,
                                !fork
                                        ? SCMHeadOrigin.DEFAULT
                                        : new SCMHeadOrigin.Fork(originProjectPath),
                                originOwner,
                                originProjectPath,
                                m.getSourceBranch()
                        );
                result.put(h, m.getState().equals("closed")
                        ? null
                        : new MergeRequestSCMRevision(
                        h,
                        new BranchSCMRevision(
                                h.getTarget(),
                                "HEAD"
                        ),
                        new BranchSCMRevision(
                                new BranchSCMHead(h.getOriginName()),
                                m.getLastCommit().getId()
                        )
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
