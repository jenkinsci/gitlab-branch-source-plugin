package io.jenkins.plugins.gitlabserver.servers.helpers;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.domains.SchemeSpecification;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.gitlabserver.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserver.credentials.PersonalAccessTokenImpl;
import io.jenkins.plugins.gitlabserver.servers.GitLabServer;
import jenkins.model.Jenkins;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.utils.AccessTokenUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;

@Extension
public class GitLabPersonalAccessTokenCreator extends Descriptor<GitLabPersonalAccessTokenCreator> implements
        Describable<GitLabPersonalAccessTokenCreator> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabPersonalAccessTokenCreator.class);

    private static final List<AccessTokenUtils.Scope> GL_PLUGIN_REQUIRED_SCOPE = ImmutableList.of(
            AccessTokenUtils.Scope.API,
            AccessTokenUtils.Scope.READ_REGISTRY,
            AccessTokenUtils.Scope.READ_USER,
            AccessTokenUtils.Scope.READ_REPOSITORY
    );

    public GitLabPersonalAccessTokenCreator() {
        super(GitLabPersonalAccessTokenCreator.class);
    }

    @Override
    public Descriptor<GitLabPersonalAccessTokenCreator> getDescriptor() {
        return this;
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return "Convert login and password to token";
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillCredentialsIdItems(@QueryParameter String serverUrl, @QueryParameter String credentialsId) {
        Jenkins jenkins = Jenkins.get();
        if(!jenkins.hasPermission(Jenkins.ADMINISTER)) {
            return new StandardListBoxModel().includeCurrentValue(credentialsId);
        }
        return new StandardUsernameListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        ACL.SYSTEM,
                        jenkins,
                        StandardUsernamePasswordCredentials.class,
                        fromUri(defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL)).build(),
                        CredentialsMatchers.always()
                ).includeMatchingAs(
                        Jenkins.getAuthentication(),
                        jenkins,
                        StandardUsernamePasswordCredentials.class,
                        fromUri(defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL)).build(),
                        CredentialsMatchers.always()
                );
    }

    @SuppressWarnings("unused")
    @RequirePOST
    public FormValidation doCreateTokenByCredentials(
            @QueryParameter String serverUrl,
            @QueryParameter String credentialsId) {

        Jenkins jenkins = Jenkins.get();
        jenkins.checkPermission(Jenkins.ADMINISTER);
        if(isEmpty(credentialsId)) {
            return FormValidation.error("Please specify credentials to create token");
        }

        StandardUsernamePasswordCredentials credentials = firstOrNull(lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    jenkins,
                    ACL.SYSTEM,
                    fromUri(defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL)).build()),
                    withId(credentialsId));

        if(credentials == null) {
            credentials = firstOrNull(lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    jenkins,
                    Jenkins.getAuthentication(),
                    fromUri(defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL)).build()),
                    withId(credentialsId));
        }

        if(Objects.isNull(credentials)) {
            return FormValidation.error("Can't create GitLab token, credentials are null");
        }
        try {
            String tokenName = UUID.randomUUID().toString();
            String token = AccessTokenUtils.createPersonalAccessToken(
                    defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL),
                    credentials.getUsername(),
                    Secret.toString(credentials.getPassword()),
                    tokenName,
                    GL_PLUGIN_REQUIRED_SCOPE
            );
            createCredentials(serverUrl, token, credentials.getUsername(), tokenName);
            return FormValidation.ok("Created credentials with id %s ", tokenName);
        } catch (GitLabApiException e) {
            return FormValidation.error(e, "Can't create GL token - %s", e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    @RequirePOST
    public FormValidation doCreateTokenByPassword(
            @QueryParameter String serverUrl,
            @QueryParameter String login,
            @QueryParameter String password) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        try {
            String tokenName = UUID.randomUUID().toString();
            String token = AccessTokenUtils.createPersonalAccessToken(
                    defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL),
                    login,
                    password,
                    tokenName,
                    GL_PLUGIN_REQUIRED_SCOPE
            );
            createCredentials(serverUrl, token, login, tokenName);
            return FormValidation.ok(
                    "Created credentials with id %s", tokenName
            );
        } catch (GitLabApiException e) {
            return FormValidation.error(e, "Can't create GL token for %s - %s", login, e.getMessage());
        }
    }

    /**
     * Creates {@link io.jenkins.plugins.gitlabserver.credentials.PersonalAccessToken}
     * with previously created GitLab Personal Access Token.
     *
     * @param serverUrl to add to domain with host and scheme requirement from this url
     * @param token        GitLab Personal Access Token
     * @param username     used to add to description of newly created credentials
     *
     * @see #saveCredentials(String, PersonalAccessToken)
     */
    private void createCredentials(@Nullable String serverUrl, String token, String username, String tokenName) {
        String url = defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL);
        String description = String.format("Auto Generated by %s server for %s user", url, username);
        PersonalAccessToken credentials = new PersonalAccessTokenImpl(
                CredentialsScope.GLOBAL,
                tokenName,
                description,
                token
        );
        saveCredentials(url, credentials);
    }

    /**
     * Saves given credentials in jenkins for domain extracted from server url
     * Adds them to domain extracted from server url (will be generated if no any exists before).
     * Domain will have domain requirements consists of scheme and host from serverUrl arg
     *
     * @param serverUrl to extract (and create if no any) domain
     * @param credentials to save credentials
     */
    private void saveCredentials(String serverUrl, final PersonalAccessToken credentials) {
        URI serverUri = URI.create(defaultIfBlank(serverUrl, GitLabServer.GITLAB_SERVER_URL));

        List<DomainSpecification> specifications = asList(
                new SchemeSpecification(serverUri.getScheme()),
                new HostnameSpecification(serverUri.getHost(), null)
        );

        final Domain domain = new Domain(serverUri.getHost(), "GitLab domain (autogenerated)", specifications);
        try (ACLContext acl = ACL.as(ACL.SYSTEM)) {
            new SystemCredentialsProvider.StoreImpl().addDomain(domain, credentials);
        } catch (IOException e) {
            LOGGER.error("Can't add credentials for domain", e);
        }
    }
}
