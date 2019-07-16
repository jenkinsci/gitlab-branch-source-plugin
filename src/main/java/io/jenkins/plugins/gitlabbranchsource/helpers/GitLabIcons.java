package io.jenkins.plugins.gitlabbranchsource.helpers;

import java.util.NoSuchElementException;
import org.jenkins.ui.icon.Icon;


import static org.jenkins.ui.icon.Icon.ICON_LARGE_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_MEDIUM_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_SMALL_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_XLARGE_STYLE;
import static org.jenkins.ui.icon.IconSet.icons;

public final class GitLabIcons {
    public enum Size {
        SMALL("icon-sm", "16x16", ICON_SMALL_STYLE),
        MEDIUM("icon-md", "24x24", ICON_MEDIUM_STYLE),
        LARGE("icon-lg", "32x32", ICON_LARGE_STYLE),
        XLARGE("icon-xlg", "48x48", ICON_XLARGE_STYLE);

        public static Size byDimensions(String dimensions) {
            for (Size s : values()) {
                if (s.dimensions.equals(dimensions)) {
                    return s;
                }
            }
            throw new NoSuchElementException("unknown dimensions: " + dimensions);
        }

        private final String className;
        private final String dimensions;
        private final String style;

        Size(String className, String dimensions, String style) {
            this.className = className;
            this.dimensions = dimensions;
            this.style = style;
        }
    }

    public static final String ICON_PROJECT = "icon-project";
    public static final String ICON_BRANCH = "icon-branch";
    public static final String ICON_COMMIT = "icon-commit";
    public static final String ICON_MERGE_REQUEST = "icon-merge-request";
    public static final String ICON_TAG = "icon-tag";

    public static final String ICON_GITLAB = "icon-gitlab";
    private static final String ICON_PATH = "plugin/gitlab-branch-source/images/";

    public static void initialize() {
        addIcon(ICON_GITLAB);
        addIcon(ICON_PROJECT);
        addIcon(ICON_BRANCH);
        addIcon(ICON_COMMIT);
        addIcon(ICON_MERGE_REQUEST);
        addIcon(ICON_TAG);
    }


    private static String classSpec(String name, Size size) {
        return name + " " + size.className;
    }

    private static void addIcon(String name) {
        for (Size size : Size.values()) {
            icons.addIcon(new Icon(classSpec(name, size), ICON_PATH + size.dimensions + "/" + name + ".png", size.style));
        }
    }

    private GitLabIcons() { /* no instances allowed */}
}
