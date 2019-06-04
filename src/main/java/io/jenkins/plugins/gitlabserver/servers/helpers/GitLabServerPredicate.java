package io.jenkins.plugins.gitlabbranchsource.servers.helpers;

import io.jenkins.plugins.gitlabbranchsource.servers.GitLabServer;

public interface GitLabServerPredicate {
    boolean test (String serverUrl, GitLabServer gitLabServer);
}
