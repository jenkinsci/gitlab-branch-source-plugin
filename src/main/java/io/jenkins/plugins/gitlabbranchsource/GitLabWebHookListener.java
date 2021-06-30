package io.jenkins.plugins.gitlabbranchsource;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHeadEvent;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Version;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.NoteEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;

import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.apiBuilder;

public class GitLabWebHookListener implements WebHookListener {
    public static final Logger LOGGER = Logger.getLogger(GitLabWebHookListener.class.getName());

    // Delay used for triggers to avoid GitLab cache
    private static final long TRIGGER_DELAY_SECONDS = 30;

    private String origin;

    public GitLabWebHookListener(String origin) {
        this.origin = origin;
    }

    @Override
    public void onNoteEvent(NoteEvent noteEvent) {
        LOGGER.log(Level.FINE, noteEvent.toString());
        GitLabMergeRequestCommentTrigger trigger = new GitLabMergeRequestCommentTrigger(noteEvent);
        AbstractGitLabJobTrigger.fireNow(trigger);
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent mrEvent) {
        LOGGER.log(Level.FINE, mrEvent.toString());
        //mrEvent.getProject().getWebUrl()
        GitLabMergeRequestSCMEvent trigger = new GitLabMergeRequestSCMEvent(mrEvent, origin);
        SCMHeadEvent.fireLater(trigger, TRIGGER_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.log(Level.FINE, pushEvent.toString());
        GitLabPushSCMEvent trigger = new GitLabPushSCMEvent(pushEvent, origin);
        SCMHeadEvent.fireLater(trigger, TRIGGER_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.log(Level.FINE, tagPushEvent.toString());
        GitLabTagPushSCMEvent trigger = new GitLabTagPushSCMEvent(tagPushEvent, origin);
        SCMHeadEvent.fireLater(trigger, TRIGGER_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private long findTriggerDelay(final String projectUrl) {
        GitlabServer projectServer = null;
        for (GitLabServer server: GitLabServers.get().getServers()) {
            if (projectUrl.startsWith(server.getServerUrl())) {
                projectServer = server;
                break;
            }
        }
        if (projectServer != null) {
            final Integer delay = projectServer.getHookTriggerDelay();
            if (delay != null) {
                return delay;
            } else {
                try {
                    final Version gitLabVersion = apiBuilder(serverName).getVersion();
                    // check version...
                } catch (GitLabApiException e) {
                    LOGGER.log(Level.WARNING, String.format("Error retrieving GitLab version: %s", e.getMessage()));
                    return TRIGGER_DELAY_SECONDS;
                }
            }
        } else {
            return TRIGGER_DELAY_SECONDS;
        }
    }
}
