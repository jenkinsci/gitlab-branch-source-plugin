package io.jenkins.plugins.gitlabserver.servers;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabserver.client.api.GitLabAuth;
import io.jenkins.plugins.gitlabserver.client.api.GitLabAuthToken;
import io.jenkins.plugins.gitlabserver.credentials.PersonalAccessTokenImpl;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMName;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.User;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.net.MalformedURLException;
import java.net.URL;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

/**
 * Represents a GitLab Server instance.
 */

public class GitLabServer extends AbstractDescribableImpl<GitLabServer> {

    public static final String GITLAB_SERVER_URL = "https://gitlab.com";
    /**
     * Used as default token value if no any credentials found by given credentialsId.
     */
    public static final String UNKNOWN_TOKEN = "UNKNOWN_TOKEN";
    /**
     * Common prefixes that we should remove when inferring a display name.
     */
    private static final String[] COMMON_PREFIX_HOSTNAMES = {
            "git.",
            "gitlab.",
            "vcs.",
            "scm.",
            "source."
    };
    /**
     * Optional name to use to describe the end-point.
     */
    @CheckForNull
    private final String name;

    /**
     * The URL of this GitLab Server.
     */
    @NonNull
    private final String serverUrl;
    /**
     * {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     */
    private final boolean manageHooks;
    /**
     * The {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for auto-management of hooks.
     */
    @CheckForNull
    private final String credentialsId;

    /**
     * Constructor
     *
     * @param displayName   Optional name to use to describe the end-point.
     * @param serverUrl     The URL of this GitLab Server
     * @param manageHooks   {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     * @param credentialsId The {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for
     *                      auto-management of hooks.
     * @since 1.0.5
     */
    @DataBoundConstructor
    public GitLabServer(@CheckForNull String displayName, @NonNull String serverUrl, boolean manageHooks,
                        @CheckForNull String credentialsId) {
        this.manageHooks = manageHooks;
        this.credentialsId = credentialsId;
        this.serverUrl = defaultIfBlank(serverUrl, GITLAB_SERVER_URL);
        this.name = StringUtils.isBlank(displayName)
                ? SCMName.fromUrl(this.serverUrl, COMMON_PREFIX_HOSTNAMES)
                : displayName;
    }

    /**
     * {@inheritDoc}
     */
    @CheckForNull
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Returns {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     *
     * @return {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     */
    public boolean isManageHooks() {
        return manageHooks;
    }

    /**
     * Returns the {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for auto-management
     * of hooks.
     *
     * @return the {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for auto-management
     * of hooks.
     */
    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Looks up the {@link StandardCredentials} to use for auto-management of hooks.
     *
     * @return the credentials or {@code null}.
     */
    @CheckForNull
    public StandardCredentials credentials() {
        return StringUtils.isBlank(credentialsId) ? null : CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        StandardCredentials.class,
                        Jenkins.getActiveInstance(),
                        ACL.SYSTEM,
                        fromUri(serverUrl).build()
                ),
                CredentialsMatchers.allOf(
                        AuthenticationTokens.matcher(GitLabAuth.class),
                        CredentialsMatchers.withId(credentialsId)
                )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Our descriptor.
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<GitLabServer> {

        /**
         * Checks that the supplied URL is valid.
         *
         * @param value the URL to check.
         * @return the validation results.
         */
        public static FormValidation doCheckServerUrl(@QueryParameter String value) {
            Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error("Malformed GitLab url (%s)", e.getMessage());
            }

            // TODO:[JENKINS-57747] Add support for GitLab Ultimate (self hosted) and Gold (saas)
            if (GITLAB_SERVER_URL.equals(value)) {
                return FormValidation.ok();
            }
            return FormValidation.warning(Messages.GitLabServer_recheckUrl());
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.GitLabServer_displayName();
        }

        @RequirePOST
        @Restricted(DoNotUse.class)
        @SuppressWarnings("unused")
        public FormValidation doTestConnection(@QueryParameter String serverUrl,
                                               @QueryParameter String credentialsId) {
            String privateToken = getToken(serverUrl, credentialsId);
            if (privateToken.equals(UNKNOWN_TOKEN)) {
                return FormValidation
                        .errorWithMarkup(Messages.GitLabServer_credentialsNotResolved(Util.escape(credentialsId)));
            }
            try {
                GitLabApi gitLabApi = new GitLabApi(serverUrl, privateToken);
                User user = gitLabApi.getUserApi().getCurrentUser();
                return FormValidation.ok(String.format("Credentials verified for user %s", user.getUsername()));
            } catch (GitLabApiException e) {
                return FormValidation.error(e, Messages.GitLabServer_failedValidation(Util.escape(e.getMessage())));
            }
        }

        /**
         * Stapler form completion.
         *
         * @param serverUrl     the server URL.
         * @param credentialsId the credentials Id
         * @return the available credentials.
         */
        @Restricted(NoExternalUse.class) // stapler
        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String name, @QueryParameter String serverUrl,
                                                     @QueryParameter String credentialsId) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(ACL.SYSTEM,
                            Jenkins.getInstance(),
                            StandardCredentials.class,
                            fromUri(serverUrl).build(),
                            credentials -> credentials instanceof PersonalAccessTokenImpl);
        }

        private static String getToken(String serverUrl, String credentialsId) {
            String privateToken = UNKNOWN_TOKEN;
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            StandardCredentials creds = CredentialsMatchers.firstOrNull(
                    lookupCredentials(
                            StandardCredentials.class,
                            Jenkins.getActiveInstance(),
                            ACL.SYSTEM,
                            fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL)).build()
                    ),
                    CredentialsMatchers.allOf(
                            AuthenticationTokens.matcher(GitLabAuth.class),
                            CredentialsMatchers.withId(credentialsId)
                    )
            );
            if (creds != null) {
                GitLabAuth gitLabAuth = AuthenticationTokens.convert(GitLabAuth.class, creds);
                if (gitLabAuth instanceof GitLabAuthToken) {
                    privateToken = ((GitLabAuthToken) gitLabAuth).getToken();
                }
            }
            return privateToken;
        }
    }
}
