package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import java.io.IOException;
import java.util.Date;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Project;

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
        Date lastActivity = project.getLastActivityAt();
        if(lastActivity == null) {
            return 0;
        }
        return lastActivity.getTime();
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

        public SCMFileSystem build(@NonNull SCMHead head, @CheckForNull SCMRevision rev, @NonNull GitLabApi gitLabApi, @NonNull Project gitlabProject)
                throws IOException, InterruptedException {
            String ref;
            if (head instanceof MergeRequestSCMHead) {
                ref = ((MergeRequestSCMHead) head).getOriginName();
            } else if (head instanceof BranchSCMHead) {
                ref = head.getName();
            } else {
                return null;
            }
            return new GitLabSCMFileSystem(gitLabApi, gitlabProject, ref, rev);
        }
    }
}
