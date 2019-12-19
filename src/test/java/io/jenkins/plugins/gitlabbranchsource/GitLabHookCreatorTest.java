package io.jenkins.plugins.gitlabbranchsource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.junit.Assert.assertEquals;

public class GitLabHookCreatorTest {

    class UrlCheckerMock extends UrlChecker {
        public UrlCheckerMock() {}
        @Override
        URL checkURL(String url) { return new URL(url); }
    }

    private void mockUrlChecker() throws Exception {
        Field urlChecker = UrlChecker.class.getDeclaredField("singleton");
        urlChecker.setAccessible(true);
        urlChecker.set(null, UrlCheckerMock.class.newInstance());
    }

    @Rule
    public final RestartableJenkinsRule plan = new RestartableJenkinsRule();

    @Test
    public void shouldCreateWebHook() {
        plan.then(j -> {
            GitLabHookCreatorTest.this.mockUrlChecker();

            URL rootUrl = j.getURL();
            assertEquals(
                rootUrl.getProtocol() + "://"+ rootUrl.getHost() +":"+ rootUrl.getPort() +"/jenkins/gitlab-webhook/post",
                GitLabHookCreator.getHookUrl(true)
            );
        });
    }

    @Test
    public void shouldCreateSystemHook() {
        plan.then(j -> {
            GitLabHookCreatorTest.this.mockUrlChecker();

            URL rootUrl = j.getURL();
            assertEquals(
                rootUrl.getProtocol() + "://"+ rootUrl.getHost() +":"+ rootUrl.getPort() +"/jenkins/gitlab-systemhook/post",
                GitLabHookCreator.getHookUrl(false)
            );
        });
    }

}
