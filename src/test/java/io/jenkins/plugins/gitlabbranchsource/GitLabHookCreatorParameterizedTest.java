package io.jenkins.plugins.gitlabbranchsource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import java.util.Arrays;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.jvnet.hudson.test.JenkinsRule;

@RunWith(Parameterized.class)
public class GitLabHookCreatorParameterizedTest {

    @ClassRule
    public static JenkinsRule r = new JenkinsRule();

    private final String jenkinsUrl;
    private final boolean hookType;
    private final String expectedPath;

    @Parameters(name = "check {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"intranet.local:8080", false, "/gitlab-systemhook/post"},
            {"intranet.local", true, "/gitlab-webhook/post"},
            {"www.mydomain.com:8000", true, "/gitlab-webhook/post"},
            {"www.mydomain.com", false, "/gitlab-systemhook/post"},
            {"www.mydomain.com/jenkins", true, "/gitlab-webhook/post"}
        });
    }

    public GitLabHookCreatorParameterizedTest(String jenkinsUrl, boolean hookType, String expectedPath) {
        this.jenkinsUrl = jenkinsUrl;
        this.hookType = hookType;
        this.expectedPath = expectedPath;
    }

    @Test
    public void hookUrl() {
        Arrays.asList("http://", "https://").forEach(proto -> {
            String expected = proto + jenkinsUrl + expectedPath;
            JenkinsLocationConfiguration.get().setUrl(proto + jenkinsUrl);
            String hookUrl = GitLabHookCreator.getHookUrl(null, hookType);
            GitLabHookCreator.checkURL(hookUrl);
            assertThat(hookUrl.replaceAll(proto, ""), not(containsString("//")));
            assertThat(hookUrl, is(expected));
        });
    }

    @Test
    public void hookUrlFromCustomRootUrl() {
        Arrays.asList("http://", "https://").forEach(proto -> {
            String expected = proto + jenkinsUrl + expectedPath;
            JenkinsLocationConfiguration.get().setUrl("http://whatever");
            GitLabServer server = new GitLabServer("https://gitlab.com", "GitLab", null);
            server.setHooksRootUrl(proto + jenkinsUrl);
            String hookUrl = GitLabHookCreator.getHookUrl(server, hookType);
            GitLabHookCreator.checkURL(hookUrl);
            assertThat(hookUrl.replaceAll(proto, ""), not(containsString("//")));
            assertThat(hookUrl, is(expected));
        });
    }
}
