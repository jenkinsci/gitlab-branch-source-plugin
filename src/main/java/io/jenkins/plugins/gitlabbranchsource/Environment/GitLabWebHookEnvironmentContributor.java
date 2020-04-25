package io.jenkins.plugins.gitlabbranchsource.Environment;

import hudson.EnvVars;
import hudson.Extension;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.gitlabbranchsource.GitLabWebHookCause;
import javax.annotation.Nonnull;

@Extension
public class GitLabWebHookEnvironmentContributor extends EnvironmentContributor {

    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) {
        GitLabWebHookCause cause = null;
        if (r instanceof MatrixRun) {
            MatrixBuild parent = ((MatrixRun)r).getParentBuild();
            if (parent != null) {
                cause = (GitLabWebHookCause) parent.getCause(GitLabWebHookCause.class);
            }
        } else {
            cause = (GitLabWebHookCause) r.getCause(GitLabWebHookCause.class);
        }
        envs.override("OBJECT_KIND", "none");
        if (cause != null) {
            if(cause.getGitLabPushCauseData() != null) {
                envs.overrideAll(cause.getGitLabPushCauseData().getBuildVariables());
            } else if(cause.getGitLabMergeRequestCauseData() != null) {
                envs.overrideAll(cause.getGitLabMergeRequestCauseData().getBuildVariables());
            } else if(cause.getGitLabTagPushCauseData() != null) {
                envs.overrideAll(cause.getGitLabTagPushCauseData().getBuildVariables());
            }
        }
    }
}
