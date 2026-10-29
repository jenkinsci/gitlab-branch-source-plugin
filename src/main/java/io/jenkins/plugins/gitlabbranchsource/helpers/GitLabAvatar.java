package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import java.util.Objects;
import jenkins.scm.api.metadata.AvatarMetadataAction;
import org.apache.commons.lang3.StringUtils;

public class GitLabAvatar extends AvatarMetadataAction {

    private final String avatar;
    private final String serverName;
    private final String ownerFullName;

    public GitLabAvatar(String avatar) {
        this(avatar, null, null);
    }

    public GitLabAvatar(String avatar, String serverName, Item owner) {
        this.avatar = avatar;
        this.serverName = serverName;
        this.ownerFullName = owner == null ? null : owner.getFullName();
    }

    @Override
    public String getAvatarImageOf(@NonNull String size) {
        return StringUtils.isBlank(avatar) ? null : GitLabAvatarCache.buildUrl(avatar, size, serverName, ownerFullName);
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

        return Objects.equals(avatar, that.avatar)
                && Objects.equals(serverName, that.serverName)
                && Objects.equals(ownerFullName, that.ownerFullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avatar, serverName, ownerFullName);
    }
}
