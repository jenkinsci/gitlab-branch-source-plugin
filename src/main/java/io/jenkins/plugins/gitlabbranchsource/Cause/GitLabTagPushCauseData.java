package io.jenkins.plugins.gitlabbranchsource.Cause;

import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultIntString;
import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultLongString;
import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultVisibilityString;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabTagPushCauseData {

    private Map<String, String> variables = new HashMap<>();

    public GitLabTagPushCauseData(TagPushEvent tagPushEvent) {
        this.variables.put("GITLAB_OBJECT_KIND", defaultString(TagPushEvent.OBJECT_KIND));
        this.variables.put("GITLAB_AFTER", defaultString(tagPushEvent.getAfter()));
        this.variables.put("GITLAB_BEFORE", defaultString(tagPushEvent.getBefore()));
        this.variables.put("GITLAB_REF", defaultString(tagPushEvent.getRef()));
        this.variables.put("GITLAB_CHECKOUT_SHA", defaultString(tagPushEvent.getCheckoutSha()));
        this.variables.put("GITLAB_USER_ID", defaultLongString(tagPushEvent.getUserId()));
        this.variables.put("GITLAB_USER_NAME", defaultString(tagPushEvent.getUserName()));
        this.variables.put("GITLAB_USER_USERNAME", defaultString(tagPushEvent.getUserUsername()));
        this.variables.put("GITLAB_USER_EMAIL", defaultString(tagPushEvent.getUserEmail()));
        this.variables.put("GITLAB_PROJECT_ID", defaultLongString(tagPushEvent.getProjectId()));
        this.variables.put(
                "GITLAB_PROJECT_ID_2",
                defaultLongString(tagPushEvent.getProject().getId()));
        this.variables.put(
                "GITLAB_PROJECT_NAME", defaultString(tagPushEvent.getProject().getName()));
        this.variables.put(
                "GITLAB_PROJECT_DESCRIPTION",
                defaultString(tagPushEvent.getProject().getDescription()));
        this.variables.put(
                "GITLAB_PROJECT_WEB_URL",
                defaultString(tagPushEvent.getProject().getWebUrl()));
        this.variables.put(
                "GITLAB_PROJECT_AVATAR_URL",
                defaultString(tagPushEvent.getProject().getAvatarUrl()));
        this.variables.put(
                "GITLAB_PROJECT_GIT_SSH_URL",
                defaultString(tagPushEvent.getProject().getGitSshUrl()));
        this.variables.put(
                "GITLAB_PROJECT_GIT_HTTP_URL",
                defaultString(tagPushEvent.getProject().getGitHttpUrl()));
        this.variables.put(
                "GITLAB_PROJECT_NAMESPACE",
                defaultString(tagPushEvent.getProject().getNamespace()));
        this.variables.put(
                "GITLAB_PROJECT_VISIBILITY_LEVEL",
                defaultVisibilityString(tagPushEvent.getProject().getVisibilityLevel()));
        this.variables.put(
                "GITLAB_PROJECT_PATH_NAMESPACE",
                defaultString(tagPushEvent.getProject().getPathWithNamespace()));
        this.variables.put(
                "GITLAB_PROJECT_CI_CONFIG_PATH",
                defaultString(tagPushEvent.getProject().getCiConfigPath()));
        this.variables.put(
                "GITLAB_PROJECT_DEFAULT_BRANCH",
                defaultString(tagPushEvent.getProject().getDefaultBranch()));
        this.variables.put(
                "GITLAB_PROJECT_HOMEPAGE",
                defaultString(tagPushEvent.getProject().getHomepage()));
        this.variables.put(
                "GITLAB_PROJECT_URL", defaultString(tagPushEvent.getProject().getUrl()));
        this.variables.put(
                "GITLAB_PROJECT_SSH_URL",
                defaultString(tagPushEvent.getProject().getSshUrl()));
        this.variables.put(
                "GITLAB_PROJECT_HTTP_URL",
                defaultString(tagPushEvent.getProject().getHttpUrl()));
        this.variables.put(
                "GITLAB_REPO_NAME", defaultString(tagPushEvent.getRepository().getName()));
        this.variables.put(
                "GITLAB_REPO_URL", defaultString(tagPushEvent.getRepository().getUrl()));
        this.variables.put(
                "GITLAB_REPO_DESCRIPTION",
                defaultString(tagPushEvent.getRepository().getDescription()));
        this.variables.put(
                "GITLAB_REPO_HOMEPAGE",
                defaultString(tagPushEvent.getRepository().getHomepage()));
        this.variables.put(
                "GITLAB_REPO_GIT_SSH_URL",
                defaultString(tagPushEvent.getRepository().getGit_ssh_url()));
        this.variables.put(
                "GITLAB_REPO_GIT_HTTP_URL",
                defaultString(tagPushEvent.getRepository().getGit_http_url()));
        this.variables.put(
                "GITLAB_REPO_VISIBILITY_LEVEL",
                defaultVisibilityString(tagPushEvent.getRepository().getVisibility_level()));
        this.variables.put("GITLAB_COMMIT_COUNT", defaultIntString(tagPushEvent.getTotalCommitsCount()));
        this.variables.put("GITLAB_REQUEST_URL", defaultString(tagPushEvent.getRequestUrl()));
        this.variables.put("GITLAB_REQUEST_STRING", defaultString(tagPushEvent.getRequestQueryString()));
        this.variables.put("GITLAB_REQUEST_TOKEN", defaultString(tagPushEvent.getRequestSecretToken()));
        this.variables.put("GITLAB_REFS_HEAD", defaultString(tagPushEvent.getRef()));
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        return variables;
    }
}
