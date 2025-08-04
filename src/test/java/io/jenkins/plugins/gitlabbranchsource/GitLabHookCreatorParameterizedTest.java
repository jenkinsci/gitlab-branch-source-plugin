package io.jenkins.plugins.gitlabbranchsource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import java.util.Arrays;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitLabHookCreatorParameterizedTest {

    private static JenkinsRule r;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        r = rule;
    }

    static Object[][] data() {
        return new Object[][] {
            {"intranet.local:8080", false, "/gitlab-systemhook/post"},
            {"intranet.local", true, "/gitlab-webhook/post"},
            {"www.mydomain.com:8000", true, "/gitlab-webhook/post"},
            {"www.mydomain.com", false, "/gitlab-systemhook/post"},
            {"www.mydomain.com/jenkins", true, "/gitlab-webhook/post"}
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void hookUrl(String jenkinsUrl, boolean hookType, String expectedPath) {
        Arrays.asList("http://", "https://").forEach(proto -> {
            String expected = proto + jenkinsUrl + expectedPath;
            JenkinsLocationConfiguration.get().setUrl(proto + jenkinsUrl);
            String hookUrl = GitLabHookCreator.getHookUrl(null, hookType);
            GitLabHookCreator.checkURL(hookUrl);
            assertThat(hookUrl.replaceAll(proto, ""), not(containsString("//")));
            assertThat(hookUrl, is(expected));
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    void hookUrlFromCustomRootUrl(String jenkinsUrl, boolean hookType, String expectedPath) {
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
