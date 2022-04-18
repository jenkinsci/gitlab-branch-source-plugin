package io.jenkins.plugins.gitlabbranchsource.helpers;

public class GitLabUser extends GitLabOwner {

    public GitLabUser(String name, String webUrl, String avatarUrl, Long id) {
        super(name, webUrl, avatarUrl, id);
    }

    @Override
    public String getWord() {
        return "User";
    }
}
