package io.jenkins.plugins.gitlabserverconfig.casc;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.security.ACL;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessTokenImpl;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.util.Collections;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

public class ConfigurationAsCodeTest {

    @ClassRule
    @ConfiguredWithCode("configuration-as-code.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void should_support_configuration_as_code() {
        List<GitLabServer> servers = GitLabServers.get().getServers();
        assertThat(servers.size(), is(1));
        GitLabServer server = servers.get(0);
        assertThat(server.getServerUrl(), is("https://gitlab.com"));
        assertThat(server.getName(), matchesPattern("gitlab-[0-9]{4}"));
        assertThat(server.isManageWebHooks(), is(true));
        assertThat(server.isManageSystemHooks(), is(true));
        assertThat(server.getHooksRootUrl(), is("https://jenkins.intranet/"));

        List<PersonalAccessTokenImpl> credentials = CredentialsProvider.lookupCredentials(
            PersonalAccessTokenImpl.class, j.jenkins, ACL.SYSTEM,
            Collections.emptyList()
        );
        assertThat(credentials, hasSize(1));
        final PersonalAccessTokenImpl credential = credentials.get(0);
        assertThat(credential.getToken().getPlainText(), is("glpat-XfsqZvVtAx5YCph5bq3r"));
        assertThat(credential.getToken().getEncryptedValue(), is(not("glpat-XfsqZvVtAx5YCph5bq3r")));
    }

    @Test
    public void should_support_configuration_export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode gitLabServers = getUnclassifiedRoot(context).get("gitLabServers");

        String exported = toYamlString(gitLabServers);

        String expected = toStringFromYamlFile(this, "expected_output.yml");

        assertThat(exported, matchesPattern(expected));
    }
}
