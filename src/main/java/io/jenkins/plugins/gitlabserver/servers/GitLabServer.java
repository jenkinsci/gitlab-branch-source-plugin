package io.jenkins.plugins.gitlabserver.servers;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabserver.credentials.PersonalAccessToken;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMName;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.User;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

/**
 * Represents a GitLab Server instance.
 */

public class GitLabServer extends AbstractDescribableImpl<GitLabServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabServer.class);

    /**
     * Used as default server URL for the serverUrl field
     */
    public static final String GITLAB_SERVER_URL = "https://gitlab.com";
    /**
     * Used as default token value if no any credentials found by given credentialsId.
     */
    public static final String UNKNOWN_TOKEN = "UNKNOWN_TOKEN";

    /**
     * Length of unique random numeric name for server
     */
    public static final int SHORT_NAME_LENGTH = 4;

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
     * A unique name used to identify the endpoint.
     */
    @NonNull
    private String name;

    /**
     * The URL of this GitLab Server.
     */
    @NonNull
    private final String serverUrl;

    /**
     * {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     */
    private boolean manageHooks;

    /**
     * The {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for auto-management of hooks.
     */
    @CheckForNull
    private String credentialsId;

    /**
     * Generates a random alphanumeric name for gitlab server if not entered by user
     *
     * @return String
     */
    private String getRandomName() {
        return String.format("%s-%s", SCMName.fromUrl(this.serverUrl, COMMON_PREFIX_HOSTNAMES),
                RandomStringUtils.randomNumeric(SHORT_NAME_LENGTH));
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
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
     * Data Bound Constructor for only mandatory parameter serverUrl
     *
     * @param serverUrl   The URL of this GitLab Server
     */
    @DataBoundConstructor
    public GitLabServer(@NonNull String serverUrl) {
        this.serverUrl = defaultIfBlank(serverUrl, GITLAB_SERVER_URL);
    }

    /**
     * Data Bound Setter for Server Name
     *
     * @param name   A unique name to use to describe the end-point, if empty replaced with a random name
     */
    @DataBoundSetter
    public void setName(@NonNull String name) {
        this.name = StringUtils.isBlank(name)
                ? getRandomName()
                : name;
    }

    /**
     * Data Bound Setter for Server URL
     *
     * @param credentialsId The {@link StandardUsernamePasswordCredentials#getId()} of the credentials to use for
     *                      GitLab Server Authentication to access GitLab APIs

     */
    @DataBoundSetter
    public void setCredentialsId(@CheckForNull String credentialsId) {
        this.credentialsId = credentialsId;
    }

    /**
     * Data Bound Setter for auto management of web hooks
     *
     * @param manageHooks   {@code true} if and only if Jenkins is supposed to auto-manage hooks for this end-point.
     */
    @DataBoundSetter
    public void setManageHooks(boolean manageHooks) {
        this.manageHooks = manageHooks;
    }

    /**
     * Looks up the {@link UsernamePasswordCredentials} to use for auto-management of hooks.
     *
     * @return the credentials or {@code null}.
     */
    @CheckForNull
    public UsernamePasswordCredentials credentials() {
        return StringUtils.isBlank(credentialsId) ? null : CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        UsernamePasswordCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        fromUri(serverUrl).build()),
                        CredentialsMatchers.withId(credentialsId)
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
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                LOGGER.error("Incorrect server url - %s", value);
                return FormValidation.error("Malformed GitLab url (%s)", e.getMessage());
            }
            // TODO:[JENKINS-57747] Add support for GitLab Ultimate (self hosted) and Gold (saas)
            if (GITLAB_SERVER_URL.equals(value)) {
                LOGGER.info("Server URL is fine - %s", value);
                return FormValidation.ok();
            }
            LOGGER.info("Unable to validate serverUrl - %s", value);
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
            LOGGER.info("Testing Connection..");
            String privateToken = getToken(serverUrl, credentialsId);
            if (privateToken.equals(UNKNOWN_TOKEN)) {
                LOGGER.error("Cannot find private token");
                return FormValidation
                        .errorWithMarkup(Messages.GitLabServer_credentialsNotResolved(Util.escape(credentialsId)));
            }
            try {
                GitLabApi gitLabApi = new GitLabApi(serverUrl, privateToken);
                User user = gitLabApi.getUserApi().getCurrentUser();
                LOGGER.info(String.format("Connection established with the GitLab Server for %s", user.getUsername()));
                return FormValidation.ok(String.format("Credentials verified for user %s", user.getUsername()));
            } catch (GitLabApiException e) {
                LOGGER.error(String.format("Failed to connect with GitLab Server - %s", e.getMessage()));
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
        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl,
                                                     @QueryParameter String credentialsId) {
            Jenkins jenkins = Jenkins.get();
            if (!jenkins.hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(ACL.SYSTEM,
                            jenkins,
                            StandardCredentials.class,
                            fromUri(serverUrl).build(),
                            credentials -> credentials instanceof PersonalAccessToken);
        }

        private static String getToken(String serverUrl, String credentialsId) {
            String privateToken = UNKNOWN_TOKEN;
            Jenkins jenkins = Jenkins.get();
            jenkins.checkPermission(Jenkins.ADMINISTER);

            PersonalAccessToken credentials = CredentialsMatchers.firstOrNull(
                    lookupCredentials(
                            PersonalAccessToken.class,
                            jenkins,
                            ACL.SYSTEM,
                            fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL)).build()),
                            CredentialsMatchers.withId(credentialsId)
                    );
            if (credentials != null) {
                privateToken = credentials.getToken().getPlainText();
            }

            return privateToken;
        }
    }
}
