package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Hudson;
import java.util.Objects;
import jenkins.scm.api.metadata.AvatarMetadataAction;
import org.kohsuke.stapler.Stapler;

public class GitLabAvatar extends AvatarMetadataAction {

    private final String avatar;

    public GitLabAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public String getAvatarImageOf(@NonNull String size) {
        if (avatar == null) {
            // fall back to the generic github org icon
            String image = avatarIconClassNameImageOf(getAvatarIconClassName(), size);
            return image != null
                    ? image
                    : (Stapler.getCurrentRequest().getContextPath() + Hudson.RESOURCE_PATH
                    + "/plugin/gitlab-branch-source/images/" + size + "/icon-gitlab.png");
        } else {
            String[] xy = size.split("x");
            if (xy.length == 0) return avatar;
            if (avatar.contains("?")) return avatar + "&s=" + xy[0];
            else return avatar + "?s=" + xy[0];
        }
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
