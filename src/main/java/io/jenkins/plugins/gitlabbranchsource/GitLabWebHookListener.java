package io.jenkins.plugins.gitlabbranchsource;

import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHeadEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.NoteEvent;
import org.gitlab4j.api.webhook.NoteEvent.NoteableType;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;

public class GitLabWebHookListener implements WebHookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabWebHookListener.class.getName());

    private String origin;

    public GitLabWebHookListener(String origin) {
        this.origin = origin;
    }

    @Override
    public void onNoteEvent(NoteEvent noteEvent) {
        LOGGER.log(Level.FINE, noteEvent.toString());

        // Add additional checks to process different noteable types
        if (noteEvent.getObjectAttributes().getNoteableType() == NoteableType.MERGE_REQUEST) {
            GitLabMergeRequestCommentTrigger trigger = new GitLabMergeRequestCommentTrigger(noteEvent);
            AbstractGitLabJobTrigger.fireNow(trigger);
        }
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent mrEvent) {
        LOGGER.log(Level.FINE, mrEvent.toString());
        GitLabMergeRequestSCMEvent trigger = new GitLabMergeRequestSCMEvent(mrEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.log(Level.FINE, pushEvent.toString());
        GitLabPushSCMEvent trigger = new GitLabPushSCMEvent(pushEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.log(Level.FINE, tagPushEvent.toString());
        GitLabTagPushSCMEvent trigger = new GitLabTagPushSCMEvent(tagPushEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

}
