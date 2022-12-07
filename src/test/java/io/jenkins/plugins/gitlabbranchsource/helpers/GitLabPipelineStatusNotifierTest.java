package io.jenkins.plugins.gitlabbranchsource.helpers;

import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Job;
import io.jenkins.plugins.gitlabbranchsource.BranchSCMHead;
import io.jenkins.plugins.gitlabbranchsource.BranchSCMRevision;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMRevision;
import jenkins.plugins.git.GitTagSCMHead;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMRevision;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.models.MergeRequest;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class GitLabPipelineStatusNotifierTest {

    @Test
    public void should_set_branch_status_name() {
        GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, null);

        BranchSCMHead head = new BranchSCMHead("head");
        SCMRevision revision = new BranchSCMRevision(head, "hash");

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, null, revision,
                new hudson.EnvVars());

        assertThat(statusName, is(GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_PREFIX
                + GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_DELIMITER
                + "branch"));
    }

    @Test
    public void should_set_merge_request_head_status_name() {
        GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, null);

        BranchSCMHead targetHead = new BranchSCMHead("target");
        MergeRequestSCMHead head = new MergeRequestSCMHead("head", 0, targetHead, null, null, null, null, null, null);

        BranchSCMRevision target = new BranchSCMRevision(targetHead, "target-hash");
        BranchSCMRevision source = new BranchSCMRevision(new BranchSCMHead("source"), "source-hash");
        SCMRevision revision = new MergeRequestSCMRevision(head, target, source);

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, "head", revision,
                new hudson.EnvVars());

        assertThat(statusName, is(GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_PREFIX
                + GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_DELIMITER
                + "mr-head"));
    }

    @Test
    public void should_set_merge_request_merge_status_name() {
        GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, null);

        BranchSCMHead targetHead = new BranchSCMHead("target");
        MergeRequestSCMHead head = new MergeRequestSCMHead("head", 0, targetHead, null, null, null, null, null, null);

        BranchSCMRevision target = new BranchSCMRevision(targetHead, "target-hash");
        BranchSCMRevision source = new BranchSCMRevision(new BranchSCMHead("source"), "source-hash");
        SCMRevision revision = new MergeRequestSCMRevision(head, target, source);

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, "merge", revision,
                new hudson.EnvVars());

        assertThat(statusName, is(GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_PREFIX
                + GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_DELIMITER
                + "mr-merge"));
    }

    @Test
    public void should_set_tag_status_name() {
        GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, null);

        GitTagSCMHead head = new GitTagSCMHead("tagName", 0);
        SCMRevision revision = new GitTagSCMRevision(head, "tag-hash");

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, null, revision,
                new hudson.EnvVars());

        assertThat(statusName, is(GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_PREFIX
                + GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_DELIMITER
                + "tag"));
    }

    @Test
    public void should_set_branch_ref_name() {
        String branchName = "branch_name";
        BranchSCMHead head = new BranchSCMHead(branchName);
        SCMRevision revision = new BranchSCMRevision(head, "hash");

        String refName = GitLabPipelineStatusNotifier.getRevisionRef(revision);

        assertThat(refName, is(branchName));
    }

    @Test
    public void should_set_merge_request_ref_name() {
        String sourceBranchName = "sourceBranchName";
        String targetBranchName = "targetBranchName";

        BranchSCMHead targetHead = new BranchSCMHead(targetBranchName);
        MergeRequestSCMHead head = new MergeRequestSCMHead("MR-123", 0, targetHead, null, null, null, null, null, null);

        BranchSCMRevision target = new BranchSCMRevision(targetHead, "target-hash");
        BranchSCMRevision source = new BranchSCMRevision(new BranchSCMHead(sourceBranchName), "source-hash");
        SCMRevision revision = new MergeRequestSCMRevision(head, target, source);

        String refName = GitLabPipelineStatusNotifier.getRevisionRef(revision);

        assertThat(refName, is(sourceBranchName));
    }

    @Test
    public void should_set_tag_ref_name() {
        String tagName = "tagName";
        GitTagSCMHead head = new GitTagSCMHead(tagName, 0);
        SCMRevision revision = new GitTagSCMRevision(head, "tag-hash");

        String refName = GitLabPipelineStatusNotifier.getRevisionRef(revision);

        assertThat(refName, is(tagName));
    }

    @Test
    public void should_get_mr_project_id() throws Exception {
        String projectPath = "project_path";
        Long projectId = Long.valueOf(100);

        ItemGroup<?> parent = Mockito.mock(ItemGroup.class);
        Mockito.when(parent.getFullName()).thenReturn("folder/project");

        Job<?, ?> job = new FreeStyleProject(parent, "MR-123");

        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        MergeRequestApi mrApi = Mockito.mock(MergeRequestApi.class);
        MergeRequest mr = Mockito.mock(MergeRequest.class);

        Mockito.when(gitLabApi.getMergeRequestApi()).thenReturn(mrApi);
        Mockito.when(mrApi.getMergeRequest(any(), eq(Long.valueOf(123)))).thenReturn(mr);
        Mockito.when(mr.getSourceProjectId()).thenReturn(projectId);

        Long sourceProjectId = GitLabPipelineStatusNotifier.getSourceProjectId(job, gitLabApi, projectPath);

        assertThat(sourceProjectId, is(projectId));
    }

    @Test
    public void should_get_mr_project_id_projects_using_both_strategy_head() throws Exception {
        String projectPath = "project_path";
        Long projectId = Long.valueOf(100);

        ItemGroup<?> parent = Mockito.mock(ItemGroup.class);
        Mockito.when(parent.getFullName()).thenReturn("folder/project");

        Job<?, ?> job = new FreeStyleProject(parent, "MR-123-head");

        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        MergeRequestApi mrApi = Mockito.mock(MergeRequestApi.class);
        MergeRequest mr = Mockito.mock(MergeRequest.class);

        Mockito.when(gitLabApi.getMergeRequestApi()).thenReturn(mrApi);
        Mockito.when(mrApi.getMergeRequest(any(), eq(Long.valueOf(123)))).thenReturn(mr);
        Mockito.when(mr.getSourceProjectId()).thenReturn(projectId);

        Long sourceProjectId = GitLabPipelineStatusNotifier.getSourceProjectId(job, gitLabApi, projectPath);

        assertThat(sourceProjectId, is(projectId));
    }

    @Test
    public void should_get_mr_project_id_projects_using_both_strategy_merge() throws Exception {
        String projectPath = "project_path";
        Long projectId = Long.valueOf(100);

        ItemGroup<?> parent = Mockito.mock(ItemGroup.class);
        Mockito.when(parent.getFullName()).thenReturn("folder/project");

        Job<?, ?> job = new FreeStyleProject(parent, "MR-123-merge");

        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        MergeRequestApi mrApi = Mockito.mock(MergeRequestApi.class);
        MergeRequest mr = Mockito.mock(MergeRequest.class);

        Mockito.when(gitLabApi.getMergeRequestApi()).thenReturn(mrApi);
        Mockito.when(mrApi.getMergeRequest(any(), eq(Long.valueOf(123)))).thenReturn(mr);
        Mockito.when(mr.getSourceProjectId()).thenReturn(projectId);

        Long sourceProjectId = GitLabPipelineStatusNotifier.getSourceProjectId(job, gitLabApi, projectPath);

        assertThat(sourceProjectId, is(projectId));
    }

}
