package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.gitlabbranchsource.retry.GitLabApiRetryWrapper;
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

    @NonNull
    public static GitLabOwner fetchOwner(GitLabApiRetryWrapper gitLabApi, String projectOwner) {
        try {
            Group group = gitLabApi.getGroupApi().getGroup(projectOwner);
            return new GitLabGroup(group.getName(), group.getWebUrl(), group.getAvatarUrl(),
                group.getId(), group.getFullName(), group.getDescription());
        } catch (GitLabApiException e) {
            if (e.getHttpStatus() != 404) {
                throw new IllegalStateException("Unable to fetch Group", e);
            }

            try {
                User user = gitLabApi.getUserApi().getUser(projectOwner);
                // If user is not found, null is returned
                if (user == null) {
                    throw new IllegalStateException(
                        String.format("Owner '%s' is neither a user/group/subgroup", projectOwner));
                }
                return new GitLabUser(user.getName(), user.getWebUrl(), user.getAvatarUrl(),
                    user.getId());
            } catch (GitLabApiException e1) {
                throw new IllegalStateException("Unable to fetch User", e1);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return name;
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
}
