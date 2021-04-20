package io.jenkins.plugins.gitlabbranchsource.helpers;

import io.jenkins.plugins.gitlabbranchsource.BranchSCMHead;
import io.jenkins.plugins.gitlabbranchsource.BranchSCMRevision;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMRevision;
import jenkins.plugins.git.GitTagSCMHead;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMRevision;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GitLabPipelineStatusNotifierTest {

    @Test
    public void should_set_branch_status_name() {
        GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, null);

        BranchSCMHead head = new BranchSCMHead("head");
        SCMRevision revision = new BranchSCMRevision(head, "hash");

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, null, revision);

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

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, "head", revision);

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

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, "merge", revision);

        assertThat(statusName, is(GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_PREFIX
                + GitLabPipelineStatusNotifier.GITLAB_PIPELINE_STATUS_DELIMITER
                + "mr-merge"));
    }

    @Test
    public void should_set_tag_status_name() {
        GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, null);

        GitTagSCMHead head = new GitTagSCMHead("tagName", 0);
        SCMRevision revision = new GitTagSCMRevision(head, "tag-hash");

        String statusName = GitLabPipelineStatusNotifier.getStatusName(sourceContext, null, revision);

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

}
