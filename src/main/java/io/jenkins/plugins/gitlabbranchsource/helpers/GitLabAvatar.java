package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.metadata.AvatarMetadataAction;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

public class GitLabAvatar extends AvatarMetadataAction {

    private final String avatar;

    public GitLabAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public String getAvatarImageOf(@NonNull String size) {
        return StringUtils.isBlank(avatar) ? null : GitLabAvatarCache.buildUrl(avatar, size);
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

        return Objects.equals(avatar, that.avatar);
    }

    @Override
    public int hashCode() {
        return avatar != null ? avatar.hashCode() : 0;
    }
}
