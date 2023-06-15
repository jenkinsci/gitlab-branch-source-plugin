package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.jenkins.ui.icon.Icon.ICON_LARGE_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_MEDIUM_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_SMALL_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_XLARGE_STYLE;
import static org.jenkins.ui.icon.IconSet.icons;

import hudson.init.Initializer;
import java.util.NoSuchElementException;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.jenkins.ui.icon.Icon;
import org.kohsuke.stapler.Stapler;

public final class GitLabIcons {

    public static final String ICON_PROJECT = "gitlab-project";
    public static final String ICON_BRANCH = "gitlab-branch";
    public static final String ICON_GITLAB = "gitlab-logo";
    public static final String ICON_COMMIT = "gitlab-commit";
    public static final String ICON_MR = "gitlab-mr";
    public static final String ICON_TAG = "gitlab-tag";
    private static final String ICON_PATH = "plugin/gitlab-branch-source/images/";

    private GitLabIcons() {
        /* no instances allowed */
    }

    @Initializer
    public static void initialize() {
        addIcon(ICON_GITLAB);
        addIcon(ICON_PROJECT);
        addIcon(ICON_BRANCH);
        addIcon(ICON_COMMIT);
        addIcon(ICON_MR);
        addIcon(ICON_TAG);
    }

    public static String iconFileName(String name, Size size) {
        Icon icon = icons.getIconByClassSpec(classSpec(name, size));
        if (icon == null) {
            return null;
        }

        JellyContext ctx = new JellyContext();
        ctx.setVariable("resURL", Stapler.getCurrentRequest().getContextPath() + Jenkins.RESOURCE_PATH);
        return icon.getQualifiedUrl(ctx);
    }

    public static String iconFilePathPattern(String name) {
        return ICON_PATH + name + ".svg";
    }

    private static String classSpec(String name, Size size) {
        return name + " " + size.className;
    }

    private static void addIcon(String name) {
        for (Size size : Size.values()) {
            icons.addIcon(new Icon(classSpec(name, size), ICON_PATH + "/" + name + ".svg", size.style));
        }
    }

    public enum Size {
        SMALL("icon-sm", "16x16", ICON_SMALL_STYLE),
        MEDIUM("icon-md", "24x24", ICON_MEDIUM_STYLE),
        LARGE("icon-lg", "32x32", ICON_LARGE_STYLE),
        XLARGE("icon-xlg", "48x48", ICON_XLARGE_STYLE);

        private final String className;
        private final String dimensions;
        private final String style;

        Size(String className, String dimensions, String style) {
            this.className = className;
            this.dimensions = dimensions;
            this.style = style;
        }

        public static Size byDimensions(String dimensions) {
            for (Size s : values()) {
                if (s.dimensions.equals(dimensions)) {
                    return s;
                }
            }
            throw new NoSuchElementException("unknown dimensions: " + dimensions);
        }
    }
}
