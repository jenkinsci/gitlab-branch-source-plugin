package io.jenkins.plugins.gitlabbranchsource;

import java.util.Arrays;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.Util;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class GitLabHookCreatorParameterizedTest {

    @ClassRule
    public static JenkinsRule r = new JenkinsRule();

    private final String jenkinsUrl;
    private final boolean hookType;
    private final String expectedPath;

    @Parameters(name = "check {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
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
        Arrays.asList("http://", "https://").forEach(
            proto -> {
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
        Arrays.asList("http://", "https://").forEach(
            proto -> {
                String expected = proto + jenkinsUrl + expectedPath;
                JenkinsLocationConfiguration.get().setUrl("http://whatever");
                // GitlabServer#getHooksRootUrl() ensures a trailing slash, we do the same here
                String hookUrl = GitLabHookCreator.getHookUrl(Util.ensureEndsWith(proto + jenkinsUrl, "/"), hookType);
                GitLabHookCreator.checkURL(hookUrl);
                assertThat(hookUrl.replaceAll(proto, ""), not(containsString("//")));
                assertThat(hookUrl, is(expected));
            });
    }
}
