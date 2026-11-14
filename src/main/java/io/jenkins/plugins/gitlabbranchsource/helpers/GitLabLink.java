package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import org.jenkins.ui.icon.IconSpec;

/**
 * Link to GitLab
 */
public class GitLabLink implements Action, IconSpec {

    /**
     * The icon class name to use.
     */
    @NonNull
    private final String iconClassName;

    /**
     * Target of the hyperlink to take the user to.
     */
    @NonNull
    private final String url;

    private String displayName;

    public GitLabLink(@NonNull String iconClassName, @NonNull String url) {
        this.iconClassName = iconClassName;
        this.url = url;
        this.displayName = "";
    }

    public GitLabLink(@NonNull String iconClassName, @NonNull String url, String displayName) {
        this.iconClassName = iconClassName;
        this.url = url;
        this.displayName = displayName;
    }

    public static GitLabLink toGroup(String url) {
        return new GitLabLink(GitLabIcons.ICON_GITLAB, url, "Group");
    }

    public static GitLabLink toProject(String url) {
        return new GitLabLink(GitLabIcons.ICON_PROJECT, url, "Project");
    }

    public static GitLabLink toBranch(String url) {
        return new GitLabLink(GitLabIcons.ICON_BRANCH, url, "Branch");
    }

    public static GitLabLink toMergeRequest(String url) {
        return new GitLabLink(GitLabIcons.ICON_MR, url, "Merge Request");
    }

    public static GitLabLink toTag(String url) {
        return new GitLabLink(GitLabIcons.ICON_TAG, url, "Tag");
    }

    public static GitLabLink toCommit(String url) {
        return new GitLabLink(GitLabIcons.ICON_COMMIT, url, "Commit");
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    @Override
    public String getIconClassName() {
        return iconClassName;
    }

    @Override
    public String getIconFileName() {
        // the icon is rendered from getIconClassName() as a themable Jenkins Symbol, no file-based fallback
        // needed
        return null;
    }

    @Override
    public String getDisplayName() {
        return defaultIfBlank(displayName, "GitLab");
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getUrlName() {
        return url;
    }

    @Override
    public int hashCode() {
        int result = iconClassName.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GitLabLink that = (GitLabLink) o;

        if (!iconClassName.equals(that.iconClassName)) {
            return false;
        }
        return url.equals(that.url);
    }

    @Override
    public String toString() {
        return "GitLabLink{" + "iconClassName='" + iconClassName + '\'' + ", url='" + url + '\'' + '}';
    }
}
