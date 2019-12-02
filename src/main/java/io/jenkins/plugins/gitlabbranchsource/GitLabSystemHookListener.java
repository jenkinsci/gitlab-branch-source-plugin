package io.jenkins.plugins.gitlabbranchsource;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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
        LOGGER.log(Level.INFO, projectSystemHookEvent.toString());
        // TODO: implement handling `project_transfer` and `project_renamed`
        switch (projectSystemHookEvent.getEventName()) {
            case ProjectSystemHookEvent.PROJECT_CREATE_EVENT:
            case ProjectSystemHookEvent.PROJECT_DESTROY_EVENT:
            case ProjectSystemHookEvent.PROJECT_UPDATE_EVENT:
                GitLabProjectSCMEvent trigger = new GitLabProjectSCMEvent(projectSystemHookEvent,
                    origin);
                SCMSourceEvent.fireLater(trigger, 5, TimeUnit.SECONDS);
                break;
            default:
                LOGGER.log(Level.INFO, String.format("unsupported System hook event: %s", projectSystemHookEvent.getEventName().toString()));
        }
    }

    @Override
    public void onGroupEvent(GroupSystemHookEvent groupSystemHookEvent) {
        LOGGER.log(Level.INFO, groupSystemHookEvent.toString());
    }
}
