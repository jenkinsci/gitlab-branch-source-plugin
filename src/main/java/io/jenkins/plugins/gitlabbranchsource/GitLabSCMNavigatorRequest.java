package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.trait.SCMNavigatorRequest;

public class GitLabSCMNavigatorRequest extends SCMNavigatorRequest {
    protected GitLabSCMNavigatorRequest(@NonNull SCMNavigator source,
                                       @NonNull GitLabSCMNavigatorContext context,
                                       @NonNull SCMSourceObserver observer) {
        super(source, context, observer);
    }
}
