package io.jenkins.plugins.gitlabserverconfig.servers;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.getPrivateTokenAsPlainText;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.getProxyConfig;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.gitlabserverconfig.credentials.helpers.GitLabCredentialMatcher;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMName;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.User;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

/**
 * Represents a GitLab Server instance.
 */
public class GitLabServer extends AbstractDescribableImpl<GitLabServer> {

    /**
     * The credentials matcher for PersonalAccessToken and StringCredentials
     */
    public static final CredentialsMatcher CREDENTIALS_MATCHER = new GitLabCredentialMatcher();
    /**
     * Default name for community SaaS version server
     */
    public static final String GITLAB_SERVER_DEFAULT_NAME = "default";
    /**
     * Used as default community SaaS version server URL for the serverUrl field
     */
    public static final String GITLAB_SERVER_URL = "https://gitlab.com";
    /**
     * Used as default token value if no any credentials found by given
     * credentialsId.
     */
    public static final String EMPTY_TOKEN = "";

    public static final Logger LOGGER = Logger.getLogger(GitLabServer.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * Length of unique random numeric name for server
     */
    private static final int SHORT_NAME_LENGTH = 4;

    /**
     * Common prefixes that we should remove when inferring a display name.
     */
    private static final String[] COMMON_PREFIX_HOSTNAMES = {"git.", "gitlab.", "vcs.", "scm.", "source."};

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
     * {@code true} if and only if Jenkins is supposed to auto-manage web hooks for
     * this end-point.
     */
    private boolean manageWebHooks;

    /**
     * {@code true} if and only if Jenkins is supposed to auto-manage system hooks
     * for this
     * end-point.
     */
    private boolean manageSystemHooks;

    /**
     * The {@link StandardCredentials#getId()} of the credentials to use for
     * auto-management of
     * hooks.
     */
    @NonNull
    private String credentialsId;

    /**
     * The Jenkins root URL to use in Gitlab hooks, instead of
     * {@link Jenkins#getRootUrl()}.
     * Useful when the main public Jenkins URL can't be accessed from Gitlab.
     */
    private String hooksRootUrl;

    /**
     * The secret token used while setting up hook url in the GitLab server
     * @Deprecated Use webhookSecretCredentialsId instead
     */
    private transient Secret secretToken;

    /**
     * The credentials id of the webhook secret token used while setting up hook url in the GitLab server
     */
    @NonNull
    private String webhookSecretCredentialsId;

    /**
     * The credentials matcher for StringCredentials
     */
    public static final CredentialsMatcher WEBHOOK_SECRET_CREDENTIALS_MATCHER =
            CredentialsMatchers.instanceOf(StringCredentials.class);

    /**
     * {@code true} if and only if Jenkins should trigger a build immediately on a
     * GitLab Web Hook trigger.
     */
    private boolean immediateHookTrigger;

    /**
     * Delay to be used for GitLab Web Hook build triggers.
     */
    private Integer hookTriggerDelay;

    /**
     * Data Bound Constructor for only mandatory parameter serverUrl
     *
     * @param serverUrl     The URL of this GitLab Server
     * @param name          A unique name to use to describe the end-point, if empty
     *                      replaced with a random
     *                      name
     * @param credentialsId The {@link StandardCredentials#getId()} of the
     *                      credentials to use for
     *                      GitLab Server Authentication to access GitLab APIs
     */
    @DataBoundConstructor
    public GitLabServer(@NonNull String serverUrl, @NonNull String name, @NonNull String credentialsId) {
        this.serverUrl = defaultIfBlank(StringUtils.trim(serverUrl), GITLAB_SERVER_URL);
        this.name = StringUtils.isBlank(name) ? getRandomName() : StringUtils.trim(name);
        this.credentialsId = credentialsId;
        this.webhookSecretCredentialsId = "";
    }

    /**
     * Generates a random alphanumeric name for gitlab server if not entered by user
     *
     * @return String
     */
    private String getRandomName() {
        return String.format(
                "%s-%s",
                SCMName.fromUrl(this.serverUrl, COMMON_PREFIX_HOSTNAMES),
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
     * Returns {@code true} if Jenkins is supposed to auto-manage web hooks for this
     * end-point.
     *
     * @return {@code true} if Jenkins is supposed to auto-manage web hooks for this
     *         end-point.
     */
    public boolean isManageWebHooks() {
        return manageWebHooks;
    }

    /**
     * Data Bound Setter for auto management of web hooks
     *
     * @param manageWebHooks {@code true} if and only if Jenkins is supposed to
     *                       auto-manage web
     *                       hooks for this end-point.
     */
    @DataBoundSetter
    public void setManageWebHooks(boolean manageWebHooks) {
        this.manageWebHooks = manageWebHooks;
    }

    /**
     * Returns {@code true} if Jenkins is supposed to auto-manage system hooks for
     * this end-point.
     *
     * @return {@code true} if Jenkins is supposed to auto-manage system hooks for
     *         this end-point.
     */
    public boolean isManageSystemHooks() {
        return manageSystemHooks;
    }

    /**
     * Data Bound Setter for auto management of system hooks
     *
     * @param manageSystemHooks {@code true} if and only if Jenkins is supposed to
     *                          auto-manage
     *                          system hooks for this end-point.
     */
    @DataBoundSetter
    public void setManageSystemHooks(boolean manageSystemHooks) {
        this.manageSystemHooks = manageSystemHooks;
    }

    /**
     * Returns The {@link StandardCredentials#getId()} of the credentials to use for
     * GitLab Server
     * Authentication to access GitLab APIs.
     *
     * @return The {@link StandardCredentials#getId()} of the credentials to use for
     *         GitLab Server
     *         Authentication to access GitLab APIs.
     */
    @NonNull
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Looks up for PersonalAccessToken and StringCredentials
     *
     * @return {@link StandardCredentials}
     */
    public StandardCredentials getCredentials(AccessControlled context) {
        Jenkins jenkins = Jenkins.get();
        if (context == null) {
            jenkins.checkPermission(CredentialsProvider.USE_OWN);
        } else {
            context.checkPermission(CredentialsProvider.USE_OWN);
        }
        return StringUtils.isBlank(credentialsId)
                ? null
                : CredentialsMatchers.firstOrNull(
                        lookupCredentials(
                                StandardCredentials.class,
                                jenkins,
                                ACL.SYSTEM,
                                fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL))
                                        .build()),
                        withId(credentialsId));
    }

    /**
     * @param hooksRootUrl a custom root URL, to be used in hooks instead of
     *                     {@link Jenkins#getRootUrl()}.
     *                     Set to {@code null} for default behavior.
     */
    @DataBoundSetter
    public void setHooksRootUrl(String hooksRootUrl) {
        this.hooksRootUrl = Util.fixEmptyAndTrim(hooksRootUrl);
    }

    /**
     * @return the custom root URL, to be used in hooks instead of
     *         {@link Jenkins#getRootUrl()}.
     *         Can be either a root URL with its trailing slash, or {@code null}.
     */
    @CheckForNull
    public String getHooksRootUrl() {
        return Util.ensureEndsWith(Util.fixEmptyAndTrim(hooksRootUrl), "/");
    }

    @DataBoundSetter
    @Deprecated
    public void setSecretToken(Secret token) {
        this.secretToken = token;
    }

    @DataBoundSetter
    public void setWebhookSecretCredentialsId(String token) {
        this.webhookSecretCredentialsId = token;
    }

    public String getWebhookSecretCredentialsId() {
        return webhookSecretCredentialsId;
    }

    /**
     * Looks up for StringCredentials
     *
     * @return {@link StringCredentials}
     */
    public StringCredentials getWebhookSecretCredentials(AccessControlled context) {
        Jenkins jenkins = Jenkins.get();
        if (context == null) {
            jenkins.checkPermission(CredentialsProvider.USE_OWN);
            return StringUtils.isBlank(webhookSecretCredentialsId)
                    ? null
                    : CredentialsMatchers.firstOrNull(
                            lookupCredentials(
                                    StringCredentials.class,
                                    jenkins,
                                    ACL.SYSTEM,
                                    fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL))
                                            .build()),
                            withId(webhookSecretCredentialsId));
        } else {
            context.checkPermission(CredentialsProvider.USE_OWN);
            if (context instanceof ItemGroup) {
                return StringUtils.isBlank(webhookSecretCredentialsId)
                        ? null
                        : CredentialsMatchers.firstOrNull(
                                lookupCredentials(
                                        StringCredentials.class,
                                        (ItemGroup) context,
                                        ACL.SYSTEM,
                                        fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL))
                                                .build()),
                                withId(webhookSecretCredentialsId));
            } else {
                return StringUtils.isBlank(webhookSecretCredentialsId)
                        ? null
                        : CredentialsMatchers.firstOrNull(
                                lookupCredentials(
                                        StringCredentials.class,
                                        (Item) context,
                                        ACL.SYSTEM,
                                        fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL))
                                                .build()),
                                withId(webhookSecretCredentialsId));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Deprecated
    public Secret getSecretToken() {
        return secretToken;
    }

    private StringCredentials getWebhookSecretCredentials(String webhookSecretCredentialsId) {
        Jenkins jenkins = Jenkins.get();
        return StringUtils.isBlank(webhookSecretCredentialsId)
                ? null
                : CredentialsMatchers.firstOrNull(
                        lookupCredentials(
                                StringCredentials.class, jenkins, ACL.SYSTEM, new ArrayList<DomainRequirement>()),
                        withId(webhookSecretCredentialsId));
    }

    public String getSecretTokenAsPlainText() {
        StringCredentials credentials = getWebhookSecretCredentials(webhookSecretCredentialsId);
        String secretToken = "";
        if (credentials != null) {
            secretToken = credentials.getSecret().getPlainText();
        } else {
            return null;
        }
        return secretToken;
    }

    /**
     * Migrate webhook secret token to Jenkins credentials
     *
     * @return {@code true} if migration occurred, {@code false} otherwise
     * @see GitLabServers#migrateWebhookSecretsToCredentials
     */
    boolean migrateWebhookSecretCredentials() {
        if (!StringUtils.isBlank(webhookSecretCredentialsId) || secretToken == null) {
            return false;
        }
        final List<StringCredentials> credentials = CredentialsProvider.lookupCredentials(
                StringCredentials.class, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());
        for (final StringCredentials cred : credentials) {
            if (StringUtils.equals(secretToken.getPlainText(), Secret.toString(cred.getSecret()))) {
                // If a credential has the same secret, use it.
                webhookSecretCredentialsId = cred.getId();
                break;
            }
        }
        if (StringUtils.isBlank(webhookSecretCredentialsId)) {
            // If we couldn't find any existing credentials, create new credential
            final StringCredentials newCredentials = new StringCredentialsImpl(
                    CredentialsScope.GLOBAL,
                    null,
                    "Migrated from gitlab-branch-source-plugin webhook secret",
                    secretToken);
            SystemCredentialsProvider.getInstance().getCredentials().add(newCredentials);
            webhookSecretCredentialsId = newCredentials.getId();
        }
        secretToken = null;
        return true;
    }

    /**
     * Returns {@code true} if Jenkins should trigger a build immediately on a
     * GitLab Web Hook trigger.
     *
     * @return {@code true} if Jenkins should trigger a build immediately on a
     *         GitLab Web Hook trigger.
     */
    public boolean isImmediateHookTrigger() {
        return immediateHookTrigger;
    }

    /**
     * Data Bound Setter for immediate build on a GitLab Web Hook trigger.
     *
     * @param immediateHookTrigger {@code true} if and only if Jenkins should
     *                             trigger a build immediately on a
     *                             GitLab Web Hook trigger.
     */
    @DataBoundSetter
    public void setImmediateHookTrigger(boolean immediateHookTrigger) {
        this.immediateHookTrigger = immediateHookTrigger;
    }

    /**
     * Data Bound Setter for web hook trigger delay
     *
     * @param hookTriggerDelay Delay to be used for GitLab Web Hook build triggers.
     *                         Set to {@code null} to use delay equal to GitLab
     *                         cache timeout, which
     *                         will avoid builds being not triggered due to GitLab
     *                         caching.
     */
    @DataBoundSetter
    public void setHookTriggerDelay(String hookTriggerDelay) {
        try {
            this.hookTriggerDelay = Integer.parseInt(hookTriggerDelay);
        } catch (NumberFormatException e) {
            this.hookTriggerDelay = null;
        }
    }

    /**
     * @return Delay to be used for GitLab Web Hook build triggers.
     *         Can be either a root URL with its trailing slash, or {@code null}.
     *         Can be {@code null} to request delay to be equal to GitLab cache
     *         timeout.
     */
    @CheckForNull
    public Integer getHookTriggerDelay() {
        return this.hookTriggerDelay;
    }

    /**
     * Our descriptor.
     */
    @Symbol("gitLabServer")
    @Extension
    public static class DescriptorImpl extends Descriptor<GitLabServer> {

        /**
         * Checks that the supplied URL is valid.
         *
         * @param serverUrl the URL to check.
         * @return the validation results.
         */
        @POST
        public static FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {
            Jenkins.get().checkPermission(Jenkins.MANAGE);
            try {
                new URL(serverUrl);
            } catch (MalformedURLException e) {
                LOGGER.log(Level.SEVERE, String.format("Incorrect url: %s", serverUrl));
                return FormValidation.error("Malformed url (%s)", e.getMessage());
            }
            if (GITLAB_SERVER_URL.equals(serverUrl)) {
                LOGGER.log(Level.FINEST, String.format("Community version of GitLab: %s", serverUrl));
            }
            GitLabApi gitLabApi = new GitLabApi(serverUrl, "", null, getProxyConfig(serverUrl));
            try {
                gitLabApi.getProjectApi().getProjects(1, 1);
                return FormValidation.ok();
            } catch (GitLabApiException e) {
                LOGGER.log(Level.FINEST, String.format("Invalid GitLab Server Url: %s", serverUrl));
                return FormValidation.error(Messages.GitLabServer_invalidUrl(serverUrl));
            }
        }

        /**
         * Checks that the supplied URL looks like a valid Jenkins root URL.
         *
         * @param hooksRootUrl the URL to check.
         * @return the validation results.
         */
        public static FormValidation doCheckHooksRootUrl(@QueryParameter String hooksRootUrl) {
            if (StringUtils.isBlank(hooksRootUrl)) {
                return FormValidation.ok();
            }
            try {
                new URL(hooksRootUrl);
            } catch (MalformedURLException e) {
                LOGGER.log(Level.FINEST, "Malformed hooks root URL: {0}", hooksRootUrl);
                return FormValidation.error("Malformed url (%s)", e.getMessage());
            }
            if (hooksRootUrl.endsWith("/post")
                    || hooksRootUrl.contains("/gitlab-webhook")
                    || hooksRootUrl.contains("/gitlab-systemhook")) {
                LOGGER.log(Level.FINEST, "Dubious hooks root URL: {0}", hooksRootUrl);
                return FormValidation.warning("This looks like a full webhook URL, it should only be a root URL.");
            }
            return FormValidation.ok();
        }

        /**
         * Checks that the supplied hook trigger delay is valid.
         *
         * @param hookTriggerDelay the delay to be checked.
         * @return the validation results.
         */
        public static FormValidation doCheckHookTriggerDelay(@QueryParameter String hookTriggerDelay) {
            try {
                if (!hookTriggerDelay.isEmpty()) {
                    Integer.parseInt(hookTriggerDelay);
                }
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                LOGGER.log(Level.FINEST, "Invalid hook trigger delay: {0}", hookTriggerDelay);
                return FormValidation.error("Invalid hook trigger delay (%s)", e.getMessage());
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
        public FormValidation doTestConnection(@QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            StandardCredentials credentials = getCredentials(serverUrl, credentialsId);
            String privateToken = getPrivateTokenAsPlainText(credentials);
            if (privateToken.equals(EMPTY_TOKEN)) {
                GitLabApi gitLabApi = new GitLabApi(serverUrl, EMPTY_TOKEN, null, getProxyConfig(serverUrl));
                try {
                    /*
                     * In order to validate a GitLab Server without personal access token,
                     * we are fetching 1 project from the GitLab Server. If no project exists,
                     * it returns an empty list. If no server exists at the specified endpoint,
                     * it raises GitLabAPIException.
                     */
                    gitLabApi.getProjectApi().getProjects(1, 1);
                    return FormValidation.ok("Valid GitLab Server but no credentials specified");
                } catch (GitLabApiException e) {
                    LOGGER.log(Level.SEVERE, "Invalid GitLab Server Url");
                    return FormValidation.errorWithMarkup(
                            Messages.GitLabServer_credentialsNotResolved(Util.escape(credentialsId)));
                }
            } else {
                GitLabApi gitLabApi = new GitLabApi(serverUrl, privateToken, null, getProxyConfig(serverUrl));
                try {
                    User user = gitLabApi.getUserApi().getCurrentUser();
                    LOGGER.log(
                            Level.FINEST,
                            String.format("Connection established with the GitLab Server for %s", user.getUsername()));
                    return FormValidation.ok(String.format("Credentials verified for user %s", user.getUsername()));
                } catch (GitLabApiException e) {
                    LOGGER.log(
                            Level.SEVERE, String.format("Failed to connect with GitLab Server - %s", e.getMessage()));
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
        public ListBoxModel doFillCredentialsIdItems(
                @QueryParameter String serverUrl, @QueryParameter String credentialsId) {
            Jenkins jenkins = Jenkins.get();
            if (!jenkins.hasPermission(Jenkins.MANAGE)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            jenkins,
                            StandardCredentials.class,
                            fromUri(serverUrl).build(),
                            CREDENTIALS_MATCHER);
        }

        /**
         * Stapler form completion.
         *
         * @param webhookSecretCredentialsId the webhook secret credentials Id
         * @return the available credentials.
         */
        @Restricted(NoExternalUse.class) // stapler
        @SuppressWarnings("unused")
        public ListBoxModel doFillWebhookSecretCredentialsIdItems(
                @QueryParameter String serverUrl, @QueryParameter String webhookSecretCredentialsId) {
            Jenkins jenkins = Jenkins.get();
            if (!jenkins.hasPermission(Jenkins.MANAGE)) {
                return new StandardListBoxModel().includeCurrentValue(webhookSecretCredentialsId);
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            jenkins,
                            StringCredentials.class,
                            fromUri(serverUrl).build(),
                            WEBHOOK_SECRET_CREDENTIALS_MATCHER);
        }

        private StandardCredentials getCredentials(String serverUrl, String credentialsId) {
            Jenkins jenkins = Jenkins.get();
            jenkins.checkPermission(Jenkins.MANAGE);
            return StringUtils.isBlank(credentialsId)
                    ? null
                    : CredentialsMatchers.firstOrNull(
                            lookupCredentials(
                                    StandardCredentials.class,
                                    jenkins,
                                    ACL.SYSTEM,
                                    fromUri(defaultIfBlank(serverUrl, GITLAB_SERVER_URL))
                                            .build()),
                            withId(credentialsId));
        }
    }
}
