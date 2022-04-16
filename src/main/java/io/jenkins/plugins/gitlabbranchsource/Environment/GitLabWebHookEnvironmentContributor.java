package io.jenkins.plugins.gitlabbranchsource.Environment;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.gitlabbranchsource.GitLabMergeRequestCommentCause;
import io.jenkins.plugins.gitlabbranchsource.GitLabWebHookCause;
import java.util.Map;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

@Extension
public class GitLabWebHookEnvironmentContributor extends EnvironmentContributor {

    @Override
    public void buildEnvironmentFor(@NonNull Run r, @NonNull EnvVars envs, @NonNull TaskListener listener) {
        GitLabWebHookCause gitLabWebHookCause = null;
        GitLabMergeRequestCommentCause gitLabMergeRequestCommentCause = null;

        if (r instanceof WorkflowRun) {
            gitLabWebHookCause = (GitLabWebHookCause) r.getCause(GitLabWebHookCause.class);
            gitLabMergeRequestCommentCause = (GitLabMergeRequestCommentCause)
                r.getCause(GitLabMergeRequestCommentCause.class);
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

        // There is GitLabMergeRequestCommentCause so we have to do extra check
        // for processing these variables. If someone wants to refactor it - look at
        // inheritance hierarchy of GitLabWebHookCause and GitLabMergeRequestCommentCause
        // TODO combine GitLabWebHookCause and GitLabMergeRequestCommentCause to refactor this class properly
        if (gitLabMergeRequestCommentCause != null) {
            if (gitLabMergeRequestCommentCause.getGitLabMergeRequestNoteData() != null) {
                Map<String, String> buildVariables = gitLabMergeRequestCommentCause
                    .getGitLabMergeRequestNoteData().getBuildVariables();
                envs.overrideAll(buildVariables);
            }
        }
    }
}
