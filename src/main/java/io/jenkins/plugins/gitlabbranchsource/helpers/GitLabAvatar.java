package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import java.util.Objects;
import jenkins.scm.api.metadata.AvatarMetadataAction;

public class GitLabAvatar extends AvatarMetadataAction {

    private final GitLabAvatarLocation location;

    /**
     * Back compat, to keep existing configs working upon plugin upgrade
     */
    private final String avatar;

    public GitLabAvatar(String avatarUrl, String serverName, String projectPath, boolean isProject) {
        this.avatar = null;
        this.location = new GitLabAvatarLocation(avatarUrl, serverName, projectPath, isProject);
    }

    public GitLabAvatar(String avatarUrl) {
        this.avatar = null;
        this.location = new GitLabAvatarLocation(avatarUrl);
    }

    @Override
    public String getAvatarImageOf(@NonNull String size) {
        if (StringUtils.isNotBlank(avatar)) {
            // Back compat, to keep existing configs working upon plugin upgrade
            return GitLabAvatarCache.buildUrl(new GitLabAvatarLocation(avatar), size);
        }
        return location != null && location.available() ? GitLabAvatarCache.buildUrl(location, size) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GitLabAvatar that = (GitLabAvatar) o;

        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return location != null ? location.hashCode() : 0;
    }
}
