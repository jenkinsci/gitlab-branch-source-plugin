package io.jenkins.plugins.gitlabbranchsource;

import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.getServerUrlFromName;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.projectUriTemplate;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.splitPath;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.damnhandy.uri.template.UriTemplate;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.plugins.git.GitSCM;
import hudson.security.ACL;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabBrowser;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import java.net.URI;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import jenkins.plugins.git.GitSCMBuilder;
import jenkins.plugins.git.MergeWithGitSCMExtension;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RefSpec;

/**
 * Builds a {@link GitSCM} for {@link GitLabSCMSource}.
 */
public class GitLabSCMBuilder extends GitSCMBuilder<GitLabSCMBuilder> {

    private static final SecureRandom RANDOM = new SecureRandom();

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
     * The repository name.
     */
    @NonNull
    private final String projectPath;

    private final String sshRemote;

    private final String httpRemote;

    /**
     * Constructor
     *
     * @param source the {@link GitLabSCMSource}
     * @param head the {@link SCMHead}
     * @param revision the (optional) {@link SCMRevision}
     */
    public GitLabSCMBuilder(
            @NonNull GitLabSCMSource source, @NonNull SCMHead head, @CheckForNull SCMRevision revision) {
        super(head, revision, source.getHttpRemote(), source.getCredentialsId());
        this.context = source.getOwner();
        serverUrl = defaultIfBlank(getServerUrlFromName(source.getServerName()), GitLabServer.GITLAB_SERVER_URL);
        projectPath = source.getProjectPath();
        sshRemote = source.getSshRemote();
        httpRemote = source.getHttpRemote();
        // configure the ref specs
        withoutRefSpecs();
        String projectUrl;
        if (head instanceof MergeRequestSCMHead) {
            MergeRequestSCMHead h = (MergeRequestSCMHead) head;
            withRefSpec("+refs/merge-requests/" + h.getId() + "/head:refs/remotes/@{remote}/" + head.getName());
            projectUrl = projectUrl(h.getOriginProjectPath());
        } else if (head instanceof GitLabTagSCMHead) {
            withRefSpec("+refs/tags/" + head.getName() + ":refs/tags/" + head.getName());
            projectUrl = projectUrl(projectPath);
        } else {

            withRefSpec("+refs/heads/" + head.getName() + ":refs/remotes/@{remote}/" + head.getName());
            projectUrl = projectUrl(projectPath);
        }
        withBrowser(new GitLabBrowser(projectUrl));
    }

    /**
     * Returns a {@link UriTemplate} for checkout according to credentials configuration.
     *
     * @param context the context within which to resolve the credentials.
     * @param serverUrl the server url
     * @param sshRemote the SSH remote URL for the project.
     * @param httpRemote the HTTPS remote URL for the project.
     * @param credentialsId the credentials.
     * @param projectPath the full path to the project (with namespace).
     * @return a {@link UriTemplate}
     */
    public static UriTemplate checkoutUriTemplate(
            @CheckForNull Item context,
            @NonNull String serverUrl,
            @CheckForNull String httpRemote,
            @CheckForNull String sshRemote,
            @CheckForNull String credentialsId,
            @NonNull String projectPath) {

        if (credentialsId != null && sshRemote != null) {
            URIRequirementBuilder builder = URIRequirementBuilder.create();
            URI serverUri = URI.create(serverUrl);
            if (serverUri.getHost() != null) {
                builder.withHostname(serverUri.getHost());
            }
            StandardUsernameCredentials credentials = CredentialsProvider.findCredentialByIdInItem(
                    credentialsId,
                    StandardUsernameCredentials.class,
                    context,
                    context instanceof Queue.Task t ? t.getDefaultAuthentication2() : ACL.SYSTEM2,
                    builder.build());
            if (credentials instanceof SSHUserPrivateKey) {
                return UriTemplate.buildFromTemplate(sshRemote).build();
            }
        }
        if (httpRemote != null) {
            return UriTemplate.buildFromTemplate(httpRemote).build();
        }
        return UriTemplate.buildFromTemplate(serverUrl + '/' + projectPath)
                .literal(".git")
                .build();
    }

    private String projectUrl(String projectPath) {
        return projectUriTemplate(serverUrl)
                .set("project", splitPath(projectPath))
                .expand();
    }

    /**
     * Returns a {@link UriTemplate} for checkout according to credentials configuration. Expects
     * the parameters {@code owner} and {@code repository} to be populated before expansion.
     *
     * @return a {@link UriTemplate}
     */
    @NonNull
    public final UriTemplate checkoutUriTemplate() {
        return checkoutUriTemplate(context, serverUrl, httpRemote, sshRemote, credentialsId(), projectPath);
    }

    /**
     * Updates the {@link GitSCMBuilder#withRemote(String)} based on the current {@link #head()} and
     * {@link #revision()}. Will be called automatically by {@link #build()} but exposed in case the
     * correct remote is required after changing the {@link #withCredentials(String)}.
     *
     * @return {@code this} for method chaining.
     */
    @NonNull
    public final GitLabSCMBuilder withGitLabRemote() {
        withRemote(checkoutUriTemplate().expand());
        final SCMHead h = head();
        String projectUrl;
        if (h instanceof MergeRequestSCMHead) {
            final MergeRequestSCMHead head = (MergeRequestSCMHead) h;
            projectUrl = projectUrl(head.getOriginProjectPath());
        } else {
            projectUrl = projectUrl(projectPath);
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
                        assert dst.startsWith(Constants.R_REFS) : "All git references must start with refs/";
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
                            localName =
                                    "remotes/" + remoteName() + "/merge-requests-" + head.getId() + "-upstream-" + name;
                        }
                        if (localNames.contains(localName)) {
                            // ok we're just going to mangle our way to something that works
                            while (localNames.contains(localName)) {
                                localName = "remotes/" + remoteName() + "/merge-requests-" + head.getId()
                                        + "-upstream-" + name
                                        + "-" + Integer.toHexString(RANDOM.nextInt(Integer.MAX_VALUE));
                            }
                        }
                        withRefSpec("+refs/heads/" + name + ":refs/" + localName);
                    }
                    withExtension(new MergeWithGitSCMExtension(
                            localName,
                            r instanceof MergeRequestSCMRevision
                                    ? ((BranchSCMRevision) ((MergeRequestSCMRevision) r).getTarget()).getHash()
                                    : null));
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
