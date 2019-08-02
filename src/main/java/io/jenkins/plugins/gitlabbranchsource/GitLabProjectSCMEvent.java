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
        if (projectSystemHookEvent.getEventName().equals("project_create")) {
            return Type.CREATED;
        }
        if (projectSystemHookEvent.getEventName().equals("project_destroy")) {
            return Type.REMOVED;
        }
        return Type.UPDATED;
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
        return "Project event to branch " + getPayload().getPath() + " in namespace " +
                getPayload().getPathWithNamespace();
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
        return navigator instanceof GitLabSCMNavigator && ((GitLabSCMNavigator) navigator).getNavigatorProjects().contains(getPayload().getPathWithNamespace());
    }

    @Override
    public boolean isMatch(@NonNull SCMSource source) {
        // Alternative to check project id as no project id is provided by project event
        return source instanceof GitLabSCMSource && getPayload().getPathWithNamespace().equals(((GitLabSCMSource) source).getProjectPath());
    }

}
