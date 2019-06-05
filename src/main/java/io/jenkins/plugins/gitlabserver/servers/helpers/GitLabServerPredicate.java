package io.jenkins.plugins.gitlabserver.servers.helpers;

import io.jenkins.plugins.gitlabserver.servers.GitLabServer;

public interface GitLabServerPredicate {
    boolean test (String serverUrl, GitLabServer gitLabServer);
}
