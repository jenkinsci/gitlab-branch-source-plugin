package io.jenkins.plugins.gitlabbranchsource.helpers;

import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import java.io.Serializable;
import java.util.Objects;

public class GitLabAvatarLocation implements Serializable {

    /**
     * Ensure consistent serialization.
     */
    private static final long serialVersionUID = 1L;

    private final String avatarUrl;
    private final String serverName;
    private final String fullPath;
    private final boolean isProject;

    /**
     * Constructor
     *
     * @param url the external GitLab URL of the source avatar image.
     * @param serverName server to use for API call, null to fall back to URL instead
     * @param fullPath project/group id parameter for API call, null to fall back to URL instead
     * @param isProject does the fullPath represent a project (true) or group (false)
     */
    public GitLabAvatarLocation(String avatarUrl, String serverName, String fullPath, boolean isProject) {
        this.avatarUrl = avatarUrl;
        this.serverName = serverName;
        this.fullPath = fullPath;
        this.isProject = isProject;
    }

    public GitLabAvatarLocation(String avatarUrl) {
        this(avatarUrl, null, null, false);
    }

    public boolean apiAvailable() {
        return (serverName != null && fullPath != null);
    }

    public boolean available() {
        // since the url string will not magically turn itself into a HTTP url it is effectively unusable
        return (apiAvailable()
                || (avatarUrl != null && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))));
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getServerName() {
        return serverName;
    }

    public String getFullPath() {
        return fullPath;
    }

    public boolean isProject() {
        return isProject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GitLabAvatarLocation that = (GitLabAvatarLocation) o;

        return Objects.equals(avatarUrl, that.avatarUrl)
                && Objects.equals(serverName, that.serverName)
                && Objects.equals(fullPath, that.fullPath);
    }

    @Override
    public int hashCode() {
        if (StringUtils.isNotBlank(avatarUrl)) {
            return avatarUrl.hashCode();
        } else if (apiAvailable()) {
            return String.format("%s/%s", serverName, fullPath).hashCode();
        }
        return 0;
    }

    @Override
    public String toString() {
        if (apiAvailable()) {
            return String.format("API://%s/%s", serverName, fullPath);
        } else if (StringUtils.isNotBlank(avatarUrl)) {
            return avatarUrl;
        }
        return "";
    }
}
