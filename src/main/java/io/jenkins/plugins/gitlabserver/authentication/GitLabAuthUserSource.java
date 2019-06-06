package io.jenkins.plugins.gitlabserver.authentication;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.gitlabserver.client.api.GitLabAuthUser;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

/**
 * Converts {@link UsernamePasswordCredentials} to {@link GitLabAuthUser} authentication.
 */
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
    @NonNull
    @Override
    public GitLabAuthUser convert(@NonNull UsernamePasswordCredentials credential) throws AuthenticationTokenException {
        return new GitLabAuthUser(credential.getUsername(), credential.getPassword().getPlainText());
    }

}
