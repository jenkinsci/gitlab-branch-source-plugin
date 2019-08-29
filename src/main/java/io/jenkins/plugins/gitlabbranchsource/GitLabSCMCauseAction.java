package io.jenkins.plugins.gitlabbranchsource;

import hudson.model.Cause;
import hudson.model.CauseAction;

public class GitLabSCMCauseAction extends CauseAction {

    public GitLabSCMCauseAction(Cause... causes) {
        super(causes);
    }

    public String getDescription() {
        GitLabWebHookCause cause = findCause(GitLabWebHookCause.class);
        return (cause != null) ? cause.getShortDescription() : null;
    }

}
