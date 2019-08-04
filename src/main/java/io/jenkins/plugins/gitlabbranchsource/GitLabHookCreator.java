package io.jenkins.plugins.gitlabbranchsource;

import com.damnhandy.uri.template.UriTemplate;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.JenkinsLocationConfiguration;
import jenkins.scm.api.SCMNavigatorOwner;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.ProjectHook;

public class GitLabHookCreator {

    public static final Logger LOGGER = Logger.getLogger(GitLabHookCreator.class.getName());

    public static void register(SCMNavigatorOwner owner, GitLabSCMNavigator navigator,
                                GitLabHookRegistration webhookMode, GitLabHookRegistration systemhookMode) {
        List<String> projects = new ArrayList<>(navigator.getNavigatorProjects());
        if(projects.isEmpty()) {
            LOGGER.log(Level.WARNING,
                    "Group is empty!");
            return;
        }
        PersonalAccessToken credentials;
        GitLabServer server = GitLabServers.get().findServer(navigator.getServerName());
        if(server == null) {
            return;
        }
        switch (webhookMode) {
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
        String hookUrl = getHookUrl();
        if(hookUrl.equals("")) {
            return;
        }
        // add web hooks
        try {
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), credentials.getToken().getPlainText());
            // Since GitLab doesn't allow API calls on Group WebHooks.
            // So fetching a list of web hooks in individual projects inside the group
            // Filters all projectHooks and returns an empty Project Hook or valid project hook per project
            for(String p : projects) {
                createHookWhenMissing(gitLabApi, p, hookUrl);
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING,
                    "Could not manage groups hooks for " + navigator.getProjectOwner() + " on " + server.getServerUrl(), e);
        }
        switch (systemhookMode) {
            case DISABLE:
                return;
            case SYSTEM:
                if (!server.isManageSystemHooks()) {
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
        // add system hooks
        createSystemHook(server, credentials);
    }

    public static void register(GitLabSCMSource source,
                                GitLabHookRegistration webhookMode, GitLabHookRegistration systemhookMode) {
        PersonalAccessToken credentials;
        GitLabServer server = GitLabServers.get().findServer(source.getServerName());
        if(server == null) {
            return;
        }
        switch (webhookMode) {
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
        String hookUrl = getHookUrl();
        if(hookUrl.equals("")) {
            return;
        }
        try {
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), credentials.getToken().getPlainText());
            createHookWhenMissing(gitLabApi, source.getProjectPath(), hookUrl);
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING,
                    "Could not manage project hooks for " + source.getProjectPath() + " on " + server.getServerUrl(), e);
        }
        switch (systemhookMode) {
            case DISABLE:
                return;
            case SYSTEM:
                if (!server.isManageSystemHooks()) {
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
        // add system hooks
        createSystemHook(server, credentials);
    }

    private static void createSystemHook(GitLabServer server, PersonalAccessToken credentials) {
        try {
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), credentials.getToken().getPlainText());
            gitLabApi.getSystemHooksApi().addSystemHook(getHookUrl(), "",
                    false, false, false);
        } catch (GitLabApiException e) {
            LOGGER.info("User is not admin so cannot set system hooks");
            e.printStackTrace();
        }
    }
    public static String getHookUrl() {
        JenkinsLocationConfiguration locationConfiguration = JenkinsLocationConfiguration.get();
        String rootUrl = locationConfiguration.getUrl();
        if (StringUtils.isBlank(rootUrl) || rootUrl.startsWith("http://localhost:")) {
            return "";
        }
        return UriTemplate.buildFromTemplate(rootUrl).literal("gitlab-webhook").literal("/post").build().expand();
    }

    public static ProjectHook createHook() {
        ProjectHook enabledHooks = new ProjectHook();
        enabledHooks.setPushEvents(true);
        enabledHooks.setMergeRequestsEvents(true);
        enabledHooks.setTagPushEvents(true);
        enabledHooks.setEnableSslVerification(false);
        // TODO add secret token, add more events give option for sslVerification
        return enabledHooks;
    }

    private static void createHookWhenMissing(GitLabApi gitLabApi, String project, String hookUrl)
            throws GitLabApiException {
        ProjectHook projectHook = gitLabApi.getProjectApi().getHooksStream(project)
                .filter(hook -> hookUrl.equals(hook.getUrl()))
                .findFirst()
                .orElseGet(GitLabHookCreator::createHook);
        if(projectHook.getId() == null) {
            gitLabApi.getProjectApi().addHook(project, hookUrl, projectHook, false, "");
        }
    }
}
