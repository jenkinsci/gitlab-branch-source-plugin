package io.jenkins.plugins.gitlabserverconfig.servers;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import java.util.List;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

public class GitLabServersTest {
    @Rule
    public LoggerRule logger = new LoggerRule().record(OldDataMonitor.class, Level.FINE).capture(50);;
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @LocalData
    @Test
    public void migrationToCredentials() throws Throwable {
        // LocalData creating using the following:
        /*
        GitLabServer server = new GitLabServer("http://localhost", "my-server", null);
        server.setSecretToken(Secret.fromString("s3cr3t!"));
        GitLabServers.get().addServer(server);
        */
        var server = GitLabServers.get().getServers().stream().filter(s -> s.getName().equals("my-server")).findFirst().orElseThrow();
        var credentialsId = server.getWebhookSecretCredentialsId();
        var credentials = CredentialsMatchers.filter(
                CredentialsProvider.lookupCredentialsInItemGroup(StringCredentials.class, Jenkins.get(), ACL.SYSTEM2),
                CredentialsMatchers.withId(credentialsId));
        assertThat(credentials, hasSize(1));
        assertThat(credentials.get(0).getSecret().getPlainText(), equalTo("s3cr3t!"));
        assertThat(logger.getMessages(), not(hasItem(containsString("Trouble loading " + GitLabServers.class.getName()))));
    }

    @TestExtension("migrationToCredentials")
    public static class CredentialsProviderThatRequiresDescriptorLookup extends CredentialsProvider {
        @Override
        public <C extends Credentials> List<C> getCredentials(
                @NonNull Class<C> type,
                @Nullable ItemGroup itemGroup,
                @Nullable Authentication authentication,
                @NonNull List<DomainRequirement> domainRequirements) {
            // Prior to fix, this caused the GitLabServer migration code to recurse infinitely, causing problems when starting Jenkins.
            // In practice this was caused by a lookup of another descriptor type, but I am using GitLabServers for clarity.
            Jenkins.get().getDescriptor(GitLabServers.class);
            return List.of();
        }
    }
}
