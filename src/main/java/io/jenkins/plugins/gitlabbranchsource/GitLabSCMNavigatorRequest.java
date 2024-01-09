package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.trait.SCMNavigatorRequest;

public class GitLabSCMNavigatorRequest extends SCMNavigatorRequest {

    private final boolean wantSharedProjects;
    private boolean wantSubgroupProjects;

    private int projectNamingStrategy;

    protected GitLabSCMNavigatorRequest(
            @NonNull SCMNavigator source,
            @NonNull GitLabSCMNavigatorContext context,
            @NonNull SCMSourceObserver observer) {
        super(source, context, observer);
        wantSubgroupProjects = context.wantSubgroupProjects();
        wantSharedProjects = context.wantSharedProjects();
        projectNamingStrategy = context.withProjectNamingStrategy();
    }

    /**
     * @return whether to include subgroup projects
     */
    public boolean wantSubgroupProjects() {
        return wantSubgroupProjects;
    }

    /**
     *
     * @return wether to include projects that are shared with the group from other groups.
     */
    public boolean wantSharedProjects() {
        return wantSharedProjects;
    }

    /**
     * Returns the project naming strategy id.
     *
     * @return the project naming strategy id.
     */
    public int withProjectNamingStrategy() {
        return projectNamingStrategy;
    }
}
