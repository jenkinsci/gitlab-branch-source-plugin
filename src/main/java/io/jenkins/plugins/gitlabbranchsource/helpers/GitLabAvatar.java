package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import jenkins.scm.api.metadata.AvatarMetadataAction;
import org.apache.commons.lang3.StringUtils;

public class GitLabAvatar extends AvatarMetadataAction {
    private final String avatar;
    private final String serverName;

    public GitLabAvatar(String avatar) {
        this(avatar, null);
    }

    public GitLabAvatar(String avatar, String serverName) {
        this.avatar = avatar;
        this.serverName = serverName;
    }

    @Override
    public String getAvatarImageOf(@NonNull String size) {
        return StringUtils.isBlank(avatar) ? null : GitLabAvatarCache.buildUrl(avatar, size, serverName);
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
        return Objects.equals(avatar, that.avatar) && Objects.equals(serverName, that.serverName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avatar, serverName);
    }
}
