package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.List;
import java.util.logging.Logger;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.TreeItem;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class RepositoryApiWithRetry {
    public static final Logger LOGGER = Logger.getLogger(RepositoryApiWithRetry.class.getName());

    private final RepositoryApi _repositoryApi;
    private final Integer _retryCount;

    public RepositoryApiWithRetry(RepositoryApi repositoryApi, Integer retryCount) {
        this._repositoryApi = repositoryApi;
        _retryCount = retryCount;
    }

    public List<Branch> getBranches(Object projectIdOrPath) throws GitLabApiException {
        Retryable<List<Branch>> call = () -> _repositoryApi.getBranches(projectIdOrPath);

        return withRetry(call, _retryCount);
    }

    public Branch getBranch(Object projectIdOrPath, String branchName) throws GitLabApiException {
        Retryable<Branch> call = () -> _repositoryApi.getBranch(projectIdOrPath, branchName);

        return withRetry(call, _retryCount);
    }

    public List<TreeItem> getTree(Object projectIdOrPath, String filePath, String refName) throws GitLabApiException {
        Retryable<List<TreeItem>> call = () -> _repositoryApi.getTree(projectIdOrPath, filePath, refName);

        return withRetry(call, _retryCount);
    }
}
