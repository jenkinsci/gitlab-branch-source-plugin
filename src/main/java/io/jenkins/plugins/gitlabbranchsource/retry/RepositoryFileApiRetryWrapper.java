package io.jenkins.plugins.gitlabbranchsource.retry;

import java.io.InputStream;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.RepositoryFileApi;
import org.gitlab4j.api.models.RepositoryFile;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class RepositoryFileApiRetryWrapper {
    private final RepositoryFileApi _repositoryFileApi;
    private final Integer _retryCount;

    public RepositoryFileApiRetryWrapper(RepositoryFileApi repositoryFileApi, Integer retryCount) {
        _repositoryFileApi = repositoryFileApi;
        _retryCount = retryCount;
    }

    public RepositoryFile getFile(Object projectIdOrPath, String filePath, String ref) throws GitLabApiException {
        Retryable<RepositoryFile> call = () -> _repositoryFileApi.getFile(projectIdOrPath, filePath, ref);

        return withRetry(call, _retryCount);
    }

    public InputStream getRawFile(Object projectIdOrPath, String commitOrBranchName, String filepath) throws GitLabApiException {
        Retryable<InputStream> call = () -> _repositoryFileApi.getRawFile(projectIdOrPath, commitOrBranchName, filepath);

        return withRetry(call, _retryCount);
    }
}
