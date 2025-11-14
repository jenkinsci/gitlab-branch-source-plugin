package io.jenkins.plugins.gitlabserverconfig.credentials;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.Secret;

public interface GroupAccessToken extends StandardUsernamePasswordCredentials {

    /**
     * Returns the token.
     *
     * @return the token.
     */
    @NonNull
    Secret getToken();
}
