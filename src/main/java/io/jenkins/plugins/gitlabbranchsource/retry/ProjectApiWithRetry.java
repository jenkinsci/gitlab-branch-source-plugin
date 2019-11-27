package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.List;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class ProjectApiWithRetry {
    private final ProjectApi _projectApi;
    private final Integer _retryCount;

    public ProjectApiWithRetry(ProjectApi projectApi, Integer retryCount) {
        _projectApi = projectApi;
        _retryCount = retryCount;
    }

    public Project getProject(Object projectIdOrPath) throws GitLabApiException {
        Retryable<Project> call = () -> _projectApi.getProject(projectIdOrPath);

        return withRetry(call, _retryCount);
    }

    public List<Member> getAllMembers(Object projectIdOrPath) throws GitLabApiException {
        Retryable<List<Member>> call = () -> _projectApi.getAllMembers(projectIdOrPath);

        return withRetry(call, _retryCount);
    }

    public List<Project> getUserProjects(Object userIdOrUsername, ProjectFilter filter) throws GitLabApiException {
        Retryable<List<Project>> call = () -> _projectApi.getUserProjects(userIdOrUsername, filter);

        return withRetry(call, _retryCount);
    }
}
