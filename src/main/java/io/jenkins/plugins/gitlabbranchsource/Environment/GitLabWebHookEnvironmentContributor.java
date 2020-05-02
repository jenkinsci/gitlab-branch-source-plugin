package io.jenkins.plugins.gitlabbranchsource.Environment;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.gitlabbranchsource.GitLabWebHookCause;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

@Extension
public class GitLabWebHookEnvironmentContributor extends EnvironmentContributor {

    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) {
        GitLabWebHookCause gitLabWebHookCause = null;
        if (r instanceof WorkflowRun) {
            gitLabWebHookCause = (GitLabWebHookCause) r.getCause(GitLabWebHookCause.class);
        }
        envs.override("GITLAB_OBJECT_KIND", "none");
        if (gitLabWebHookCause != null) {
            if(gitLabWebHookCause.getGitLabPushCauseData() != null) {
                envs.overrideAll(gitLabWebHookCause.getGitLabPushCauseData().getBuildVariables());
            } else if(gitLabWebHookCause.getGitLabMergeRequestCauseData() != null) {
                envs.overrideAll(gitLabWebHookCause.getGitLabMergeRequestCauseData().getBuildVariables());
            } else if(gitLabWebHookCause.getGitLabTagPushCauseData() != null) {
                envs.overrideAll(gitLabWebHookCause.getGitLabTagPushCauseData().getBuildVariables());
            }
        }
    }
}
