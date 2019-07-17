package io.jenkins.plugins.gitlabbranchsource;

import com.damnhandy.uri.template.UriTemplate;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabOwner;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabUser;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.JenkinsLocationConfiguration;
import jenkins.scm.api.SCMNavigatorOwner;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;

public class GitLabWebhookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabWebhookListener.class.getName());

    public static void register(SCMNavigatorOwner owner, GitLabSCMNavigator navigator,
                                WebhookRegistration mode, String credentialsId) {
        PersonalAccessToken credentials;
        String serverUrl = GitLabHelper.getServerUrlFromName(navigator.getServerName());
        switch (mode) {
            case DISABLE:
                return;
            case SYSTEM:
                GitLabServer server = GitLabServers.get().findServer(serverUrl);
                if (server == null || !server.isManageHooks()) {
                    return;
                }
                credentials = server.getCredentials();
                break;
            case ITEM:
                credentials = navigator.credentials(owner);
                break;
            default:
                return;
        }
        if (credentials == null) {
            return;
        }
        JenkinsLocationConfiguration locationConfiguration = JenkinsLocationConfiguration.get();
        String rootUrl = locationConfiguration.getUrl();
        if (StringUtils.isBlank(rootUrl) || rootUrl.startsWith("http://localhost:")) {
            return;
        }
        String hookUrl =
                UriTemplate.buildFromTemplate(rootUrl).literal("gitlab-webhook").literal("/post").build().expand();
        try {
            GitLabApi gitLabApi = new GitLabApi(serverUrl, credentials.getToken().getPlainText());
            gitLabApi.getUserApi().getCurrentUser();
            GitLabOwner gitLabOwner = GitLabOwner.fetchOwner(gitLabApi, navigator.getProjectOwner());
            List<Project> projects;
            // TODO check if user can be supported
            if(gitLabOwner instanceof GitLabUser) {
                return;
            } else {
                Group gitLabGroup = gitLabApi.getGroupApi().getGroup(navigator.getProjectOwner());
                projects = gitLabGroup.getProjects();
            }
            if(projects.isEmpty()) {
                LOGGER.log(Level.WARNING,
                        "Group is empty!");
                return;
            }
            // Since GitLab doesn't allow API calls on Group WebHooks.
            // So fetching a list of web hooks in individual projects inside the group
            List<ProjectHook> validHooks = new ArrayList<>();
            // Filters all projectHooks and returns an empty Project Hook or valid project hook per project
            for(Project p : projects) {
                validHooks.add(gitLabApi.getProjectApi().getHooksStream(p)
                                        .filter(hook -> hookUrl.equals(hook.getUrl()))
                                        .findFirst()
                                        .orElse(new ProjectHook()));
            }
            for(ProjectHook hook : validHooks) {
                if(hook.getId() == null) {
                    Project project = projects.get(validHooks.indexOf(hook));
                    ProjectHook enabledHooks = new ProjectHook();
                    enabledHooks.setIssuesEvents(true);
                    enabledHooks.setPushEvents(true);
                    enabledHooks.setMergeRequestsEvents(true);
                    // TODO add secret token, add more events give option for sslVerification
                    gitLabApi.getProjectApi().addHook(project, hookUrl, enabledHooks, true, "");
                }
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING,
                    "Could not manage groups hooks for " + navigator.getProjectOwner() + " on " + serverUrl, e);
        }
    }

    public static void register(SCMSourceOwner owner, GitLabSCMSource source,
                                WebhookRegistration mode, String credentialsId) {
        PersonalAccessToken credentials;
        String serverUrl = GitLabHelper.getServerUrlFromName(source.getServerName());
        switch (mode) {
            case DISABLE:
                return;
            case SYSTEM:
                GitLabServer server = GitLabServers.get().findServer(serverUrl);
                if (server == null || !server.isManageHooks()) {
                    return;
                }
                credentials = server.getCredentials();
                break;
            case ITEM:
                credentials = source.credentials();
                break;
            default:
                return;
        }
        if (credentials == null) {
            return;
        }
        JenkinsLocationConfiguration locationConfiguration = JenkinsLocationConfiguration.get();
        String rootUrl = locationConfiguration.getUrl();
        if (StringUtils.isBlank(rootUrl) || rootUrl.startsWith("http://localhost:")) {
            return;
        }
        String hookUrl =
                UriTemplate.buildFromTemplate(rootUrl).literal("gitlab-webhook").literal("/post").build().expand();
        try {
            GitLabApi gitLabApi = new GitLabApi(serverUrl, credentials.getToken().getPlainText());
            gitLabApi.getUserApi().getCurrentUser();
            Project gitlabProject = gitLabApi.getProjectApi().getProject(source.getProjectOwner());
            try {
                gitLabApi.getRepositoryApi().getTree(gitlabProject);
            } catch (GitLabApiException e) {
                LOGGER.log(Level.WARNING,
                        "Project is empty!", e);
                return;
            }
            // Since GitLab doesn't allow API calls on Group WebHooks.
            // So fetching a list of web hooks in individual projects inside the group
            // Filters all projectHooks and returns an empty Project Hook or valid project hook per project
            ProjectHook validHook = gitLabApi.getProjectApi().getHooksStream(gitlabProject)
                            .filter(hook -> hookUrl.equals(hook.getUrl()))
                            .findFirst()
                            .orElse(new ProjectHook());
            if(validHook.getId() == null) {
                ProjectHook enabledHooks = new ProjectHook();
                enabledHooks.setIssuesEvents(true);
                enabledHooks.setPushEvents(true);
                enabledHooks.setMergeRequestsEvents(true);
                // TODO add secret token, add more events give option for sslVerification
                gitLabApi.getProjectApi().addHook(gitlabProject, hookUrl, enabledHooks, true, "");
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING,
                    "Could not manage project hooks for " + source.getProjectOwner() + " on " + serverUrl, e);
        }
    }

}
