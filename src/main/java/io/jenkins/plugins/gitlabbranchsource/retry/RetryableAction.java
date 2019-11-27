package io.jenkins.plugins.gitlabbranchsource.retry;

import org.gitlab4j.api.GitLabApiException;

@FunctionalInterface
public interface RetryableAction {
    void execute() throws GitLabApiException;
}
