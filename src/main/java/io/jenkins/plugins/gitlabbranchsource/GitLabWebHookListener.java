package io.jenkins.plugins.gitlabbranchsource;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHeadEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.NoteEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;

public class GitLabWebHookListener implements WebHookListener {
    public static final Logger LOGGER = Logger.getLogger(GitLabWebHookListener.class.getName());

    // GitLab API caching timeout used for branches endpoint
    private static final long GITLAB_CACHING_TIMEOUT = 30;

    private String origin;

    public GitLabWebHookListener(String origin) {
        this.origin = origin;
    }

    @Override
    public void onNoteEvent(NoteEvent noteEvent) {
        LOGGER.log(Level.FINE, noteEvent.toString());
        GitLabMergeRequestCommentTrigger trigger =
            new GitLabMergeRequestCommentTrigger(noteEvent);
        AbstractGitLabJobTrigger.fireNow(trigger);
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent mrEvent) {
        LOGGER.log(Level.FINE, mrEvent.toString());
        final long triggerDelay = findTriggerDelay(mrEvent.getProject().getWebUrl());
        GitLabMergeRequestSCMEvent trigger = new GitLabMergeRequestSCMEvent(mrEvent, origin);
        SCMHeadEvent.fireLater(trigger, triggerDelay, TimeUnit.SECONDS);
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.log(Level.FINE, pushEvent.toString());
        final long triggerDelay = findTriggerDelay(pushEvent.getProject().getWebUrl());
        GitLabPushSCMEvent trigger = new GitLabPushSCMEvent(pushEvent, origin);
        SCMHeadEvent.fireLater(trigger, triggerDelay, TimeUnit.SECONDS);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.log(Level.FINE, tagPushEvent.toString());
        final long triggerDelay = findTriggerDelay(tagPushEvent.getProject().getWebUrl());
        GitLabTagPushSCMEvent trigger = new GitLabTagPushSCMEvent(tagPushEvent, origin);
        SCMHeadEvent.fireLater(trigger, triggerDelay, TimeUnit.SECONDS);
    }

    private long findTriggerDelay(final String projectUrl) {
        final GitLabServer projectServer = findProjectServer(projectUrl);

        if (projectServer == null) {
            LOGGER.log(
                Level.WARNING,
                "Falling back to default trigger delay equal GitLab caching timeout");
            return GITLAB_CACHING_TIMEOUT;
        }

        final Integer configuredDelay = projectServer.getHookTriggerDelay();
        if (configuredDelay != null) {
            return configuredDelay;
        } else {
            return GITLAB_CACHING_TIMEOUT;
        }
    }

    private GitLabServer findProjectServer(final String projectUrl) {
        for (GitLabServer server: GitLabServers.get().getServers()) {
            if (projectUrl.startsWith(server.getServerUrl())) {
                return server;
            }
        }
        LOGGER.log(Level.WARNING,
                   String.format("No GitLab server for project URL: %s", projectUrl));
        return null;
    }
}
