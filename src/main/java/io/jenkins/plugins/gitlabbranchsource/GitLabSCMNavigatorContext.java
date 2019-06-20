package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.trait.SCMNavigatorContext;

public class GitLabSCMNavigatorContext extends SCMNavigatorContext<GitLabSCMNavigatorContext, GitLabSCMNavigatorRequest> {
    @NonNull
    @Override
    public GitLabSCMNavigatorRequest newRequest(@NonNull SCMNavigator navigator, @NonNull SCMSourceObserver observer) {
        return new GitLabSCMNavigatorRequest(navigator, this, observer);
    }
}
