package io.jenkins.plugins.gitlabbranchsource;


import java.util.logging.Logger;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMSourceEvent;
import org.gitlab4j.api.systemhooks.GroupSystemHookEvent;
import org.gitlab4j.api.systemhooks.ProjectSystemHookEvent;
import org.gitlab4j.api.systemhooks.SystemHookListener;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;

public class GitLabHookListener implements WebHookListener, SystemHookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabHookListener.class.getName());

    private String origin;

    public GitLabHookListener(String origin) {
        this.origin = origin;
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent event) {
        LOGGER.info("MR EVENT");
        LOGGER.info(event.toString());
        GitLabMergeRequestSCMEvent trigger = new GitLabMergeRequestSCMEvent(event, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.info("PUSH EVENT");
        LOGGER.info(pushEvent.toString());
        GitLabPushSCMEvent trigger = new GitLabPushSCMEvent(pushEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.info("TAG EVENT");
        LOGGER.info(tagPushEvent.toString());
        GitLabTagPushSCMEvent trigger = new GitLabTagPushSCMEvent(tagPushEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onProjectEvent(ProjectSystemHookEvent projectSystemHookEvent) {
        LOGGER.info("PROJECT EVENT");
        LOGGER.info(projectSystemHookEvent.toString());
        // TODO: implement handling `project_transfer` and `project_renamed`
        if(!projectSystemHookEvent.getEventName().equals("project_transfer") && !projectSystemHookEvent.getEventName().equals("project_renamed")) {
            GitLabProjectSCMEvent trigger = new GitLabProjectSCMEvent(projectSystemHookEvent, origin);
            SCMSourceEvent.fireNow(trigger);
        }
    }

    @Override
    public void onGroupEvent(GroupSystemHookEvent groupSystemHookEvent) {
        LOGGER.info("GROUP EVENT");
        LOGGER.info(groupSystemHookEvent.toString());
    }
}
