package io.jenkins.plugins.gitlabserverconfig.credentials;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;


/**
 * Default implementation of {@link PersonalAccessToken} for use by {@link Jenkins} {@link
 * CredentialsProvider} instances that store {@link Secret} locally.
 */
public class PersonalAccessTokenImpl extends BaseStandardCredentials implements
    PersonalAccessToken {

    /**
     * Our token.
     */
    @NonNull
    private final Secret token;

    /**
     * Constructor.
     *
     * @param scope the credentials scope.
     * @param id the credentials id.
     * @param description the description of the token.
     * @param token the token itself (will be passed through {@link Secret#fromString(String)})
     */
    @DataBoundConstructor
    public PersonalAccessTokenImpl(
        @CheckForNull CredentialsScope scope,
        @CheckForNull String id,
        @CheckForNull String description,
        @NonNull String token) {
        super(scope, id, description);
        this.token = Secret.fromString(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Secret getToken() {
        return token;
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("gitlabPersonalAccessToken")
    public static class DescriptorImpl extends CredentialsDescriptor {

        private static final int GITLAB_ACCESS_TOKEN_MINIMAL_LENGTH = 20;

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.PersonalAccessTokenImpl_displayName();
        }

        /**
         * Sanity check for a Gitlab access token.
         *
         * @param value the personal access token.
         * @return the results of the sanity check.
         */
        @Restricted(NoExternalUse.class) // stapler
        @SuppressWarnings("unused")
        public FormValidation doCheckToken(@QueryParameter String value) {
            Secret secret = Secret.fromString(value);
            if (StringUtils.equals(value, secret.getPlainText())) {
                if (value.length() < GITLAB_ACCESS_TOKEN_MINIMAL_LENGTH) {
                    return FormValidation
                        .error(Messages.PersonalAccessTokenImpl_tokenWrongLength());
                }
            } else if (secret.getPlainText().length() < GITLAB_ACCESS_TOKEN_MINIMAL_LENGTH) {
                return FormValidation.error(Messages.PersonalAccessTokenImpl_tokenWrongLength());
            }
            return FormValidation.ok();
        }

    }

}
