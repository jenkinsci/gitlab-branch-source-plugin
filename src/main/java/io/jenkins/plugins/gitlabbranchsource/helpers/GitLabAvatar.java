package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import jenkins.scm.api.metadata.AvatarMetadataAction;
import org.apache.commons.lang.StringUtils;

public class GitLabAvatar extends AvatarMetadataAction {

    private final String avatar;

    public GitLabAvatar(String avatar) {
        this.avatar = avatar;
    }

//    @Override
//    public String getAvatarImageOf(@NonNull String size) {
//        if (avatar == null) {
//            // fall back to the generic gitlab logo
//            String image = avatarIconClassNameImageOf(getAvatarIconClassName(), size);
//            return image != null
//                ? image
//                : (Stapler.getCurrentRequest().getContextPath() + Hudson.RESOURCE_PATH
//                    + "/plugin/gitlab-branch-source/images/" + size + "/gitlab-logo.png");
//        } else {
//            String[] xy = size.split("x");
//            if (xy.length == 0) {
//                return avatar;
//            }
//            if (avatar.contains("?")) {
//                return avatar + "&s=" + xy[0];
//            } else {
//                return avatar + "?s=" + xy[0];
//            }
//        }
//    }

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
