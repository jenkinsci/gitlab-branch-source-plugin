package io.jenkins.plugins.gitlabbranchsource;

import java.util.logging.Logger;
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
        new GitLabMergeRequestSCMEvent(event, origin);
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.info("PUSH EVENT");
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.info("TAG EVENT");
    }
}
