package io.jenkins.plugins.gitlabserverconfig.servers;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.xml.sax.SAXException;

@WithJenkins
class GitLabServerTest {

    private static JenkinsRule j;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testFixEmptyAndTrimOne() throws Exception {
        GitLabServer server = new GitLabServer("https://gitlab.com", "default", null);
        server.setHooksRootUrl("https://myhooks/");
        assertThat(server.getName(), is("default"));
        assertThat(server.getServerUrl(), is("https://gitlab.com"));
        assertThat(server.getCredentialsId(), nullValue());
        assertThat(server.getHooksRootUrl(), is("https://myhooks/"));
    }

    @Test
    void testFixEmptyAndTrimTwo() throws Exception {
        GitLabServer server = new GitLabServer("     https://gitlab.com    ", "     default      ", null);
        server.setHooksRootUrl("       https://myhooks/        ");
        assertThat(server.getName(), is("default"));
        assertThat(server.getServerUrl(), is("https://gitlab.com"));
        assertThat(server.getCredentialsId(), nullValue());
        assertThat(server.getHooksRootUrl(), is("https://myhooks/"));
    }

    @Test
    void testFixEmptyAndTrimThree() throws Exception {
        GitLabServer server = new GitLabServer(null, null, null);
        server.setHooksRootUrl(null);
        assertThat(server.getName(), startsWith("gitlab-"));
        assertThat(server.getServerUrl(), is("https://gitlab.com"));
        assertThat(server.getCredentialsId(), nullValue());
        assertThat(server.getHooksRootUrl(), nullValue());
    }

    @Test
    void testFixEmptyAndTrimFour() throws Exception {
        GitLabServer server = new GitLabServer("https://whatever.com", "whatever", null);
        server.setHooksRootUrl("https://myhooks/");
        assertThat(server.getName(), is("whatever"));
        assertThat(server.getServerUrl(), is("https://whatever.com"));
        assertThat(server.getCredentialsId(), nullValue());
        assertThat(server.getHooksRootUrl(), is("https://myhooks/"));
    }

    @Test
    void testFixEmptyAndTrimFive() throws Exception {
        GitLabServer server = new GitLabServer("", "", "");
        server.setHooksRootUrl("");
        assertThat(server.getName(), startsWith("gitlab-"));
        assertThat(server.getServerUrl(), is("https://gitlab.com"));
        assertThat(server.getCredentialsId(), is(""));
        assertThat(server.getHooksRootUrl(), nullValue());
    }

    @Test
    @Issue("SECURITY-3251")
    void testGetDoCheckServerUrl() throws IOException, SAXException {
        try (WebClient wc = j.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            HtmlPage page = wc.goTo(
                    "descriptorByName/io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer/checkServerUrl?serverUrl=http://attacker.example.com");
            assertEquals(
                    404, page.getWebResponse().getStatusCode()); // Should be 405 but Stapler doesn't work that way.
        }
    }
}
