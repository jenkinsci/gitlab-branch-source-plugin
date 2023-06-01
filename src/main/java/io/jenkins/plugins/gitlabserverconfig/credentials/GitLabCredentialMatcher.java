package io.jenkins.plugins.gitlabserverconfig.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

public class GitLabCredentialMatcher implements CredentialsMatcher {

    private static final long serialVersionUID = 1;

    @Override
    public boolean matches(@NonNull Credentials credentials) {
        try {
            return credentials instanceof PersonalAccessToken || credentials instanceof StringCredentials;
        } catch (Exception e) {
            return false;
        }
    }
}
