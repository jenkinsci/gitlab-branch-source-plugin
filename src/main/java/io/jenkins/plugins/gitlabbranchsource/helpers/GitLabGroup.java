package io.jenkins.plugins.gitlabbranchsource.helpers;

public class GitLabGroup extends GitLabOwner {

    private String fullName;
    private String description;

    public GitLabGroup(String name, String webUrl, String avatarUrl, Long id, String fullName, String description) {
        super(name, webUrl, avatarUrl, id);
        this.fullName = fullName;
        this.description = description;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String getWord() {
        if (fullName.indexOf('/') == -1) {
            return "Group";
        }
        return "Subgroup";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
