package io.jenkins.plugins.gitlabbranchsource.servers;

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
import io.jenkins.plugins.gitlabbranchsource.client.api.GitLabAuth;
import io.jenkins.plugins.gitlabbranchsource.client.api.GitLabAuthToken;
import io.jenkins.plugins.gitlabbranchsource.credentials.PersonalAccessTokenImpl;
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
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

/**
 * Represents a GitLab Server instance.
 */

public class GitLabServer extends AbstractDescribableImpl<GitLabServer> {

    private static final Logger LOGGER = Logger.getLogger(GitLabServer.class.getName());
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


    public static final String GITLAB_SERVER_URL = "https://gitlab.com";

    /**
     * Used as default token value if no any credentials found by given credentialsId.*/
    public static final String UNKNOWN_TOKEN = "UNKNOWN_TOKEN";

    /**
     * Optional name to use to describe the end-point.
     */
    @CheckForNull
    private final String name;

    /**
     * The URL of this GitLab Server.
     */
    @NonNull
    private String serverUrl;

    /**
     * {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     */
    private boolean manageHooks;

    /**
     * The {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for auto-management of hooks.
     */
    @CheckForNull
    private final String credentialsId;


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
     * Looks up the {@link StandardCredentials} to use for auto-management of hooks.
     *
     * @return the credentials or {@code null}.
     */
    @CheckForNull
    public StandardCredentials credentials() {
        return StringUtils.isBlank(credentialsId) ? null : CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardCredentials.class,
                        Jenkins.getActiveInstance(),
                        ACL.SYSTEM,
                        fromUri(serverUrl).build()
                ),
                CredentialsMatchers.allOf(
                        AuthenticationTokens.matcher(GitLabAuth.class),
                        withId(credentialsId)
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

        @NonNull
        @Override
        public String getDisplayName() {
            return "GitLab Server";
        }


        public String getAdvanceConfigMessage() {
            return Messages.GitLabServer_advancedSectionForFuture();
        }

        /**
         * Checks that the supplied URL is valid.
         *
         * @param value the URL to check.
         * @return the validation results.
         */
        public static FormValidation doCheckServerUrl(@QueryParameter String value) {
            // TODO: Add support for GitLab Ultimate (self hosted) and Gold (saas)
            Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error("Malformed GitLab url (%s)", e.getMessage());
            }

            if(GITLAB_SERVER_URL.equals(value)) {
                return FormValidation.ok();
            }
            return FormValidation.warning(Messages.GitLabServer_recheckUrl());
        }

        @RequirePOST
        @Restricted(DoNotUse.class)
        @SuppressWarnings("unused")
        public FormValidation doTestConnection(@QueryParameter String serverUrl,
                                               @QueryParameter String credentialsId) {
            String privateToken = UNKNOWN_TOKEN;
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                    lookupCredentials(
                            StandardCredentials.class,
                            Jenkins.getActiveInstance(),
                            ACL.SYSTEM,
                            fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL)).build()
                    ),
                    CredentialsMatchers.allOf(
                            AuthenticationTokens.matcher(GitLabAuth.class),
                            withId(credentialsId)
                    )
            );
            if(credentials == null) {
                return FormValidation.errorWithMarkup(Messages.GitLabServer_credentialsNotResolved(Util.escape(credentialsId)));
            }
            try {
                GitLabAuth gitLabAuth = AuthenticationTokens.convert(GitLabAuth.class, credentials);
                if(isToken(gitLabAuth)) {
                    privateToken = ((GitLabAuthToken) gitLabAuth).getToken();
                }
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
         * @param serverUrl the server URL.
         * @param credentialsId the credentials Id
         * @return the available credentials.
         */
        @Restricted(NoExternalUse.class) // stapler
        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            if(!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return  new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(ACL.SYSTEM,
                        Jenkins.getInstance(),
                        StandardCredentials.class,
                        fromUri(serverUrl).build(),
                        credentials -> credentials instanceof PersonalAccessTokenImpl
                );
        }
    }

    /**
     *  Helper function
     *
     * @param gitLabAuth a generic auth object
     * @return true if gitLabAuth is an object of GitLabAuthToken
     */
    private static boolean isToken(GitLabAuth gitLabAuth) {
        return gitLabAuth instanceof GitLabAuthToken;
    }
}
