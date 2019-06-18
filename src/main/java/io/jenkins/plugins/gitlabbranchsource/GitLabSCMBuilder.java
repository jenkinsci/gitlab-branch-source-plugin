package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.plugins.git.GitSCM;
import hudson.security.ACL;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import jenkins.plugins.git.GitSCMBuilder;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RefSpec;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Builds a {@link GitSCM} for {@link GitLabSCMSource}.
 */
public class GitLabSCMBuilder extends GitSCMBuilder<GitLabSCMBuilder> {

    private static final String API_V4 = "api/v4";

    private static final String API_V3 = "api/v3";
    /**
     * The context within which credentials should be resolved.
     */
    @CheckForNull
    private final SCMSourceOwner context;

    /**
     * The server URL
     */
    @NonNull
    private final String serverUrl;

    /**
     * The repository owner.
     */
    @NonNull
    private final String projectOwner;

    /**
     * The repository name.
     */
    @NonNull
    private final String project;

    private final String sshRemote;

    /**
     * Constructor
     *
     * @param source    the {@link GitLabSCMSource}
     * @param head      the {@link SCMHead}
     * @param revision  the (optional) {@link SCMRevision}
     */
    public GitLabSCMBuilder(@Nonnull GitLabSCMSource source, @Nonnull SCMHead head, @CheckForNull SCMRevision revision) {
        super(
                head,
                revision,
                checkoutUriTemplate(null, source.getServerUrl(), null, null)
                        .set("owner", source.getProject())
                        .set("repository", source.getProject())
                        .expand(),
                source.getCredentialsId()
        );
        this.context = source.getOwner();
        serverUrl = StringUtils.defaultIfBlank(source.getServerUrl(), GitLabServer.GITLAB_SERVER_URL);
        projectOwner = source.getProjectOwner();
        project = source.getProject();
        sshRemote = source.getSshRemote();
        // configure the ref specs
        withoutRefSpecs();
        String projectUrl;
        if(head instanceof MergeRequestSCMHead) {
            MergeRequestSCMHead h = (MergeRequestSCMHead) head;
            withRefSpec("+refs/merge-requests" + h.getId() + "/head:refs/remotes/@{remote}/" + head.getName());
            projectUrl = projectUrl(h.getOriginOwner(), h.getOriginProject());
        } else {
            withRefSpec("+refs/heads/" + head.getName() + ":refs/remotes/@{remote}/" + head.getName());
            projectUrl = projectUrl(projectOwner, project);
        }
        withBrowser(new GitLabBrowser(projectUrl));
    }

