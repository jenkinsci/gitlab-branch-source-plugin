package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultBooleanString;
import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultDateString;
import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultIntString;
import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultLabelString;
import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultListSize;
import static io.jenkins.plugins.gitlabbranchsource.Cause.GitLabCauseUtils.defaultVisibilityString;
import static org.apache.commons.lang.StringUtils.defaultString;

@ExportedBean
public class GitLabMergeRequestCauseData {

    private Map<String, String> variables = new HashMap<>();

    public GitLabMergeRequestCauseData(MergeRequestEvent mergeRequestEvent) {
        this.variables.put("GITLAB_OBJECT_KIND", defaultString(mergeRequestEvent.OBJECT_KIND));
        this.variables.put("GITLAB_USER_NAME", defaultString(mergeRequestEvent.getUser().getName()));
        this.variables.put("GITLAB_USER_USERNAME", defaultString(mergeRequestEvent.getUser().getUsername()));
        this.variables.put("GITLAB_USER_AVATAR_URL", defaultString(mergeRequestEvent.getUser().getAvatarUrl()));
        this.variables.put("GITLAB_PROJECT_ID", defaultIntString(mergeRequestEvent.getProject().getId()));
        this.variables.put("GITLAB_PROJECT_NAME", defaultString(mergeRequestEvent.getProject().getName()));
        this.variables.put("GITLAB_PROJECT_DESCRIPTION", defaultString(mergeRequestEvent.getProject().getDescription()));
        this.variables.put("GITLAB_PROJECT_WEB_URL", defaultString(mergeRequestEvent.getProject().getWebUrl()));
        this.variables.put("GITLAB_PROJECT_AVATAR_URL", defaultString(mergeRequestEvent.getProject().getAvatarUrl()));
        this.variables.put("GITLAB_PROJECT_GIT_SSH_URL", defaultString(mergeRequestEvent.getProject().getGitSshUrl()));
        this.variables.put("GITLAB_PROJECT_GIT_HTTP_URL", defaultString(mergeRequestEvent.getProject().getGitHttpUrl()));
        this.variables.put("GITLAB_PROJECT_NAMESPACE", defaultString(mergeRequestEvent.getProject().getNamespace()));
        this.variables.put("GITLAB_PROJECT_VISIBILITY_LEVEL", defaultVisibilityString(mergeRequestEvent.getProject().getVisibilityLevel()));
        this.variables.put("GITLAB_PROJECT_PATH_NAMESPACE", defaultString(mergeRequestEvent.getProject().getPathWithNamespace()));
        this.variables.put("GITLAB_PROJECT_CI_CONFIG_PATH", defaultString(mergeRequestEvent.getProject().getCiConfigPath()));
        this.variables.put("GITLAB_PROJECT_DEFAULT_BRANCH", defaultString(mergeRequestEvent.getProject().getDefaultBranch()));
        this.variables.put("GITLAB_PROJECT_HOMEPAGE", defaultString(mergeRequestEvent.getProject().getHomepage()));
        this.variables.put("GITLAB_PROJECT_URL", defaultString(mergeRequestEvent.getProject().getUrl()));
        this.variables.put("GITLAB_PROJECT_SSH_URL", defaultString(mergeRequestEvent.getProject().getSshUrl()));
        this.variables.put("GITLAB_PROJECT_HTTP_URL", defaultString(mergeRequestEvent.getProject().getHttpUrl()));
        this.variables.put("GITLAB_REPO_NAME", defaultString(mergeRequestEvent.getRepository().getName()));
        this.variables.put("GITLAB_REPO_URL", defaultString(mergeRequestEvent.getRepository().getUrl()));
        this.variables.put("GITLAB_REPO_DESCRIPTION", defaultString(mergeRequestEvent.getRepository().getDescription()));
        this.variables.put("GITLAB_REPO_HOMEPAGE", defaultString(mergeRequestEvent.getRepository().getHomepage()));
        this.variables.put("GITLAB_REPO_GIT_SSH_URL", defaultString(mergeRequestEvent.getRepository().getGit_ssh_url()));
        this.variables.put("GITLAB_REPO_GIT_HTTP_URL", defaultString(mergeRequestEvent.getRepository().getGit_http_url()));
        this.variables.put("GITLAB_REPO_VISIBILITY_LEVEL", defaultVisibilityString(mergeRequestEvent.getRepository().getVisibility_level()));
        this.variables.put("GITLAB_OA_ID", defaultIntString(mergeRequestEvent.getObjectAttributes().getId()));
        this.variables.put("GITLAB_OA_TARGET_BRANCH", defaultString(mergeRequestEvent.getObjectAttributes().getTargetBranch()));
        this.variables.put("GITLAB_OA_SOURCE_BRANCH", defaultString(mergeRequestEvent.getObjectAttributes().getSourceBranch()));
        this.variables.put("GITLAB_OA_SOURCE_PROJECT_ID", defaultIntString(mergeRequestEvent.getObjectAttributes().getSourceProjectId()));
        this.variables.put("GITLAB_OA_AUTHOR_ID", defaultIntString(mergeRequestEvent.getObjectAttributes().getAuthorId()));
        this.variables.put("GITLAB_OA_ASSIGNEE_ID", defaultIntString(mergeRequestEvent.getObjectAttributes().getAssigneeId()));
        this.variables.put("GITLAB_OA_TITLE", defaultString(mergeRequestEvent.getObjectAttributes().getTitle()));
        this.variables.put("GITLAB_OA_CREATED_AT", defaultDateString(mergeRequestEvent.getObjectAttributes().getCreatedAt()));
        this.variables.put("GITLAB_OA_UPDATED_AT", defaultDateString(mergeRequestEvent.getObjectAttributes().getUpdatedAt()));
        this.variables.put("GITLAB_OA_MILESTONE_ID", defaultIntString(mergeRequestEvent.getObjectAttributes().getMilestoneId()));
        this.variables.put("GITLAB_OA_STATE", defaultString(mergeRequestEvent.getObjectAttributes().getState()));
        this.variables.put("GITLAB_OA_MERGE_STATUS", defaultString(mergeRequestEvent.getObjectAttributes().getMergeStatus()));
        this.variables.put("GITLAB_OA_TARGET_PROJECT_ID", defaultIntString(mergeRequestEvent.getObjectAttributes().getTargetProjectId()));
        this.variables.put("GITLAB_OA_IID", defaultIntString(mergeRequestEvent.getObjectAttributes().getIid()));
        this.variables.put("GITLAB_OA_DESCRIPTION", defaultString(mergeRequestEvent.getObjectAttributes().getDescription()));
        this.variables.put("GITLAB_OA_SOURCE_NAME", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getName()));
        this.variables.put("GITLAB_OA_SOURCE_DESCRIPTION", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getDescription()));
        this.variables.put("GITLAB_OA_SOURCE_WEB_URL", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getWebUrl()));
        this.variables.put("GITLAB_OA_SOURCE_AVATAR_URL", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getAvatarUrl()));
        this.variables.put("GITLAB_OA_SOURCE_GIT_SSH_URL", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getGitSshUrl()));
        this.variables.put("GITLAB_OA_SOURCE_GIT_HTTP_URL", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getGitHttpUrl()));
        this.variables.put("GITLAB_OA_SOURCE_NAMESPACE", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getNamespace()));
        this.variables.put("GITLAB_OA_SOURCE_VISIBILITY_LEVEL", defaultVisibilityString(mergeRequestEvent.getObjectAttributes().getSource().getVisibilityLevel()));
        this.variables.put("GITLAB_OA_SOURCE_PATH_WITH_NAMESPACE", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getPathWithNamespace()));
        this.variables.put("GITLAB_OA_SOURCE_DEFAULT_BRANCH", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getDefaultBranch()));
        this.variables.put("GITLAB_OA_SOURCE_HOMEPAGE", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getHomepage()));
        this.variables.put("GITLAB_OA_SOURCE_URL", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getUrl()));
        this.variables.put("GITLAB_OA_SOURCE_SSH_URL", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getSshUrl()));
        this.variables.put("GITLAB_OA_SOURCE_HTTP_URL", defaultString(mergeRequestEvent.getObjectAttributes().getSource().getHttpUrl()));
        this.variables.put("GITLAB_OA_TARGET_NAME", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getName()));
        this.variables.put("GITLAB_OA_TARGET_DESCRIPTION", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getDescription()));
        this.variables.put("GITLAB_OA_TARGET_WEB_URL", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getWebUrl()));
        this.variables.put("GITLAB_OA_TARGET_AVATAR_URL", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getAvatarUrl()));
        this.variables.put("GITLAB_OA_TARGET_GIT_SSH_URL", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getGitSshUrl()));
        this.variables.put("GITLAB_OA_TARGET_GIT_HTTP_URL", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getGitHttpUrl()));
        this.variables.put("GITLAB_OA_TARGET_NAMESPACE", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getNamespace()));
        this.variables.put("GITLAB_OA_TARGET_VISIBILITY_LEVEL", defaultVisibilityString(mergeRequestEvent.getObjectAttributes().getTarget().getVisibilityLevel()));
        this.variables.put("GITLAB_OA_TARGET_PATH_WITH_NAMESPACE", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getPathWithNamespace()));
        this.variables.put("GITLAB_OA_TARGET_DEFAULT_BRANCH", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getDefaultBranch()));
        this.variables.put("GITLAB_OA_TARGET_HOMEPAGE", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getHomepage()));
        this.variables.put("GITLAB_OA_TARGE_URL", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getUrl()));
        this.variables.put("GITLAB_OA_TARGET_SSH_URL", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getSshUrl()));
        this.variables.put("GITLAB_OA_TARGET_HTTP_URL", defaultString(mergeRequestEvent.getObjectAttributes().getTarget().getHttpUrl()));
        int totalLabels = defaultListSize(mergeRequestEvent.getLabels());
        for(int i = 0; i < totalLabels; i++) {
            this.variables.put("GITLAB_LABEL_ID_" + i, defaultIntString(mergeRequestEvent.getLabels().get(i).getId()));
            this.variables.put("GITLAB_LABEL_TITLE_" + i, defaultString(mergeRequestEvent.getLabels().get(i).getTitle()));
            this.variables.put("GITLAB_LABEL_COLOR_" + i, defaultString(mergeRequestEvent.getLabels().get(i).getColor()));
            this.variables.put("GITLAB_LABEL_PROJECT_ID_" + i, defaultIntString(mergeRequestEvent.getLabels().get(i).getProjectId()));
            this.variables.put("GITLAB_LABEL_CREATED_AT_" + i, defaultDateString(mergeRequestEvent.getLabels().get(i).getCreatedAt()));
            this.variables.put("GITLAB_LABEL_UPDATED_AT_" + i, defaultDateString(mergeRequestEvent.getLabels().get(i).getUpdatedAt()));
            this.variables.put("GITLAB_LABEL_TEMPLATE_" + i, defaultBooleanString(mergeRequestEvent.getLabels().get(i).getTemplate()));
            this.variables.put("GITLAB_LABEL_DESCRIPTION_" + i, defaultString(mergeRequestEvent.getLabels().get(i).getDescription()));
            this.variables.put("GITLAB_LABEL_TYPE_" + i, defaultLabelString(mergeRequestEvent.getLabels().get(i).getType()));
            this.variables.put("GITLAB_LABEL_GROUP_ID_" + i, defaultIntString(mergeRequestEvent.getLabels().get(i).getGroupId()));
        }
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        return variables;
    }
}
