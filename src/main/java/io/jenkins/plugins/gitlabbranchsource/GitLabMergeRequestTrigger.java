package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHeadObserver;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;

public class GitLabMergeRequestTrigger extends GitLabMergeRequestSCMEvent {

    public static final Logger LOGGER = Logger
        .getLogger(GitLabMergeRequestTrigger.class.getName());

    public GitLabMergeRequestTrigger(MergeRequestEvent mrEvent, String origin) {
        super(mrEvent, origin);
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMSource source) {
        final GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(
            null, SCMHeadObserver.none())
            .withTraits(source.getTraits());

        boolean shouldBuild = this.shouldBuild(getPayload(), sourceContext);
        LOGGER.log(Level.FINE, "isMatch() result for MR-{0}: {1}",
            new Object[]{
                getPayload().getObjectAttributes().getIid(),
                String.valueOf(shouldBuild)
            });

        return getPayload().getObjectAttributes().getTargetProjectId()
            .equals(source.getProjectId()) && shouldBuild;
    }

    private boolean shouldBuild(MergeRequestEvent mrEvent, GitLabSCMSourceContext context) {
        ObjectAttributes attributes = mrEvent.getObjectAttributes();
        String action = attributes.getAction();
        boolean shouldBuild = true;

        if (action != null) {
            if (action.equals("update") && context.alwaysIgnoreNonCodeRelatedUpdates()) {
                if (mrEvent.getChanges().getAssignees() != null) {
                    shouldBuild = false;
                }

                if (mrEvent.getChanges().getDescription() != null) {
                    shouldBuild = false;
                }

                if (mrEvent.getChanges().getMilestoneId() != null) {
                    shouldBuild = false;
                }

                if (mrEvent.getChanges().getTitle() != null) {
                    shouldBuild = false;
                }

                if (mrEvent.getChanges().getTotalTimeSpent() != null) {
                    shouldBuild = false;
                }

                if (mrEvent.getChanges().getLabels() != null) {
                    shouldBuild = false;
                }
            }

            if (!shouldBuild) {
                LOGGER.log(Level.FINE, "shouldBuild for MR-{0} set to false due to non-code related updates.",
                    getPayload().getObjectAttributes().getIid());
            }

            if (action.equals("open")) {
                return context.alwaysBuildMROpen();
            }

            if (action.equals("reopen")) {
                return context.alwaysBuildMRReOpen();
            }

            if (action.equals("approved")) {
                return !context.alwaysIgnoreMRApprove();
            }

            if (action.equals("unapproved")) {
                return !context.alwaysIgnoreMRUnApprove();
            }
        }

        return shouldBuild;
    }
}
