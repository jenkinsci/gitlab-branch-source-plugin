package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import static org.apache.commons.lang.StringUtils.defaultString;

@ExportedBean
public class GitLabTagPushCauseData {

    private Map<String, String> variables = new HashMap<>();

    public GitLabTagPushCauseData(TagPushEvent tagPushEvent) {
        this.variables.put("OBJECT_KIND", tagPushEvent.OBJECT_KIND);
        this.variables.put("HOOK_AFTER", defaultString(tagPushEvent.getAfter()));
        this.variables.put("HOOK_BEFORE", defaultString(tagPushEvent.getBefore()));
        this.variables.put("HOOK_REF", defaultString(tagPushEvent.getRef()));
        this.variables.put("HOOK_CHECKOUT_SHA", defaultString(tagPushEvent.getCheckoutSha()));
        this.variables.put("HOOK_USER_ID", defaultIntString(tagPushEvent.getUserId()));
        this.variables.put("HOOK_USER_NAME", defaultString(tagPushEvent.getUserName()));
        this.variables.put("HOOK_USER_EMAIL", defaultString(tagPushEvent.getUserEmail()));
        this.variables.put("HOOK_PROJECT_ID", defaultIntString(tagPushEvent.getProjectId()));
        this.variables.put("HOOK_PROJECT_ID_2", defaultIntString(tagPushEvent.getProjectId()));
        this.variables.put("HOOK_PROJECT_NAME", defaultString(tagPushEvent.getProject().getName()));
        this.variables.put("HOOK_PROJECT_DESCRIPTION", defaultString(tagPushEvent.getProject().getDescription()));
        this.variables.put("HOOK_PROJECT_WEB_URL", defaultString(tagPushEvent.getProject().getWebUrl()));
        this.variables.put("HOOK_PROJECT_AVATAR_URL", defaultString(tagPushEvent.getProject().getAvatarUrl()));
        this.variables.put("HOOK_PROJECT_GIT_SSH_URL", defaultString(tagPushEvent.getProject().getGitSshUrl()));
        this.variables.put("HOOK_PROJECT_GIT_HTTP_URL", defaultString(tagPushEvent.getProject().getGitHttpUrl()));
        this.variables.put("HOOK_PROJECT_NAMESPACE", defaultString(tagPushEvent.getProject().getNamespace()));
        this.variables.put("HOOK_PROJECT_VISIBILITY_LEVEL", defaultString(tagPushEvent.getProject().getVisibilityLevel().toString()));
        this.variables.put("HOOK_PROJECT_PATH_NAMESPACE", defaultString(tagPushEvent.getProject().getPathWithNamespace()));
        this.variables.put("HOOK_PROJECT_CI_CONFIG_PATH", defaultString(tagPushEvent.getProject().getCiConfigPath()));
        this.variables.put("HOOK_PROJECT_DEFAULT_BRANCH", defaultString(tagPushEvent.getProject().getDefaultBranch()));
        this.variables.put("HOOK_PROJECT_HOMEPAGE", defaultString(tagPushEvent.getProject().getHomepage()));
        this.variables.put("HOOK_PROJECT_URL", defaultString(tagPushEvent.getProject().getUrl()));
        this.variables.put("HOOK_PROJECT_SSH_URL", defaultString(tagPushEvent.getProject().getSshUrl()));
        this.variables.put("HOOK_PROJECT_HTTP_URL", defaultString(tagPushEvent.getProject().getHttpUrl()));
        this.variables.put("HOOK_REPO_NAME", defaultString(tagPushEvent.getRepository().getName()));
        this.variables.put("HOOK_REPO_URL", defaultString(tagPushEvent.getRepository().getUrl()));
        this.variables.put("HOOK_REPO_DESCRIPTION", defaultString(tagPushEvent.getRepository().getDescription()));
        this.variables.put("HOOK_REPO_HOMEPAGE", defaultString(tagPushEvent.getRepository().getHomepage()));
        this.variables.put("HOOK_REPO_GIT_SSH_URL", defaultString(tagPushEvent.getRepository().getGit_ssh_url()));
        this.variables.put("HOOK_REPO_GIT_HTTP_URL", defaultString(tagPushEvent.getRepository().getGit_http_url()));
        this.variables.put("HOOK_REPO_VISIBILITY_LEVEL", defaultString(tagPushEvent.getRepository().getVisibility_level().toString()));
        this.variables.put("HOOK_COMMIT_COUNT", defaultIntString(tagPushEvent.getTotalCommitsCount()));
        this.variables.put("HOOK_REQUEST_URL", defaultString(tagPushEvent.getRequestUrl()));
        this.variables.put("HOOK_REQUEST_STRING", defaultString(tagPushEvent.getRequestQueryString()));
        this.variables.put("HOOK_REQUEST_TOKEN", defaultString(tagPushEvent.getRequestSecretToken()));
        this.variables.put("HOOK_REFS_HEAD", defaultString(tagPushEvent.getRef()));
    }

    private String defaultDateString(Date date) {
        return date == null ? "" : date.toString();
    }

    private String defaultIntString(Integer val) {
        return val == null ? "" : val.toString();
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        return variables;
    }
}
