package io.jenkins.plugins.gitlabbranchsource.helpers;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.User;

public enum GitLabOwner {

    GROUP,

    USER;

    public static GitLabOwner fetchOwner(GitLabApi gitLabApi, String projectOwner) {
        try {
            gitLabApi.getGroupApi().getGroup(projectOwner);
            return GitLabOwner.GROUP;
        } catch (GitLabApiException e) {
            try {
                gitLabApi.getUserApi().getUser(projectOwner);
                return GitLabOwner.USER;
            } catch (GitLabApiException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return null;
    }
}
