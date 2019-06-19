package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.damnhandy.uri.template.UriTemplate;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jenkins.model.JenkinsLocationConfiguration;
import jenkins.scm.api.SCMNavigatorOwner;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.User;

import java.util.logging.Logger;

public class GitLabWebhookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabWebhookListener.class.getName());

    public static void register(SCMNavigatorOwner owner, GitLabSCMNavigator navigator,
                                WebhookRegistration mode, String credentialsId) {
        PersonalAccessToken credentials;
        String serverUrl = navigator.getServerUrl();
        switch (mode) {
            case DISABLE:
                return;
            case SYSTEM:
                GitLabServer server = GitLabServers.get().findServer(serverUrl);
                if (server == null || !server.isManageHooks()) {
                    return;
                }
                credentials = server.credentials();
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
            User user = gitLabApi.getUserApi().getCurrentUser();

        } catch (GitLabApiException e) {
            e.printStackTrace();
        }


    }
