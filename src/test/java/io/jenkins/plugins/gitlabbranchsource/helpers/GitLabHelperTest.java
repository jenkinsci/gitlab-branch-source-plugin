package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import org.junit.jupiter.api.Test;

class GitLabHelperTest {

    @Test
    void server_url_does_not_have_trailing_slash() {
        assertThat(GitLabHelper.getServerUrl(null), is("https://gitlab.com"));

        GitLabServer server1 = new GitLabServer("https://company.com/gitlab/", "comp_server", "1245");
        assertThat(GitLabHelper.getServerUrl(server1), is("https://company.com/gitlab"));

        GitLabServer server2 = new GitLabServer("https://gitlab.example.org", "", "pw-id");
        assertThat(GitLabHelper.getServerUrl(server2), is("https://gitlab.example.org"));
    }
}
