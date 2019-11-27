package io.jenkins.plugins.gitlabbranchsource.retry;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.User;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class UserApiRetryWrapper {
    private final UserApi _userApi;
    private final Integer _retryCount;

    public UserApiRetryWrapper(UserApi userApi, Integer retryCount){
        _userApi = userApi;
        _retryCount = retryCount;
    }

    public User getUser(String userName) throws GitLabApiException {
        Retryable<User> call = () -> _userApi.getUser(userName);

        return withRetry(call, _retryCount);
    }
}
