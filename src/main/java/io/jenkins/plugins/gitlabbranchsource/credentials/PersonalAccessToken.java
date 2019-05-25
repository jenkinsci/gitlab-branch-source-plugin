package io.jenkins.plugins.gitlabbranchsource.credentials;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;
import javax.annotation.Nonnull;

public interface PersonalAccessToken extends StandardCredentials {

    /**
     * Returns the token.
     * @return the token.
     */

    @Nonnull
    Secret getToken();

}
