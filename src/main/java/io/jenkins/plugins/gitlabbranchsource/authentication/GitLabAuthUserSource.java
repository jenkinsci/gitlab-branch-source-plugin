package io.jenkins.plugins.gitlabbranchsource.authentication;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.Extension;
import io.jenkins.plugins.gitlabbranchsource.api.client.GitLabAuthUser;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

import javax.annotation.Nonnull;

@Extension
public class GitLabAuthUserSource extends AuthenticationTokenSource<GitLabAuthUser, UsernamePasswordCredentials> {

    /**
     * Constructor.
     */
    public GitLabAuthUserSource() {
        super(GitLabAuthUser.class, UsernamePasswordCredentials.class);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public GitLabAuthUser convert(@Nonnull UsernamePasswordCredentials credential) throws AuthenticationTokenException {
        return new GitLabAuthUser(credential.getUsername(), credential.getPassword().getPlainText());
    }

}
