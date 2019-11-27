package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.List;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupProjectsFilter;
import org.gitlab4j.api.models.Project;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class GroupApiRetryWrapper {
    private final GroupApi _groupApi;
    private final Integer _retryCount;

    public GroupApiRetryWrapper(GroupApi groupApi, Integer retryCount) {
        _groupApi = groupApi;
        _retryCount = retryCount;
    }

    public List<Project> getProjects(Object groupIdOrPath) throws GitLabApiException {
        Retryable<List<Project>> call = () -> _groupApi.getProjects(groupIdOrPath);

        return withRetry(call, _retryCount);
    }

    public List<Project> getProjects(Object groupIdOrPath, GroupProjectsFilter filter) throws GitLabApiException {
        Retryable<List<Project>> call = () -> _groupApi.getProjects(groupIdOrPath, filter);

        return withRetry(call, _retryCount);
    }

    public Group getGroup(Object groupIdOrPath) throws GitLabApiException {
        Retryable<Group> call = () -> _groupApi.getGroup(groupIdOrPath);

        return withRetry(call, _retryCount);
    }
}
