package io.jenkins.plugins.gitlabbranchsource.authentication;

import io.jenkins.plugins.gitlabbranchsource.api.client.GitLabAuthToken;
import io.jenkins.plugins.gitlabbranchsource.credentials.PersonalAccessToken;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

import javax.annotation.Nonnull;

public class GitLabAuthTokenSource extends AuthenticationTokenSource<GitLabAuthToken, PersonalAccessToken> {
    /**
     * Constructor.
     */
    public GitLabAuthTokenSource() {
        super(GitLabAuthToken.class, PersonalAccessToken.class);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public GitLabAuthToken convert(@Nonnull PersonalAccessToken credential) throws AuthenticationTokenException {
        return new GitLabAuthToken(credential.getToken().getPlainText());
    }
}
