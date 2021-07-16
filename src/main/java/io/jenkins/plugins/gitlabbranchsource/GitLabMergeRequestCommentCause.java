package io.jenkins.plugins.gitlabbranchsource;

import hudson.model.Cause;
import io.jenkins.plugins.gitlabbranchsource.Cause.GitLabMergeRequestNoteData;
import org.gitlab4j.api.webhook.NoteEvent;

public final class GitLabMergeRequestCommentCause extends Cause {

    private final String commentUrl;
    private final GitLabMergeRequestNoteData gitLabMergeRequestNoteData;

    /**
     * Constructor.
     *
     * @param commentUrl the URL for the GitLab comment
     */
    public GitLabMergeRequestCommentCause(String commentUrl) {
        this.commentUrl = commentUrl;
        this.gitLabMergeRequestNoteData = null;
    }

    /**
     * Constructor.
     *
     * @param commentUrl the URL for the GitLab comment
     * @param noteEvent note event
     */
    public GitLabMergeRequestCommentCause(String commentUrl, NoteEvent noteEvent) {
        this.commentUrl = commentUrl;
        this.gitLabMergeRequestNoteData = new GitLabMergeRequestNoteData(noteEvent);
    }

    @Override
    public String getShortDescription() {
        return "GitLab merge request comment";
    }

    /**
     * Retrieves the URL for the GitLab comment for this cause.
     *
     * @return the URL for the GitLab comment
     */
    public String getCommentUrl() {
        return commentUrl;
    }

    /**
     * Retrieves the cause data
     * @return cause data
     */
    public GitLabMergeRequestNoteData getGitLabMergeRequestNoteData() {
        return gitLabMergeRequestNoteData;
    }
}
