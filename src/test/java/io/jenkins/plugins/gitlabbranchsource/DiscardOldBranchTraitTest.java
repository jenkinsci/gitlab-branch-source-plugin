package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jenkins.plugins.gitlabbranchsource.DiscardOldBranchTrait.ExcludeOldSCMHeadBranch;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.trait.SCMHeadFilter;
import org.apache.commons.lang.time.DateUtils;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.junit.jupiter.api.Test;

class DiscardOldBranchTraitTest {

    @Test
    void should_include_branch_if_last_commit_within_range() throws Exception {
        DiscardOldBranchTrait uut = new DiscardOldBranchTrait(10);
        GitLabSCMSourceContext context = new GitLabSCMSourceContext(null, SCMHeadObserver.none());
        uut.decorateContext(context);

        Optional<SCMHeadFilter> optFilter = context.filters().stream()
                .filter(it -> ExcludeOldSCMHeadBranch.class.equals(it.getClass()))
                .findFirst();
        assertTrue(optFilter.isPresent());

        SCMHead head = mock(SCMHead.class);
        when(head.getName()).thenReturn("expected");

        Date today = new Date();

        GitLabSCMSourceRequest request = mock(GitLabSCMSourceRequest.class);
        when(request.getBranches())
                .thenReturn(List.of(
                        buildBranch("other", DateUtils.addDays(today, -7)),
                        buildBranch("expected", DateUtils.addDays(today, -10))));

        SCMHeadFilter filter = optFilter.get();
        assertFalse(filter.isExcluded(request, head));
    }

    @Test
    void should_exclude_branch_if_last_commit_not_within_range() throws Exception {
        DiscardOldBranchTrait uut = new DiscardOldBranchTrait(10);
        GitLabSCMSourceContext context = new GitLabSCMSourceContext(null, SCMHeadObserver.none());
        uut.decorateContext(context);

        Optional<SCMHeadFilter> optFilter = context.filters().stream()
                .filter(it -> ExcludeOldSCMHeadBranch.class.equals(it.getClass()))
                .findFirst();
        assertTrue(optFilter.isPresent());

        SCMHead head = mock(SCMHead.class);
        when(head.getName()).thenReturn("expected");

        Date today = new Date();

        GitLabSCMSourceRequest request = mock(GitLabSCMSourceRequest.class);
        when(request.getBranches())
                .thenReturn(List.of(
                        buildBranch("other", DateUtils.addDays(today, -7)),
                        buildBranch("expected", DateUtils.addDays(today, -11))));

        SCMHeadFilter filter = optFilter.get();
        assertTrue(filter.isExcluded(request, head));
    }

    private Branch buildBranch(String name, Date commitDate) {
        Branch branch = new Branch();
        branch.setName(name);
        Commit commit = new Commit();
        commit.setCommittedDate(commitDate);
        branch.setCommit(commit);
        return branch;
    }
}
