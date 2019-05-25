package io.jenkins.plugins.gitlabbranchsource.authentication;

import io.jenkins.plugins.gitlabbranchsource.credentials.PersonalAccessToken;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;
import org.gitlab4j.api.models.ImpersonationToken;

import javax.annotation.Nonnull;

public class GitLabAuthTokenSource extends AuthenticationTokenSource<ImpersonationToken, PersonalAccessToken> {
    /**
     * Constructor.
     */
    public GitLabAuthTokenSource() {
        super(ImpersonationToken.class, PersonalAccessToken.class);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public ImpersonationToken convert(@Nonnull PersonalAccessToken credential) throws AuthenticationTokenException {
        ImpersonationToken impersonationToken = new ImpersonationToken();
        impersonationToken.setToken(credential.getToken().getPlainText());
        return impersonationToken;
    }
}
