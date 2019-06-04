package io.jenkins.plugins.gitlabserver.authentication;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.gitlabserver.client.api.GitLabAuthToken;
import io.jenkins.plugins.gitlabserver.credentials.PersonalAccessToken;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

/**
 * Converts {@link PersonalAccessToken} to {@link GitLabAuthToken} authentication.
 */
@Extension
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
    @NonNull
    @Override
    public GitLabAuthToken convert(@NonNull PersonalAccessToken credential) throws AuthenticationTokenException {
        return new GitLabAuthToken(credential.getToken().getPlainText());
    }
}
