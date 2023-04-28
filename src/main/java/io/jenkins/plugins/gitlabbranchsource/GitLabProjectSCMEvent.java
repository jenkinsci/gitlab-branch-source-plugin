package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceEvent;
import org.gitlab4j.api.systemhooks.ProjectSystemHookEvent;

public class GitLabProjectSCMEvent extends SCMSourceEvent<ProjectSystemHookEvent> {

    public GitLabProjectSCMEvent(ProjectSystemHookEvent projectSystemHookEvent, String origin) {
        super(typeOf(projectSystemHookEvent), projectSystemHookEvent, origin);
    }

    private static Type typeOf(ProjectSystemHookEvent projectSystemHookEvent) {
        switch (projectSystemHookEvent.getEventName()) {
            case ProjectSystemHookEvent.PROJECT_CREATE_EVENT:
                return Type.CREATED;
            case ProjectSystemHookEvent.PROJECT_DESTROY_EVENT:
                return Type.REMOVED;
            case ProjectSystemHookEvent.PROJECT_UPDATE_EVENT:
                return Type.UPDATED;
            default:
                throw new IllegalArgumentException("cannot handle system-hook " + projectSystemHookEvent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String descriptionFor(@NonNull SCMNavigator navigator) {
        return description();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        return "Project event to branch " + getPayload().getPath() + " in namespace "
                + getPayload().getPathWithNamespace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String descriptionFor(SCMSource source) {
        return "Project event in " + getPayload().getPathWithNamespace();
    }

    @NonNull
    @Override
    public String getSourceName() {
        return getPayload().getPathWithNamespace();
    }

    @Override
    public boolean isMatch(@NonNull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    private boolean isMatch(@NonNull GitLabSCMNavigator navigator) {
        switch (getType()) {
            case CREATED:
                String projectPathWithNamespace = getPayload().getPathWithNamespace();
                String projectOwner = GitLabSCMNavigator.getProjectOwnerFromNamespace(projectPathWithNamespace);
                if (navigator.isGroup()) {
                    // checks when project owner is a Group
                    if (navigator.isWantSubGroupProjects()) {
                        // can be a subgroup so needs to at least start with the project owner when subgroup projects
                        // are required
                        return projectOwner.startsWith(navigator.getProjectOwner());
                    } else {
                        // when subgroup projects are not required, project owner should match project owner
                        return projectOwner.equals(navigator.getProjectOwner());
                    }
                } else {
                    // check if username matches when subgroup projects are not required
                    // project owner is derived from project namespace
                    return projectOwner.equals(navigator.getProjectOwner());
                }
            case UPDATED:
                return navigator.getNavigatorProjects().contains(getPayload().getPathWithNamespace());
            case REMOVED:
                return navigator.getNavigatorProjects().contains(getPayload().getPathWithNamespace());
            default:
                return false;
        }
    }

    @Override
    public boolean isMatch(@NonNull SCMSource source) {
        return source instanceof GitLabSCMSource
                && getPayload().getProjectId().equals(((GitLabSCMSource) source).getProjectId());
    }
}
