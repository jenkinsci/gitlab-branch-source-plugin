package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHeadObserver;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;

public class GitLabMergeRequestTrigger extends GitLabMergeRequestSCMEvent {

    public static final Logger LOGGER = Logger.getLogger(GitLabMergeRequestTrigger.class.getName());

    public GitLabMergeRequestTrigger(MergeRequestEvent mrEvent, String origin) {
        super(mrEvent, origin);
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMSource source) {
        final GitLabSCMSourceContext sourceContext =
                new GitLabSCMSourceContext(null, SCMHeadObserver.none()).withTraits(source.getTraits());

        boolean shouldBuild = this.shouldBuild(getPayload(), sourceContext);
        LOGGER.log(Level.FINE, "isMatch() result for MR-{0}: {1}", new Object[] {
            getPayload().getObjectAttributes().getIid(), String.valueOf(shouldBuild)
        });

        return getPayload().getObjectAttributes().getTargetProjectId().equals(source.getProjectId()) && shouldBuild;
    }

    private boolean shouldBuild(MergeRequestEvent mrEvent, GitLabSCMSourceContext context) {
        ObjectAttributes attributes = mrEvent.getObjectAttributes();
        String action = attributes.getAction();
        boolean shouldBuild = true;

        if (attributes.getWorkInProgress() && context.alwaysIgnoreMRWorkInProgress()) {
            LOGGER.log(
                    Level.FINE,
                    "shouldBuild for MR-{0} set to false due to WorkInProgress=true.",
                    getPayload().getObjectAttributes().getIid());
            return false;
        }

        if (action != null) {
            if (action.equals("update") && context.alwaysIgnoreNonCodeRelatedUpdates()) {
                if (attributes.getOldrev() == null) {
                    shouldBuild = false;
                }
            }

            if (!shouldBuild) {
                LOGGER.log(
                        Level.FINE,
                        "shouldBuild for MR-{0} set to false due to non-code related updates.",
                        getPayload().getObjectAttributes().getIid());
            }

            LOGGER.log(
                    Level.FINEST,
                    "shouldBuild for MR-{0} will be set for action {1} based on pipeline configuration.",
                    new Object[] {getPayload().getObjectAttributes().getIid(), action});

            if (action.equals("open")) {
                return context.alwaysBuildMROpen();
            }

            if (action.equals("reopen")) {
                return context.alwaysBuildMRReOpen();
            }

            if (action.equals("approval")) {
                return !context.alwaysIgnoreMRApproval();
            }

            if (action.equals("unapproval")) {
                return !context.alwaysIgnoreMRUnApproval();
            }

            if (action.equals("approved")) {
                return !context.alwaysIgnoreMRApproved();
            }

            if (action.equals("unapproved")) {
                return !context.alwaysIgnoreMRUnApproved();
            }
        }

        return shouldBuild;
    }
}
