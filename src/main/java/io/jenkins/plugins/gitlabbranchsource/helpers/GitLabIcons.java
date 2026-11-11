package io.jenkins.plugins.gitlabbranchsource.helpers;

/**
 * Icon class names used by this plugin, backed by Jenkins Symbols provided by the
 * <a href="https://plugins.jenkins.io/ionicons-api/">ionicons-api</a> and
 * <a href="https://plugins.jenkins.io/oss-symbols-api/">oss-symbols-api</a> plugins.
 * <p>
 * Symbols are rendered inline as raw {@code <svg>} markup, so {@code currentColor} tracks the
 * active Jenkins theme, unlike plain file-based icons which are always rendered as an opaque
 * {@code <img>} regardless of the active theme.
 */
public final class GitLabIcons {

    public static final String ICON_PROJECT = "symbol-folder-outline plugin-ionicons-api";
    public static final String ICON_BRANCH = "symbol-git-branch-outline plugin-ionicons-api";
    public static final String ICON_GITLAB = "symbol-gitlab-icon-color plugin-oss-symbols-api";
    public static final String ICON_COMMIT = "symbol-git-commit-outline plugin-ionicons-api";
    public static final String ICON_MR = "symbol-git-pull-request-outline plugin-ionicons-api";
    public static final String ICON_TAG = "symbol-pricetag-outline plugin-ionicons-api";

    private GitLabIcons() {
        /* no instances allowed */
    }
}
