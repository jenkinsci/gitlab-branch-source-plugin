package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.PushEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import static org.apache.commons.lang.StringUtils.defaultString;

@ExportedBean
public class GitLabPushCauseData {

    private Map<String, String> variables = new HashMap<>();

    public GitLabPushCauseData(PushEvent pushEvent) {
        this.variables.put("OBJECT_KIND", pushEvent.OBJECT_KIND);
        this.variables.put("HOOK_AFTER", defaultString(pushEvent.getAfter()));
        this.variables.put("HOOK_BEFORE", defaultString(pushEvent.getBefore()));
        this.variables.put("HOOK_REF", defaultString(pushEvent.getRef()));
        this.variables.put("HOOK_CHECKOUT_SHA", defaultString(pushEvent.getCheckoutSha()));
        this.variables.put("HOOK_USER_ID", defaultIntString(pushEvent.getUserId()));
        this.variables.put("HOOK_USER_NAME", defaultString(pushEvent.getUserName()));
        this.variables.put("HOOK_USER_EMAIL", defaultString(pushEvent.getUserEmail()));
        this.variables.put("HOOK_PROJECT_ID", defaultIntString(pushEvent.getProjectId()));
        this.variables.put("HOOK_PROJECT_ID_2", defaultIntString(pushEvent.getProjectId()));
        this.variables.put("HOOK_PROJECT_NAME", defaultString(pushEvent.getProject().getName()));
        this.variables.put("HOOK_PROJECT_DESCRIPTION", defaultString(pushEvent.getProject().getDescription()));
        this.variables.put("HOOK_PROJECT_WEB_URL", defaultString(pushEvent.getProject().getWebUrl()));
        this.variables.put("HOOK_PROJECT_AVATAR_URL", defaultString(pushEvent.getProject().getAvatarUrl()));
        this.variables.put("HOOK_PROJECT_GIT_SSH_URL", defaultString(pushEvent.getProject().getGitSshUrl()));
        this.variables.put("HOOK_PROJECT_GIT_HTTP_URL", defaultString(pushEvent.getProject().getGitHttpUrl()));
        this.variables.put("HOOK_PROJECT_NAMESPACE", defaultString(pushEvent.getProject().getNamespace()));
        this.variables.put("HOOK_PROJECT_VISIBILITY_LEVEL", defaultString(pushEvent.getProject().getVisibilityLevel().toString()));
        this.variables.put("HOOK_PROJECT_PATH_NAMESPACE", defaultString(pushEvent.getProject().getPathWithNamespace()));
        this.variables.put("HOOK_PROJECT_CI_CONFIG_PATH", defaultString(pushEvent.getProject().getCiConfigPath()));
        this.variables.put("HOOK_PROJECT_DEFAULT_BRANCH", defaultString(pushEvent.getProject().getDefaultBranch()));
        this.variables.put("HOOK_PROJECT_HOMEPAGE", defaultString(pushEvent.getProject().getHomepage()));
        this.variables.put("HOOK_PROJECT_URL", defaultString(pushEvent.getProject().getUrl()));
        this.variables.put("HOOK_PROJECT_SSH_URL", defaultString(pushEvent.getProject().getSshUrl()));
        this.variables.put("HOOK_PROJECT_HTTP_URL", defaultString(pushEvent.getProject().getHttpUrl()));
        this.variables.put("HOOK_REPO_NAME", defaultString(pushEvent.getRepository().getName()));
        this.variables.put("HOOK_REPO_URL", defaultString(pushEvent.getRepository().getUrl()));
        this.variables.put("HOOK_REPO_DESCRIPTION", defaultString(pushEvent.getRepository().getDescription()));
        this.variables.put("HOOK_REPO_HOMEPAGE", defaultString(pushEvent.getRepository().getHomepage()));
        this.variables.put("HOOK_REPO_GIT_SSH_URL", defaultString(pushEvent.getRepository().getGit_ssh_url()));
        this.variables.put("HOOK_REPO_GIT_HTTP_URL", defaultString(pushEvent.getRepository().getGit_http_url()));
        this.variables.put("HOOK_REPO_VISIBILITY_LEVEL", defaultString(pushEvent.getRepository().getVisibility_level().toString()));
        this.variables.put("HOOK_COMMIT_COUNT", defaultIntString(pushEvent.getTotalCommitsCount()));
        int totalCommitsCount = pushEvent.getCommits().size();
        for(int index = 0; index < totalCommitsCount; index++) {
            this.variables.put("HOOK_COMMIT_ID_" + index+1,  defaultString(pushEvent.getCommits().get(index).getId()));
            this.variables.put("HOOK_COMMIT_MESSAGE_" + index+1, defaultString(pushEvent.getCommits().get(index).getMessage()));
            this.variables.put("HOOK_COMMIT_TIMESTAMP_" + index+1, defaultDateString(pushEvent.getCommits().get(index).getTimestamp()));
            this.variables.put("HOOK_COMMIT_URL_" + index+1, defaultString(pushEvent.getCommits().get(index).getUrl()));
            this.variables.put("HOOK_COMMIT_AUTHOR_AVATAR_URL_" + index+1, defaultString(pushEvent.getCommits().get(index).getAuthor().getAvatarUrl()));
            this.variables.put("HOOK_COMMIT_AUTHOR_CREATED_AT_" + index+1, defaultDateString(pushEvent.getCommits().get(index).getAuthor().getCreatedAt()));
            this.variables.put("HOOK_COMMIT_AUTHOR_EMAIL_" + index+1, defaultString(pushEvent.getCommits().get(index).getAuthor().getEmail()));
            this.variables.put("HOOK_COMMIT_AUTHOR_ID_" + index+1, defaultIntString(pushEvent.getCommits().get(index).getAuthor().getId()));
            this.variables.put("HOOK_COMMIT_AUTHOR_NAME_" + index+1, defaultString(pushEvent.getCommits().get(index).getAuthor().getName()));
            this.variables.put("HOOK_COMMIT_AUTHOR_STATE_" + index+1, defaultString(pushEvent.getCommits().get(index).getAuthor().getState()));
            this.variables.put("HOOK_COMMIT_AUTHOR_USERNAME_" + index+1, defaultString(pushEvent.getCommits().get(index).getAuthor().getUsername()));
            this.variables.put("HOOK_COMMIT_AUTHOR_WEB_URL_" + index+1, defaultString(pushEvent.getCommits().get(index).getAuthor().getWebUrl()));
            this.variables.put("HOOK_COMMIT_ADDED_" + index+1, String.join(", ", pushEvent.getCommits().get(index).getAdded()));
            this.variables.put("HOOK_COMMIT_MODIFIED_" + index+1, String.join(", ", pushEvent.getCommits().get(index).getModified()));
            this.variables.put("HOOK_COMMIT_REMOVED_" + index+1, String.join(", ", pushEvent.getCommits().get(index).getRemoved()));
        }
        this.variables.put("HOOK_REQUEST_URL", defaultString(pushEvent.getRequestUrl()));
        this.variables.put("HOOK_REQUEST_STRING", defaultString(pushEvent.getRequestQueryString()));
        this.variables.put("HOOK_REQUEST_TOKEN", defaultString(pushEvent.getRequestSecretToken()));
        this.variables.put("HOOK_REFS_HEAD", defaultString(pushEvent.getRef()));
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
