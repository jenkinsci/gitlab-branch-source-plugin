package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;

import java.io.IOException;

import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;

public class GitLabSCMFileSystem extends SCMFileSystem {
    private final GitLabApi gitLabApi;
    private final Project project;
    private final String ref;

    protected GitLabSCMFileSystem(GitLabApi gitLabApi, Project project, String ref,
                                 @CheckForNull SCMRevision rev) throws IOException {
        super(rev);
        this.gitLabApi = gitLabApi;
        this.project = project;
        if (rev != null) {
            if (rev.getHead() instanceof MergeRequestSCMHead) {
                this.ref = ((MergeRequestSCMRevision) rev).getOrigin().getHash();
            } else if (rev instanceof BranchSCMRevision) {
                this.ref = ((BranchSCMRevision) rev).getHash();
            } else {
                this.ref = ref;
            }
        } else {
            this.ref = ref;
        }
    }

    @Override
    public long lastModified() throws IOException {
        // TODO Fix this
        return 0L;
    }

    @NonNull
    @Override
    public SCMFile getRoot() {
        return new GitLabSCMFile(gitLabApi, project, ref);
    }

    @Extension
    public static class BuilderImpl extends Builder {

        @Override
        public boolean supports(SCM source) {
            // TODO implement a GitLabSCM so we can work for those
            return false;
        }

        @Override
        public boolean supports(SCMSource source) {
            return source instanceof GitLabSCMSource;
        }

        @Override
        protected boolean supportsDescriptor(SCMDescriptor scmDescriptor) {
            return false;
        }

        @Override
        protected boolean supportsDescriptor(SCMSourceDescriptor scmSourceDescriptor) {
            return false;
        }

        @Override
        public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm, @CheckForNull SCMRevision rev) {
            return null;
        }

        @Override
        public SCMFileSystem build(@NonNull SCMSource source, @NonNull SCMHead head, @CheckForNull SCMRevision rev)
                throws IOException, InterruptedException {
            GitLabSCMSource src = (GitLabSCMSource) source;
            String projectOwner;
            String project;
            String ref;
            if (head instanceof MergeRequestSCMHead) {
                projectOwner = ((MergeRequestSCMHead) head).getOriginOwner();
                project = ((MergeRequestSCMHead) head).getOriginProject();
                ref = ((MergeRequestSCMHead) head).getOriginName();
            } else if (head instanceof BranchSCMHead) {
                projectOwner = src.getProjectOwner();
                project = src.getProject();
                ref = head.getName();
            } else {
                return null;
            }
            SCMSourceOwner owner = source.getOwner();
            String serverUrl = src.getServerUrl();
            String credentialsId = src.getCredentialsId();
            PersonalAccessToken credentials = StringUtils.isBlank(credentialsId)
                    ? null
                    : CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                            PersonalAccessToken.class,
                            owner,
                            Jenkins.getAuthentication(),
                            fromUri(serverUrl).build()),
                    CredentialsMatchers.withId(credentialsId)
            );
            if (owner != null) {
                CredentialsProvider.track(owner, credentials);
            }
            GitLabApi gitLabApi;
            if(credentials == null) {
                gitLabApi = new GitLabApi(serverUrl, "");
            } else {
                gitLabApi = new GitLabApi(serverUrl, credentials.getToken().getPlainText());
            }
            // TODO needs review
            try {
                return new GitLabSCMFileSystem(gitLabApi, gitLabApi.getProjectApi().getProject(projectOwner, project), ref, rev);
            } catch (GitLabApiException e) {
                throw new IOException(e);
            }
        }
    }
}
