package io.jenkins.plugins.gitlabbranchsource.servers.helpers;


import io.jenkins.plugins.gitlabbranchsource.servers.GitLabServer;

public class GitLabServerEqualPredicate implements GitLabServerPredicate {
        public boolean test(String serverUrl, GitLabServer gitLabServer) {
            return serverUrl.equals(gitLabServer.getServerUrl());
        }
    }
