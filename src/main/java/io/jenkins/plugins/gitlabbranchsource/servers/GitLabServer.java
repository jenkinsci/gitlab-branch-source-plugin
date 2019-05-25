package io.jenkins.plugins.gitlabbranchsource.servers;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.api.client.GitLabAuth;
import io.jenkins.plugins.gitlabbranchsource.api.client.GitLabAuthToken;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMName;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

/**
 * Represents a GitLab Server instance.
 */

public class GitLabServer extends AbstractDescribableImpl<GitLabServer> {

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
     * Used as default token value if no any creds found by given credsId.
     */
    private static final String UNKNOWN_TOKEN = "UNKNOWN_TOKEN";

    /**
     * Optional name to use to describe the end-point.
     */
    @CheckForNull
    private final String displayName;

    /**
     * The URL of this GitLab Server.
     */
    @Nonnull
    private String serverUrl = GITLAB_SERVER_URL;

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
     * The {@link #serverUrl} that Gitea thinks it is served at, if different from the URL that Jenkins needs to use to
     * access Gitea.
     *
     * @since 1.0.5
     */
    @CheckForNull
    private final String aliasUrl;

    /**
     * {@inheritDoc}
     */
    @CheckForNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
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
     * Returns the {@link #getServerUrl()} that the GitLab server believes it has when publishing webhook events or
     * {@code null} if this is a normal environment and GitLab is accessed through one true URL and has been configured
     * with that URL.
     * @return the {@link #getServerUrl()} that the GitLab server believes it has when publishing webhook events or
     * {@code null}
     */
    @CheckForNull
    public String getAliasUrl() {
        return aliasUrl;
    }

    /**
     * Constructor
     *
     * @param displayName   Optional name to use to describe the end-point.
     * @param serverUrl     The URL of this GitLab Server
     * @param manageHooks   {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     * @param credentialsId The {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for
     *                      auto-management of hooks.
     * @param aliasUrl      The URL this GitLab Server thinks it is at.
     * @since 1.0.5
     */
    @DataBoundConstructor
    public GitLabServer(@CheckForNull String displayName, @Nonnull String serverUrl, boolean manageHooks,
                       @CheckForNull String credentialsId, @CheckForNull String aliasUrl) {
        this.manageHooks = manageHooks && StringUtils.isNotBlank(credentialsId);
        this.credentialsId = manageHooks ? credentialsId : null;
        this.serverUrl = defaultIfBlank(serverUrl, GITLAB_SERVER_URL);
        this.displayName = StringUtils.isBlank(displayName)
                ? SCMName.fromUrl(this.serverUrl, COMMON_PREFIX_HOSTNAMES)
                : displayName;
        this.aliasUrl = StringUtils.trimToNull(GitLabServers.normalizeServerUrl(aliasUrl));
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
                        URIRequirementBuilder.fromUri(serverUrl).build()
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
    public static class DescriptorImpl extends Descriptor<GitLabServer> {

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.GitLabServer_displayName();
        }

        /**
         * Checks that the supplied URL is valid.
         *
         * @param value the URL to check.
         * @return the validation results.
         */
        public static FormValidation doCheckServerUrl(@QueryParameter String value) {
            // TODO: Add support for GitLab Ultimate (self hosted) and Gold (saas) with premium support
            Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error("Malformed GitLab url (%s)", e.getMessage());
            }

            if(GITLAB_SERVER_URL.equals(value)) {
                FormValidation.ok();
            }
            return FormValidation.warning("Only community version of GitLab is supported, GitLab Gold, Ultimate, " +
                    "Community self hosted etc are not supported, use only https://gitlab.com endpoint");
        }
    }

    /**
     * Checks that the supplied URL is valid.
     *
     * @param value the URL to check.
     * @return the validation results.
     */
    public static FormValidation doCheckAliasUrl(@QueryParameter String value) {
        Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
        if (StringUtils.isBlank(value)) return FormValidation.ok();
        try {
            new URI(value);
            return FormValidation.ok();
        } catch (URISyntaxException e) {
            return FormValidation.errorWithMarkup(Messages.GitLabServer_invalidUrl(Util.escape(e.getMessage())));
        }
    }


    /**
     * Stapler form completion.
     *
     * @param serverUrl the server URL.
     * @return the available credentials.
     */
    @Restricted(NoExternalUse.class) // stapler
    @SuppressWarnings("unused")
    public ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl) {
        Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
        StandardListBoxModel result = new StandardListBoxModel();
        serverUrl = GitLabServers.normalizeServerUrl(serverUrl);
        result.includeMatchingAs(
                ACL.SYSTEM,
                Jenkins.getActiveInstance(),
                StandardCredentials.class,
                URIRequirementBuilder.fromUri(serverUrl).build(),
                AuthenticationTokens.matcher(GitLabAuth.class)
        );
        return result;
    }

    public FormValidation doCheckCredentialsId(@QueryParameter String serverUrl, @QueryParameter String credentialsId) {
        Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
        serverUrl = GitLabServers.normalizeServerUrl(serverUrl);
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        StandardCredentials.class,
                        Jenkins.getActiveInstance(),
                        ACL.SYSTEM,
                        URIRequirementBuilder.fromUri(serverUrl).build()
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
            // TODO: resolve the null pointer exception raised by getToken() method
            GitLabAuth gitLabAuth = AuthenticationTokens.convert(GitLabAuth.class, credentials);
            String privateToken = "unknown";
            if(gitLabAuth instanceof GitLabAuthToken) {
                privateToken = ((GitLabAuthToken) gitLabAuth).getToken();
            }
            GitLabApi gitLabApi = new GitLabApi(serverUrl, privateToken);
            gitLabApi.getUserApi().getActiveUsers();
        } catch (GitLabApiException e) {
            return FormValidation.errorWithMarkup(Messages.GitLabServer_cannotConnect(Util.escape(e.getMessage())));
        }
        return FormValidation.warning(Messages.GitLabServer_someException());
    }

}
