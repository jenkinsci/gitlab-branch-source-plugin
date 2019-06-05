package io.jenkins.plugins.gitlabserver.servers.helpers;


import io.jenkins.plugins.gitlabserver.servers.GitLabServer;

public class GitLabServerEqualPredicate implements GitLabServerPredicate {
        public boolean test(String serverUrl, GitLabServer gitLabServer) {
            return serverUrl.equals(gitLabServer.getServerUrl());
        }
    }
