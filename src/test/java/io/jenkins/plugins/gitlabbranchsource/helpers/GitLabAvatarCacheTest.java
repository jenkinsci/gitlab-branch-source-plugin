package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatarCache.GitLabAvatarLocation;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import org.junit.Test;

public class GitLabAvatarCacheTest {

    private static final GitLabServer GITLAB_COM = new GitLabServer("https://gitlab.com", "gitlab.com", "");

    @Test
    public void matches_project_avatar() {
        GitLabAvatarLocation location = GitLabAvatarCache.matchAvatar(
                GITLAB_COM, "https://gitlab.com/uploads/-/system/project/avatar/42/x.png");

        assertThat(location.type(), is("project"));
        assertThat(location.id(), is(42L));
    }

    @Test
    public void matches_group_avatar() {
        GitLabAvatarLocation location =
                GitLabAvatarCache.matchAvatar(GITLAB_COM, "https://gitlab.com/uploads/-/system/group/avatar/7/x.png");

        assertThat(location.type(), is("group"));
        assertThat(location.id(), is(7L));
    }

    @Test
    public void tolerates_a_trailing_slash_on_the_server_url() {
        GitLabServer server = new GitLabServer("https://gitlab.com/", "gitlab.com", "");

        assertThat(
                GitLabAvatarCache.matchAvatar(server, "https://gitlab.com/uploads/-/system/project/avatar/1/x.png"),
                is(notNullValue()));
    }

    @Test
    public void matches_a_server_hosted_under_a_sub_path() {
        GitLabServer server = new GitLabServer("https://example.com/gitlab", "self-hosted", "");

        GitLabAvatarLocation location = GitLabAvatarCache.matchAvatar(
                server, "https://example.com/gitlab/uploads/-/system/group/avatar/9/x.png");

        assertThat(location.id(), is(9L));
    }

    @Test
    public void ignores_user_avatars() {
        // user avatars are public, so they keep using the plain URL fetch
        assertThat(
                GitLabAvatarCache.matchAvatar(GITLAB_COM, "https://gitlab.com/uploads/-/system/user/avatar/5/x.png"),
                is(nullValue()));
    }

    @Test
    public void ignores_urls_from_other_hosts() {
        assertThat(GitLabAvatarCache.matchAvatar(GITLAB_COM, "https://gravatar.com/avatar/abc.png"), is(nullValue()));
        assertThat(
                GitLabAvatarCache.matchAvatar(
                        GITLAB_COM, "https://evil.example.com/uploads/-/system/project/avatar/42/x.png"),
                is(nullValue()));
    }

    @Test
    public void ignores_an_id_too_large_to_be_real() {
        assertThat(
                GitLabAvatarCache.matchAvatar(
                        GITLAB_COM, "https://gitlab.com/uploads/-/system/project/avatar/99999999999999999999/x.png"),
                is(nullValue()));
    }
}
