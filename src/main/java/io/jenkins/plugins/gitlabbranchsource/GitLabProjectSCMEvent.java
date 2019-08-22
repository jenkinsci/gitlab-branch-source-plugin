package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabOwner;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabUser;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceEvent;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.GroupProjectsFilter;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
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
            default: throw new IllegalArgumentException("cannot handle system-hook " + projectSystemHookEvent);
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
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    private boolean isMatch(@Nonnull GitLabSCMNavigator navigator) {
        switch (getType()) {
            case CREATED:
                String projectPathWithNamespace = getPayload().getPathWithNamespace();
                GitLabApi gitLabApi = null;
                try {
                    gitLabApi = GitLabHelper.apiBuilder(navigator.getServerName());
                    GitLabOwner gitlabOwner = GitLabOwner.fetchOwner(gitLabApi, navigator.getProjectOwner());
                    GitLabSCMNavigatorContext navigatorContext = new GitLabSCMNavigatorContext().withTraits(navigator.getTraits());
                    List<Project> projects;
                    // Cannpt check the navigator projects because there could be empty projects/empty groups/new group or subgroups altogether
                    if(gitlabOwner instanceof GitLabUser) {
                        // Even returns the group projects owned by the user
                        if(navigatorContext.wantSubgroupProjects()) {
                            projects = gitLabApi.getProjectApi().getOwnedProjects();
                        } else {
                            projects = gitLabApi.getProjectApi().getUserProjects(navigator.getProjectOwner(), new ProjectFilter().withOwned(true));
                        }
                    } else {
                        GroupProjectsFilter groupProjectsFilter = new GroupProjectsFilter();
                        groupProjectsFilter.withIncludeSubGroups(navigatorContext.wantSubgroupProjects());
                        // If projectOwner is a subgroup, it will only return projects in the subgroup
                        projects = gitLabApi.getGroupApi().getProjects(navigator.getProjectOwner(), groupProjectsFilter);
                    }
                    for(Project project : projects) {
                        if(project.getPathWithNamespace().equals(projectPathWithNamespace)) {
                            return true;
                        }
                    }
                    return false;
                } catch (NoSuchFieldException | GitLabApiException e) {
                    e.printStackTrace();
                }
                break;
            case UPDATED:
                if(navigator.getNavigatorProjects().contains(getPayload().getPathWithNamespace())) {
                    // TODO: Not sure if this is the way to do it, need to check
                    navigator.getNavigatorProjects().remove(getPayload().getPathWithNamespace());
                    return true;
                }
                break;
            case REMOVED:
                if(navigator.getNavigatorProjects().contains(getPayload().getPathWithNamespace())) {
                    navigator.getNavigatorProjects().remove(getPayload().getPathWithNamespace());
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    @Override
    public boolean isMatch(@NonNull SCMSource source) {
        return source instanceof GitLabSCMSource && getPayload().getProjectId().equals(((GitLabSCMSource) source).getProjectId());
    }

}
