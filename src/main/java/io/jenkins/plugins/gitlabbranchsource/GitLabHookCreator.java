package io.jenkins.plugins.gitlabbranchsource;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMNavigatorOwner;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.api.models.SystemHook;

import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.getProxyConfig;

public class GitLabHookCreator {

    public static final Logger LOGGER = Logger.getLogger(GitLabHookCreator.class.getName());

    public static void register(SCMNavigatorOwner owner, GitLabSCMNavigator navigator,
        GitLabHookRegistration systemhookMode) {
        PersonalAccessToken credentials;
        GitLabServer server = GitLabServers.get().findServer(navigator.getServerName());
        if (server == null) {
            return;
        }
        switch (systemhookMode) {
            case DISABLE:
                return;
            case SYSTEM:
                if (!server.isManageSystemHooks()) {
                    return;
                }
                credentials = server.getCredentials();
                if (credentials == null) {
                    LOGGER.log(Level.WARNING, "No System credentials added, cannot create system hook");
                }
                break;
            case ITEM:
                credentials = navigator.credentials(owner);
                if (credentials == null) {
                    LOGGER.log(Level.WARNING, "No Item credentials added, cannot create system hook");
                }
                break;
            default:
                return;
        }
        // add system hooks
        if (credentials != null) {
            createSystemHookWhenMissing(server, credentials);
        }
    }

    public static void register(GitLabSCMSource source,
        GitLabHookRegistration webhookMode, GitLabHookRegistration systemhookMode) {
        PersonalAccessToken credentials = null;
        GitLabServer server = GitLabServers.get().findServer(source.getServerName());
        if (server == null) {
            return;
        }
        switch (webhookMode) {
            case DISABLE:
                break;
            case SYSTEM:
                if (!server.isManageWebHooks()) {
                    break;
                }
                credentials = server.getCredentials();
                if (credentials == null) {
                    LOGGER.log(Level.WARNING, "No System credentials added, cannot create web hook");
                }
                break;
            case ITEM:
                credentials = source.credentials();
                if (credentials == null) {
                    LOGGER.log(Level.WARNING, "No Item credentials added, cannot create web hook");
                }
                break;
            default:
                return;
        }
        String hookUrl = getHookUrl(server, true);
        String secretToken = server.getSecretTokenAsPlainText();
        if (hookUrl.equals("")) {
            return;
        }
        if (credentials != null) {
            try {
                GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(),
                    credentials.getToken().getPlainText(), null, getProxyConfig(server.getServerUrl()));
                createWebHookWhenMissing(gitLabApi, source.getProjectPath(), hookUrl, secretToken);
            } catch (GitLabApiException e) {
                LOGGER.log(Level.WARNING,
                    "Could not manage project hooks for " + source.getProjectPath() + " on "
                        + server.getServerUrl(), e);
            }
        }
        switch (systemhookMode) {
            case DISABLE:
                return;
            case SYSTEM:
                if (!server.isManageSystemHooks()) {
                    return;
                }
                credentials = server.getCredentials();
                if (credentials == null) {
                    LOGGER.log(Level.WARNING, "No System credentials added, cannot create system hook");
                }
                break;
            case ITEM:
                credentials = source.credentials();
                if (credentials == null) {
                    LOGGER.log(Level.WARNING, "No Item credentials added, cannot create system hook");
                }
                break;
            default:
                return;
        }
        // add system hooks
        if (credentials != null) {
            createSystemHookWhenMissing(server, credentials);
        }
    }

    public static void createSystemHookWhenMissing(GitLabServer server,
        PersonalAccessToken credentials) {
        String systemHookUrl = getHookUrl(server, false);
        try {
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(),
                credentials.getToken().getPlainText(), null, getProxyConfig(server.getServerUrl()));
            SystemHook systemHook = gitLabApi.getSystemHooksApi()
                .getSystemHookStream()
                .filter(hook -> systemHookUrl.equals(hook.getUrl()))
                .findFirst()
                .orElse(null);
            if (systemHook == null) {
                gitLabApi.getSystemHooksApi().addSystemHook(systemHookUrl, server.getSecretTokenAsPlainText(),
                    false, false, false);
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.INFO, "User is not admin so cannot set system hooks", e);
        }
    }

    /**
     * @deprecated use {@link #getHookUrl(GitLabServer,boolean)} instead
     */
    @Deprecated
    public static String getHookUrl(boolean isWebHook) {
        return getHookUrl(null, isWebHook);
    }

    /**
     * @param server the {@code GitLabServer} for which the hooks URL would be created. If not {@code null} and it
     *        has a {@link GitLabServer#getHooksRootUrl()}, then the hook URL will be based on this root URL.
     *        Otherwise, the hook URL will be based on {@link Jenkins#getRootUrl()}.
     * @param isWebHook {@code true} to get the webhook URL, {@code false} for the systemhook URL
     * @return a webhook or systemhook URL
     */
    public static String getHookUrl(GitLabServer server, boolean isWebHook) {
        String rootUrl = (server == null || server.getHooksRootUrl() == null)
                ? Jenkins.get().getRootUrl()
                : server.getHooksRootUrl();
        if (StringUtils.isBlank(rootUrl)) {
            return "";
        }
        checkURL(rootUrl);
        UriTemplateBuilder templateBuilder = UriTemplate.buildFromTemplate(rootUrl);
        if (isWebHook) {
            templateBuilder.literal("gitlab-webhook");
        } else {
            templateBuilder.literal("gitlab-systemhook");
        }
        return templateBuilder.literal("/post").build().expand();
    }

    static void checkURL(String url) {
        try {
            URL anURL = new URL(url);
            if ("localhost".equals(anURL.getHost())) {
                throw new IllegalStateException(
                    "Jenkins URL cannot start with http://localhost \nURL is: " + url);
            }
            if (!anURL.getHost().contains(".")) {
                throw new IllegalStateException(
                    "You must use a fully qualified domain name for Jenkins URL, this is required by GitLab"
                        + "\nURL is: " + url);
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad Jenkins URL\nURL is: " + url);
        }
    }


    public static ProjectHook createWebHook() {
        ProjectHook enabledHooks = new ProjectHook();
        enabledHooks.setPushEvents(true);
        enabledHooks.setMergeRequestsEvents(true);
        enabledHooks.setTagPushEvents(true);
        enabledHooks.setNoteEvents(true);
        return enabledHooks;
    }

    public static String createWebHookWhenMissing(GitLabApi gitLabApi, String project,
        String hookUrl, String secretToken)
        throws GitLabApiException {
        ProjectHook projectHook = gitLabApi.getProjectApi().getHooksStream(project)
            .filter(hook -> hookUrl.equals(hook.getUrl()))
            .findFirst()
            .orElseGet(GitLabHookCreator::createWebHook);
        if (projectHook.getId() == null) {
            gitLabApi.getProjectApi().addHook(project, hookUrl, projectHook, false, secretToken);
            return "created";
        }
        // Primarily done due to legacy reason, secret token might not be configured in previous releases. So setting up hook url with the token.
        if(!isTokenEqual(projectHook.getToken(), secretToken)) {
            projectHook.setToken(secretToken);
            gitLabApi.getProjectApi().modifyHook(projectHook);
            return "modified";
        }
        return "already created";
    }

    public static boolean isTokenEqual(String str1, String str2) {
        if(str1 == null && str2.isEmpty()) {
            return true;
        }
        if(str1 == null) {
            return false;
        }
        return str1.equals(str2);
    }
}
