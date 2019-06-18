package io.jenkins.plugins.gitlabbranchsource;

import jenkins.plugins.git.AbstractGitSCMSource;

public class BranchSCMRevision extends AbstractGitSCMSource.SCMRevisionImpl {
    public BranchSCMRevision(BranchSCMHead head, String hash) {
        super(head, hash);
    }
}

