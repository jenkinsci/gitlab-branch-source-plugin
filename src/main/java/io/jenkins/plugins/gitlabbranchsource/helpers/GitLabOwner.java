package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.User;

public abstract class GitLabOwner {

    private String name;
    private String webUrl;
    private String avatarUrl;
    private Integer id;

    public GitLabOwner(String name, String webUrl, String avatarUrl, Integer id) {
        this.name = name;
        this.webUrl = webUrl;
        this.avatarUrl = avatarUrl;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public abstract String getWord();

    @NonNull
    public static GitLabOwner fetchOwner(GitLabApi gitLabApi, String projectOwner) {
        try {
            Group group = gitLabApi.getGroupApi().getGroup(projectOwner);
            return new GitLabGroup(group.getName(), group.getWebUrl(), group.getAvatarUrl(),
                group.getId(), group.getFullName(), group.getDescription());
        } catch (GitLabApiException e) {
            try {
                User user = gitLabApi.getUserApi().getUser(projectOwner);
                // If invalid projectOwner, null is returned and doesn't throw an exception
                // TODO: New GitLab4J API release after fixing https://github.com/gitlab4j/gitlab4j-api/issues/408
                if (user == null) {
                    throw new IllegalStateException("Owner is neither a user/group/subgroup", e);
                }
                return new GitLabUser(user.getName(), user.getWebUrl(), user.getAvatarUrl(),
                    user.getId());
            } catch (GitLabApiException e1) {
                throw new IllegalStateException("Unable to fetch User", e);
            }
        }
    }
}