    /**
     * Returns a {@link UriTemplate} for checkout according to credentials configuration.
     * Expects the parameters {@code owner} and {@code repository} to be populated before expansion.
     *
     * @param context       the context within which to resolve the credentials.
     * @param serverUrl     the server url
     * @param sshRemote     any valid SSH remote URL for the server.
     * @param credentialsId the credentials.
     * @return a {@link UriTemplate}
     */
    public static UriTemplate checkoutUriTemplate(@CheckForNull Item context,
                                                  @NonNull String serverUrl,
                                                  @CheckForNull String sshRemote,
                                                  @CheckForNull String credentialsId) {
        if (credentialsId != null && sshRemote != null) {
            URIRequirementBuilder builder = URIRequirementBuilder.create();
            URI serverUri = URI.create(serverUrl);
            if (serverUri.getHost() != null) {
                builder.withHostname(serverUri.getHost());
            }
            StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            StandardCredentials.class,
                            context,
                            context instanceof Queue.Task
                                    ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                                    : ACL.SYSTEM,
                            builder.build()
                    ),
                    CredentialsMatchers.allOf(
                            CredentialsMatchers.withId(credentialsId),
                            CredentialsMatchers.instanceOf(StandardCredentials.class)
                    )
            );
            if (credentials instanceof SSHUserPrivateKey) {
                URI sshUri = URI.create(sshRemote);
                return UriTemplate.buildFromTemplate(
                        "ssh://git@" + sshUri.getHost() + (sshUri.getPort() != 22 ? ":" + sshUri.getPort() : "")
                )
                        .path(UriTemplateBuilder.var("owner"))
                        .path(UriTemplateBuilder.var("repository"))
                        .literal(".git")
                        .build();
            }
            if (credentials instanceof PersonalAccessToken) {
                try {
                    URI tokenUri = new URI(
                            serverUri.getScheme(),
                            ((PersonalAccessToken) credentials).getToken().getPlainText(),
                            serverUri.getHost(),
                            serverUri.getPort(),
                            serverUri.getPath(),
                            serverUri.getQuery(),
                            serverUri.getFragment()
                    );
                    return UriTemplate.buildFromTemplate(tokenUri.toASCIIString())
                            .path(UriTemplateBuilder.var("owner"))
                            .path(UriTemplateBuilder.var("repository"))
                            .literal(".git")
                            .build();
                } catch (URISyntaxException e) {
                    // ok we are at the end of the road
                }
            }
        }
        return UriTemplate.buildFromTemplate(serverUrl)
                .path(UriTemplateBuilder.var("owner"))
                .path(UriTemplateBuilder.var("repository"))
                .literal(".git")
                .build();
    }

    private String projectUrl(String owner, String project) {
        return UriTemplate.buildFromTemplate(serverUrl)
                .path(UriTemplateBuilder.var("owner"))
                .path(UriTemplateBuilder.var("project"))
                .build()
                .set("owner", owner)
                .set("project", project)
                .expand();
    }

    /**
     * Returns a {@link UriTemplate} for checkout according to credentials configuration.
     * Expects the parameters {@code owner} and {@code repository} to be populated before expansion.
     *
     * @return a {@link UriTemplate}
     */
    @NonNull
    public final UriTemplate checkoutUriTemplate() {
        String credentialsId = credentialsId();
        return checkoutUriTemplate(context, serverUrl, sshRemote, credentialsId);
    }

    /**
     * Updates the {@link GitSCMBuilder#withRemote(String)} based on the current {@link #head()} and
     * {@link #revision()}.
     * Will be called automatically by {@link #build()} but exposed in case the correct remote is required after
     * changing the {@link #withCredentials(String)}.
     *
     * @return {@code this} for method chaining.
     */
    @NonNull
    public final GitLabSCMBuilder withGitLabRemote() {
        withRemote(checkoutUriTemplate().set("owner", projectOwner).set("repository", project).expand());
        final SCMHead h = head();
        String projectUrl;
        if (h instanceof MergeRequestSCMHead) {
            final MergeRequestSCMHead head = (MergeRequestSCMHead) h;
            projectUrl = projectUrl(head.getOriginOwner(), head.getOriginProject());
        } else {
            projectUrl = projectUrl(projectOwner, project);
        }
        if (projectUrl != null) {
            withBrowser(new GitLabBrowser(projectUrl));
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public GitSCM build() {
        final SCMHead h = head();
        final SCMRevision r = revision();
        try {
            withGitLabRemote();
            if (h instanceof MergeRequestSCMHead) {
                MergeRequestSCMHead head = (MergeRequestSCMHead) h;
                if (head.getCheckoutStrategy() == ChangeRequestCheckoutStrategy.MERGE) {
                    // add the target branch to ensure that the revision we want to merge is also available
                    String name = head.getTarget().getName();
                    String localName = "remotes/" + remoteName() + "/" + name;
                    Set<String> localNames = new HashSet<>();
                    boolean match = false;
                    String targetSrc = Constants.R_HEADS + name;
                    String targetDst = Constants.R_REMOTES + remoteName() + "/" + name;
                    for (RefSpec b : asRefSpecs()) {
                        String dst = b.getDestination();
                        assert dst.startsWith(Constants.R_REFS)
                                : "All git references must start with refs/";
                        if (targetSrc.equals(b.getSource())) {
                            if (targetDst.equals(dst)) {
                                match = true;
                            } else {
                                // pick up the configured destination name
                                localName = dst.substring(Constants.R_REFS.length());
                                match = true;
                            }
                        } else {
                            localNames.add(dst.substring(Constants.R_REFS.length()));
                        }
                    }
                    if (!match) {
                        if (localNames.contains(localName)) {
                            // conflict with intended name
                            localName = "remotes/" + remoteName() + "/upstream-" + name;
                        }
                        if (localNames.contains(localName)) {
                            // conflict with intended alternative name
                            localName = "remotes/" + remoteName() + "/merge-requests-" + head.getId() + "-upstream-" + name;
                        }
                        if (localNames.contains(localName)) {
                            // ok we're just going to mangle our way to something that works
                            Random entropy = new Random();
                            while (localNames.contains(localName)) {
                                localName = "remotes/" + remoteName() + "/merge-requests-" + head.getId() + "-upstream-" + name
                                        + "-" + Integer.toHexString(entropy.nextInt(Integer.MAX_VALUE));
                            }
                        }
                        withRefSpec("+refs/heads/" + name + ":refs/" + localName);
                    }
                    withExtension(new MergeWithGitSCMExtension(
                                    localName,
                                    r instanceof MergeRequestSCMRevision
                                            ? ((BranchSCMRevision) ((MergeRequestSCMRevision) r).getTarget()).getHash()
                                            : null
                            )
                    );
                }
                if (r instanceof MergeRequestSCMRevision) {
                    withRevision(((MergeRequestSCMRevision) r).getOrigin());
                }
            }
            return super.build();
        } finally {
            withHead(h);
            withRevision(r);
        }
    }

}
