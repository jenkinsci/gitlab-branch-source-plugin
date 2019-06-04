package io.jenkins.plugins.gitlabbranchsource.credentials;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.Secret;

public interface PersonalAccessToken extends StandardCredentials {

    /**
     * Returns the token.
     *
     * @return the token.
     */
    @NonNull
    Secret getToken();
}
