package io.jenkins.plugins.gitlabbranchsource;

import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;

public class GitLabWebhookListener implements WebHookListener {

    @Override
    public void onMergeRequestEvent(MergeRequestEvent event) {

    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {

    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {

    }
}
