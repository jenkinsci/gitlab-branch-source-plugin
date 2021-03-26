package io.jenkins.plugins.gitlabserverconfig.servers;

import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;

public class GitLabServerTest {

  @ClassRule
  public static JenkinsRule j = new JenkinsRule();

  @Test
  public void testFixEmptyAndTrimOne() throws Exception {
    GitLabServer server = new GitLabServer("https://gitlab.com", "default", null);
    server.setHooksRootUrl("https://myhooks/");
    assertThat(server.getName(),is("default"));
    assertThat(server.getServerUrl(),is("https://gitlab.com"));
    assertThat(server.getCredentialsId(),nullValue());
    assertThat(server.getHooksRootUrl(),is("https://myhooks/"));
  }

  @Test
  public void testFixEmptyAndTrimTwo() throws Exception {
    GitLabServer server = new GitLabServer("     https://gitlab.com    ", "     default      ", null);
    server.setHooksRootUrl("       https://myhooks/        ");
    assertThat(server.getName(),is("default"));
    assertThat(server.getServerUrl(),is("https://gitlab.com"));
    assertThat(server.getCredentialsId(),nullValue());
    assertThat(server.getHooksRootUrl(),is("https://myhooks/"));
  }

  @Test
  public void testFixEmptyAndTrimThree() throws Exception {
    GitLabServer server = new GitLabServer(null, null, null);
    server.setHooksRootUrl(null);
    assertThat(server.getName(),startsWith("gitlab-"));
    assertThat(server.getServerUrl(),is("https://gitlab.com"));
    assertThat(server.getCredentialsId(),nullValue());
    assertThat(server.getHooksRootUrl(),nullValue());
  }

  @Test
  public void testFixEmptyAndTrimFour() throws Exception {
    GitLabServer server = new GitLabServer("https://whatever.com", "whatever", null);
    server.setHooksRootUrl("https://myhooks/");
    assertThat(server.getName(),is("whatever"));
    assertThat(server.getServerUrl(),is("https://whatever.com"));
    assertThat(server.getCredentialsId(),nullValue());
    assertThat(server.getHooksRootUrl(),is("https://myhooks/"));
  }

  @Test
  public void testFixEmptyAndTrimFive() throws Exception {
    GitLabServer server = new GitLabServer("","","");
    server.setHooksRootUrl("");
    assertThat(server.getName(),startsWith("gitlab-"));
    assertThat(server.getServerUrl(),is("https://gitlab.com"));
    assertThat(server.getCredentialsId(),is(""));
    assertThat(server.getHooksRootUrl(),nullValue());
  }
}
