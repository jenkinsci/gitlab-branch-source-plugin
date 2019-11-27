package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.List;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.models.MergeRequest;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class MergeRequestApiWithRetry {
    private final MergeRequestApi _mergeRequestApi;
    private final Integer _retryCount;

    public MergeRequestApiWithRetry(MergeRequestApi mergeRequestApi, Integer retryCount) {
        _mergeRequestApi = mergeRequestApi;
        _retryCount = retryCount;
    }

    public MergeRequest getMergeRequest(Object projectIdOrPath, Integer mergeRequestIid) throws GitLabApiException {
        Retryable<MergeRequest> call = () -> _mergeRequestApi.getMergeRequest(projectIdOrPath, mergeRequestIid);

        return withRetry(call, _retryCount);
    }

    public List<MergeRequest> getMergeRequests(Object projectIdOrPath, Constants.MergeRequestState state) throws GitLabApiException {
        Retryable<List<MergeRequest>> call = () -> _mergeRequestApi.getMergeRequests(projectIdOrPath, state);

        return withRetry(call, _retryCount);
    }
}
