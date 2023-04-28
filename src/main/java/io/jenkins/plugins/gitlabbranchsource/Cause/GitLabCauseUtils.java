package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.Date;
import java.util.List;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.webhook.EventLabel.LabelType;

public class GitLabCauseUtils {
    public static int defaultListSize(List<?> anyList) {
        return anyList == null ? 0 : anyList.size();
    }

    public static String defaultLabelString(LabelType labelType) {
        return labelType == null ? "" : labelType.toString();
    }

    public static String defaultBooleanString(Boolean bool) {
        return bool == null ? "" : bool.toString();
    }

    public static String defaultVisibilityString(AccessLevel accessLevel) {
        return accessLevel == null ? "" : accessLevel.toString();
    }

    public static String defaultDateString(Date date) {
        return date == null ? "" : date.toString();
    }

    public static String defaultIntString(Integer val) {
        return val == null ? "" : val.toString();
    }

    public static String defaultLongString(Long val) {
        return val == null ? "" : val.toString();
    }
}
