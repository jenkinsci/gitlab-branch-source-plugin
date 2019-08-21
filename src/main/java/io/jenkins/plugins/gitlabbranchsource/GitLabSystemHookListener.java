package io.jenkins.plugins.gitlabbranchsource;

import java.util.logging.Logger;
import jenkins.scm.api.SCMSourceEvent;
import org.gitlab4j.api.systemhooks.GroupSystemHookEvent;
import org.gitlab4j.api.systemhooks.ProjectSystemHookEvent;
import org.gitlab4j.api.systemhooks.SystemHookListener;

public class GitLabSystemHookListener implements SystemHookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabSystemHookListener.class.getName());

    private String origin;

    public GitLabSystemHookListener(String origin) {
        this.origin = origin;
    }

    @Override
    public void onProjectEvent(ProjectSystemHookEvent projectSystemHookEvent) {
        LOGGER.info("PROJECT EVENT");
        LOGGER.info(projectSystemHookEvent.toString());
        // TODO: implement handling `project_transfer` and `project_renamed`

        switch (projectSystemHookEvent.getEventName()) {
            case ProjectSystemHookEvent.PROJECT_CREATE_EVENT:
            case ProjectSystemHookEvent.PROJECT_DESTROY_EVENT:
            case ProjectSystemHookEvent.PROJECT_UPDATE_EVENT:
                GitLabProjectSCMEvent trigger = new GitLabProjectSCMEvent(projectSystemHookEvent, origin);
                SCMSourceEvent.fireNow(trigger);
                break;
            default:
                LOGGER.info("unsupported System hook event");
        }
    }

    @Override
    public void onGroupEvent(GroupSystemHookEvent groupSystemHookEvent) {
        LOGGER.info("GROUP EVENT");
        LOGGER.info(groupSystemHookEvent.toString());
    }
}
