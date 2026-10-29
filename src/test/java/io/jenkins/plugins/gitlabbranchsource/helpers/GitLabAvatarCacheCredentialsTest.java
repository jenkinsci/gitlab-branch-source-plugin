package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatarCache.GitLabAvatarLocation;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessTokenImpl;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jenkins.model.Jenkins;
import org.gitlab4j.api.GitLabApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitLabAvatarCacheCredentialsTest {

    private GitLabServer server;
    private JenkinsRule r;

    @BeforeEach
    void setUp(JenkinsRule r) {
        this.r = r;
        server = new GitLabServer("https://gitlab.example.com", "example", "gitlab-token");
        server.setHooksRootUrl("https://jenkins.example.com");
        GitLabServers.get().addServer(server);
    }

    /**
     * Avatars are fetched on a background thread with no authenticated user. Resolving the server
     * credentials from there used to blow up with an AccessDeniedException on a secured instance
     * (#690); the fetch now impersonates SYSTEM. This reproduces the failing setup and checks the
     * token still resolves.
     */
    @Test
    void resolves_server_credentials_from_an_unauthenticated_thread() throws Exception {
        PersonalAccessTokenImpl credential =
                new PersonalAccessTokenImpl(CredentialsScope.GLOBAL, "gitlab-token", "avatar test");
        credential.setToken("s3cr3t");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), credential);

        GitLabAvatarLocation location = GitLabAvatarCache.matchAvatar(
                server, "https://gitlab.example.com/uploads/-/system/group/avatar/7/x.png");

        // lock the instance down so anonymous - as the background threads run - has no permissions
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy());

        try (ACLContext ignored = ACL.as2(Jenkins.ANONYMOUS2)) {
            // resolving the credentials directly as the anonymous user is what failed before
            assertThrows(Exception.class, () -> server.getCredentials(Jenkins.get()));

            // the avatar fetch resolves them anyway by impersonating SYSTEM
            try (GitLabApi api = GitLabAvatarCache.openApi(location)) {
                assertThat(api.getAuthToken(), is("s3cr3t"));
            }
        }
    }
}
