package io.jenkins.plugins.gitlabbranchsource.authentication;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.util.Secret;
import io.jenkins.plugins.gitlabbranchsource.client.api.GitLabAuth;
import io.jenkins.plugins.gitlabbranchsource.client.api.GitLabAuthToken;
import io.jenkins.plugins.gitlabbranchsource.client.api.GitLabAuthUser;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import io.jenkins.plugins.gitlabbranchsource.credentials.PersonalAccessToken;

import static org.junit.Assert.assertThat;

public class GitLabAuthSourceTest {
    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void given__tokenCredential__when__convert__then__tokenAuth() throws Exception {
        // we use a mock to ensure that java.lang.reflect.Proxy implementations of the credential interface work
        PersonalAccessToken personalAccessToken = Mockito.mock(PersonalAccessToken.class);
        Mockito.when(personalAccessToken.getToken()).thenReturn(Secret.fromString("sAfbXasnou47yxoAsCax"));
        GitLabAuth gitLabAuth = AuthenticationTokens.convert(GitLabAuth.class, personalAccessToken);

        assertThat(gitLabAuth, instanceOf(GitLabAuthToken.class));
        assertThat(((GitLabAuthToken)gitLabAuth).getToken(), is("sAfbXasnou47yxoAsCax"));
    }

    @Test
    public void given__userPassCredential__when__convert__then__tokenAuth() throws Exception {
        // we use a mock to ensure that java.lang.reflect.Proxy implementations of the credential interface work
        UsernamePasswordCredentials usernamePasswordCredential = Mockito.mock(UsernamePasswordCredentials.class);
        Mockito.when(usernamePasswordCredential.getUsername()).thenReturn("alice");
        Mockito.when(usernamePasswordCredential.getPassword()).thenReturn(Secret.fromString("ilovejenkins"));
        GitLabAuth gitLabAuth = AuthenticationTokens.convert(GitLabAuth.class, usernamePasswordCredential);
        assertThat(gitLabAuth, instanceOf(GitLabAuthUser.class));
        assertThat(((GitLabAuthUser)gitLabAuth).getUsername(), is("alice"));
        assertThat(((GitLabAuthUser)gitLabAuth).getPassword(), is("ilovejenkins"));
    }

}
