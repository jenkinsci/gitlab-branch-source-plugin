package io.jenkins.plugins.gitlabbranchsource;


import java.util.logging.Logger;
import jenkins.scm.api.SCMHeadEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;

public class GitLabWebhookListener implements WebHookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabWebhookListener.class.getName());

    private String origin;

    public GitLabWebhookListener(String origin) {
        this.origin = origin;
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent event) {
        LOGGER.info("MR EVENT");
        LOGGER.info(event.toString());
        GitLabMergeRequestSCMEvent trigger = new GitLabMergeRequestSCMEvent(event, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.info("PUSH EVENT");
        LOGGER.info(pushEvent.toString());
        GitLabPushSCMEvent trigger = new GitLabPushSCMEvent(pushEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.info("TAG EVENT");
    }
}
