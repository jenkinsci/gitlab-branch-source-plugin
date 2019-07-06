package io.jenkins.plugins.gitlabserverconfig.servers;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import java.net.MalformedURLException;
import java.net.URL;
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

import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

/**
 * Represents a GitLab Server instance.
 */
public class GitLabServer extends AbstractDescribableImpl<GitLabServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabServer.class);

    /**
     * The credentials matcher for GitLab Personal Access Token
     */
    public static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.instanceOf(PersonalAccessToken.class);

    /**
     * Used as default community saas version server URL for the serverUrl field
     */
    public static final String GITLAB_SERVER_URL = "https://gitlab.com";

    /**
     * Used as default token value if no any credentials found by given credentialsId.
     */
    public final static String EMPTY_TOKEN = "";

    /**
     * Length of unique random numeric name for server
     */
    private static final int SHORT_NAME_LENGTH = 4;

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
    private final String name;

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
     * The {@link PersonalAccessToken#getId()} of the credentials to use for auto-management of hooks.
     */
    @NonNull
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
     * Returns {@code true} if Jenkins is supposed to auto-manage hooks for this end-point.
     *
     * @return {@code true} if Jenkins is supposed to auto-manage hooks for this end-point.
     */
    public boolean isManageHooks() {
        return manageHooks;
    }

    /**
     * Returns The {@link PersonalAccessToken#getId()} of the credentials to use for GitLab Server Authentication to access GitLab APIs.
     *
     * @return The {@link PersonalAccessToken#getId()} of the credentials to use for GitLab Server Authentication to access GitLab APIs.
     */
    @NonNull
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Data Bound Constructor for only mandatory parameter serverUrl
     *
     * @param serverUrl     The URL of this GitLab Server
     * @param name          A unique name to use to describe the end-point, if empty replaced with a random name
     * @param credentialsId The {@link PersonalAccessToken#getId()} of the credentials to use for GitLab Server Authentication to access GitLab APIs
     */
    @DataBoundConstructor
    public GitLabServer(@NonNull String serverUrl, @NonNull String name, @NonNull String credentialsId) {
        this.serverUrl = defaultIfBlank(serverUrl, GITLAB_SERVER_URL);
        this.name = StringUtils.isBlank(name)
                ? getRandomName()
                : name;
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
     * Looks up for Personal Access Token
     *
     * @param serverUrl         GitLab Server Url
     * @param credentialsId     Credentials ListBox value
     *
     * @return {@link PersonalAccessToken}
     */
    public static PersonalAccessToken getCredentials(String serverUrl, String credentialsId) {
        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.ADMINISTER);
        return StringUtils.isBlank(credentialsId) ? null : CredentialsMatchers.firstOrNull(
                    lookupCredentials(
                            PersonalAccessToken.class,
                            jenkins,
                            ACL.SYSTEM,
                            fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL)).build()),
                            withId(credentialsId)
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
         * @param serverUrl the URL to check.
         * @return the validation results.
         */
        public static FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            try {
                new URL(serverUrl);
            } catch (MalformedURLException e) {
                LOGGER.error(String.format("Incorrect url: %s", serverUrl));
                return FormValidation.error("Malformed url (%s)", e.getMessage());
            }
            if (GITLAB_SERVER_URL.equals(serverUrl)) {
                LOGGER.info(String.format("Community version of GitLab: %s", serverUrl));
            }
            GitLabApi gitLabApi = new GitLabApi(serverUrl, "");
            try {
                gitLabApi.getProjectApi().getProjects(1, 1);
                return FormValidation.ok();
            } catch (GitLabApiException e) {
                LOGGER.info(String.format("Invalid GitLab Server Url: %s", serverUrl));
                return FormValidation.error(Messages.GitLabServer_invalidUrl(serverUrl));
            }
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
            LOGGER.info("Testing Connection for GitLab, url:" + serverUrl);
            PersonalAccessToken credentials = getCredentials(serverUrl, credentialsId);
            String privateToken = "";
            if(credentials != null) {
                privateToken = credentials.getToken().getPlainText();
            }
            if (privateToken.equals(EMPTY_TOKEN)) {
                GitLabApi gitLabApi = new GitLabApi(serverUrl, EMPTY_TOKEN);
                try {
                    gitLabApi.getProjectApi().getProjects(1, 1);
                    return FormValidation.ok("Valid GitLab Server but no credentials specified");
                } catch (GitLabApiException e) {
                    LOGGER.error("Invalid GitLab Server Url");
                    return FormValidation
                            .errorWithMarkup(Messages.GitLabServer_credentialsNotResolved(Util.escape(credentialsId)));
                }
            } else {

                GitLabApi gitLabApi = new GitLabApi(serverUrl, privateToken);
                try {
                    User user = gitLabApi.getUserApi().getCurrentUser();
                    LOGGER.info(String.format("Connection established with the GitLab Server for %s", user.getUsername()));
                    return FormValidation.ok(String.format("Credentials verified for user %s", user.getUsername()));
                } catch (GitLabApiException e) {
                    LOGGER.error(String.format("Failed to connect with GitLab Server - %s", e.getMessage()));
                    return FormValidation.error(e, Messages.GitLabServer_failedValidation(Util.escape(e.getMessage())));
                }
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
                            CREDENTIALS_MATCHER
                    );
        }
    }
}
