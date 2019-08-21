package io.jenkins.plugins.gitlabbranchsource;

import com.damnhandy.uri.template.UriTemplate;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import jenkins.scm.api.SCMNavigatorOwner;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.api.models.SystemHook;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

public class GitLabHookCreator {

    public static final Logger LOGGER = Logger.getLogger(GitLabHookCreator.class.getName());

    public static void register(SCMNavigatorOwner owner, GitLabSCMNavigator navigator,
                                GitLabHookRegistration systemhookMode) {
        PersonalAccessToken credentials;
        GitLabServer server = GitLabServers.get().findServer(navigator.getServerName());
        if(server == null) {
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
                if(credentials == null) {
                    LOGGER.info("No System credentials added, cannot create system hook");
                }
                break;
            case ITEM:
                credentials = navigator.credentials(owner);
                if(credentials == null) {
                    LOGGER.info("No Item credentials added, cannot create system hook");
                }
                break;
            default:
                return;
        }
        // add system hooks
        if(credentials != null) {
            createSystemHookWhenMissing(server, credentials);
        }
    }

    public static void register(GitLabSCMSource source,
                                GitLabHookRegistration webhookMode, GitLabHookRegistration systemhookMode) {
        PersonalAccessToken credentials = null;
        GitLabServer server = GitLabServers.get().findServer(source.getServerName());
        if(server == null) {
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
                if(credentials == null) {
                    LOGGER.info("No System credentials added, cannot create web hook");
                }
                break;
            case ITEM:
                credentials = source.credentials();
                if(credentials == null) {
                    LOGGER.info("No Item credentials added, cannot create web hook");
                }
                break;
            default:
                return;
        }
        String hookUrl = getHookUrl(true);
        if(hookUrl.equals("")) {
            return;
        }
        if(credentials != null) {
            try {
                GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), credentials.getToken().getPlainText());
                createWebHookWhenMissing(gitLabApi, source.getProjectPath(), hookUrl);
            } catch (GitLabApiException e) {
                LOGGER.log(Level.WARNING,
                        "Could not manage project hooks for " + source.getProjectPath() + " on " + server.getServerUrl(), e);
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
                if(credentials == null) {
                    LOGGER.info("No System credentials added, cannot create system hook");
                }
                break;
            case ITEM:
                credentials = source.credentials();
                if(credentials == null) {
                    LOGGER.info("No Item credentials added, cannot create system hook");
                }
                break;
            default:
                return;
        }
        // add system hooks
        if(credentials != null) {
            createSystemHookWhenMissing(server, credentials);
        }
    }

    public static void createSystemHookWhenMissing(GitLabServer server, PersonalAccessToken credentials) {
        String systemHookUrl = getHookUrl(false);
        try {
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), credentials.getToken().getPlainText());
            SystemHook systemHook = gitLabApi.getSystemHooksApi()
                    .getSystemHookStream()
                    .filter(hook -> systemHookUrl.equals(hook.getUrl()))
                    .findFirst()
                    .orElse(null);
            if(systemHook == null) {
                gitLabApi.getSystemHooksApi().addSystemHook(systemHookUrl, "",
                        false, false, false);
            }
        } catch (GitLabApiException e) {
            LOGGER.info("User is not admin so cannot set system hooks");
            e.printStackTrace();
        }
    }

    public static String getHookUrl(boolean isWebHook) {
        JenkinsLocationConfiguration locationConfiguration = JenkinsLocationConfiguration.get();
        String rootUrl = locationConfiguration.getUrl();
        if (StringUtils.isBlank(rootUrl)) {
            return "";
        }
        checkURL(rootUrl);
        String pronoun = "gitlab-systemhook";
        if(isWebHook) {
            pronoun = "gitlab-webhook";
        }
        return UriTemplate.buildFromTemplate(rootUrl).literal(pronoun).literal("/post").build().expand();
    }

    static void checkURL(String url) {
        try {
            URL anURL = new URL(url);
            if ("localhost".equals(anURL.getHost())) {
                throw new IllegalStateException(
                    "Jenkins URL cannot start with http://localhost \nURL is:" + url);
            }
            if (!anURL.getHost().contains(".")) {
                throw new IllegalStateException(
                    "You must use a fully qualified domain name for Jenkins URL, this is required by GitLab"
                        + "\nURL is:" + url);
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad Jenkins URL\nURL is:" + url);
        }
    }


    public static ProjectHook createWebHook() {
        ProjectHook enabledHooks = new ProjectHook();
        enabledHooks.setPushEvents(true);
        enabledHooks.setMergeRequestsEvents(true);
        enabledHooks.setTagPushEvents(true);
        enabledHooks.setNoteEvents(true);
        enabledHooks.setEnableSslVerification(false);
        // TODO add secret token, add more events give option for sslVerification
        return enabledHooks;
    }

    public static String createWebHookWhenMissing(GitLabApi gitLabApi, String project, String hookUrl)
            throws GitLabApiException {
        ProjectHook projectHook = gitLabApi.getProjectApi().getHooksStream(project)
                .filter(hook -> hookUrl.equals(hook.getUrl()))
                .findFirst()
                .orElseGet(GitLabHookCreator::createWebHook);
        if(projectHook.getId() == null) {
            gitLabApi.getProjectApi().addHook(project, hookUrl, projectHook, false, "");
            return "created";
        }
        return "already created";
    }
}
