package io.jenkins.plugins.gitlabbranchsource;

import hudson.model.Cause;

public final class GitLabMergeRequestCommentCause extends Cause {

    private final String commentUrl;

    /**
     * Constructor.
     *
     * @param commentUrl the URL for the GitLab comment
     */
    public GitLabMergeRequestCommentCause(String commentUrl) {
        this.commentUrl = commentUrl;
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
}
