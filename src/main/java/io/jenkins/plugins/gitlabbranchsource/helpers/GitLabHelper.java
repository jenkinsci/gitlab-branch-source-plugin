package io.jenkins.plugins.gitlabbranchsource.helpers;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import com.damnhandy.uri.template.impl.Operator;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.security.ACL;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import org.eclipse.jgit.annotations.NonNull;
import org.gitlab4j.api.GitLabApi;

public class GitLabHelper {

    public static GitLabApi apiBuilder(String serverName) {
        return apiBuilder(serverName, null);
    }

    public static GitLabApi apiBuilder(String serverName, StandardCredentials credentials) {
        GitLabServer server = GitLabServers.get().findServer(serverName);
        if (server != null) {
            credentials = credentials == null ?server.getCredentials(): credentials;
            GitLabApi gitLabApi = new GitLabApi(server.getServerUrl(), GitLabServer.EMPTY_TOKEN);

            if (credentials == null) {
                gitLabApi = new GitLabApi(server.getServerUrl(), GitLabServer.EMPTY_TOKEN);

            } else if (credentials instanceof PersonalAccessToken) {
                gitLabApi = new GitLabApi(server.getServerUrl(),((PersonalAccessToken) credentials).getToken().getPlainText());

            } else if (credentials instanceof UsernamePasswordCredentials) {
                gitLabApi = new GitLabApi(server.getServerUrl(),((UsernamePasswordCredentials) credentials).getPassword().getPlainText());
            }
            gitLabApi.setRequestTimeout(10000, 15000);
            return gitLabApi;
        }
        throw new IllegalStateException(
            String.format("No server found with the name: %s", serverName));
    }

    public static StandardCredentials getSourceCredentials(@NonNull String server, @NonNull Item context, @NonNull String credentialsId) {
        return CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(
                StandardCredentials.class,
                context,
                context instanceof Queue.Task
                        ? ((Queue.Task) context).getDefaultAuthentication()
                        : ACL.SYSTEM,
                URIRequirementBuilder.create()
                        .withHostname(getServerUrl(server))
                        .build()
        ),
        CredentialsMatchers.allOf(
                CredentialsMatchers.withId(credentialsId),
                CredentialsMatchers.instanceOf(StandardCredentials.class)
        ));
    }

    @NonNull
    public static String getServerUrlFromName(String serverName) {
        GitLabServer server = GitLabServers.get().findServer(serverName);
        return getServerUrl(server);
    }

    @NonNull
    public static String getServerUrl(GitLabServer server) {
        return server != null ? server.getServerUrl() : GitLabServer.GITLAB_SERVER_URL;
    }

    @NonNull
    private static String getServerUrl(String server) {
        if (server.startsWith("http://") || server.startsWith("https://")) {
            return server;
        } else {
            return getServerUrlFromName(server);
        }
    }

    public static UriTemplateBuilder getUriTemplateFromServer(String server) {
        return UriTemplate.buildFromTemplate(getServerUrl(server));
    }

    public static UriTemplate projectUriTemplate(String serverNameOrUrl) {
        return getUriTemplateFromServer(serverNameOrUrl)
            .template("{/project*}").build();
    }

    public static UriTemplate branchUriTemplate(String serverNameOrUrl) {
        return getUriTemplateFromServer(serverNameOrUrl)
            .template("{/project*}/tree/{branch*}").build();
    }

    public static UriTemplate mergeRequestUriTemplate(String serverNameOrUrl) {
        return getUriTemplateFromServer(serverNameOrUrl)
            .template("{/project*}/merge_requests/{iid}").build();
    }

    public static UriTemplate tagUriTemplate(String serverNameOrUrl) {
        return getUriTemplateFromServer(serverNameOrUrl)
            .template("{/project*}/tree/{tag*}").build();
    }

    public static UriTemplate commitUriTemplate(String serverNameOrUrl) {
        return getUriTemplateFromServer(serverNameOrUrl)
            .template("{/project*}/commit/{hash}")
            .build();
    }

    public static String[] splitPath(String path) {
        return path.split(Operator.PATH.getSeparator());
    }

}
