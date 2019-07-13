package io.jenkins.plugins.gitlabbranchsource;

import com.damnhandy.uri.template.UriTemplate;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabOwner;
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
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.api.models.Visibility;

public class GitLabWebhookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabWebhookListener.class.getName());

    public static void register(SCMNavigatorOwner owner, GitLabSCMNavigator navigator,
                                WebhookRegistration mode) {
        PersonalAccessToken credentials;
        GitLabServer server = GitLabServers.get().findServer(navigator.getServerName());
        if(server == null) {
            return;
        }
        switch (mode) {
            case DISABLE:
                return;
            case SYSTEM:
                if (!server.isManageHooks()) {
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
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), credentials.getToken().getPlainText());
            GitLabOwner gitLabOwner = GitLabOwner.fetchOwner(gitLabApi, navigator.getProjectOwner());
            List<Project> projects;
            LOGGER.info("Project Owner: "+ gitLabOwner);
            if(gitLabOwner == GitLabOwner.USER) {
                projects = gitLabApi.getProjectApi().getUserProjects(navigator.getProjectOwner(), new ProjectFilter().withVisibility(
                        Visibility.PUBLIC));
            } else {
                projects = gitLabApi.getGroupApi().getProjects(navigator.getProjectOwner());
            }
            LOGGER.info(projects.toString());
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
            LOGGER.info(validHooks.toString());
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
                    "Could not manage groups hooks for " + navigator.getProjectOwner() + " on " + server.getServerUrl(), e);
        }
    }

    public static void register(SCMSourceOwner owner, GitLabSCMSource source,
                                WebhookRegistration mode, String credentialsId) {
        PersonalAccessToken credentials;
        GitLabServer server = GitLabServers.get().findServer(source.getServerName());
        if(server == null) {
            return;
        }        switch (mode) {
            case DISABLE:
                return;
            case SYSTEM:
                if (!server.isManageHooks()) {
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
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), credentials.getToken().getPlainText());
            Project gitlabProject = gitLabApi.getProjectApi().getProject(source.getProjectPath());
            try {
                gitLabApi.getRepositoryApi().getTree(gitlabProject);
            } catch (GitLabApiException e) {
                LOGGER.log(Level.WARNING,
                        "Project is empty!", e);
                return;
            }
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
                    "Could not manage project hooks for " + source.getProjectPath() + " on " + server.getServerUrl(), e);
        }
    }

}
