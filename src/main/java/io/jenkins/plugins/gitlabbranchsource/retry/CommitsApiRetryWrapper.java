package io.jenkins.plugins.gitlabbranchsource.retry;

import org.gitlab4j.api.CommitsApi;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Comment;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.CommitStatus;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class CommitsApiRetryWrapper {
    private final CommitsApi _commitsApi;
    private final Integer _retryCount;

    public CommitsApiRetryWrapper(CommitsApi commitsApi, Integer retryCount) {
        this._commitsApi = commitsApi;
        this._retryCount = retryCount;
    }

    public Comment addComment(Object projectIdOrPath, String sha, String note) throws GitLabApiException {
        Retryable<Comment> call = () -> _commitsApi.addComment(projectIdOrPath, sha, note);

        return withRetry(call, _retryCount);
    }

    public CommitStatus addCommitStatus(Object projectIdOrPath, String sha, Constants.CommitBuildState state, CommitStatus status) throws GitLabApiException {
        Retryable<CommitStatus> call = () -> _commitsApi.addCommitStatus(projectIdOrPath, sha, state, status);

        return withRetry(call, _retryCount);
    }

    public Commit getCommit(Object projectIdOrPath, String sha) throws GitLabApiException {
        Retryable<Commit> call = () -> _commitsApi.getCommit(projectIdOrPath, sha);

        return withRetry(call, _retryCount);
    }
}
