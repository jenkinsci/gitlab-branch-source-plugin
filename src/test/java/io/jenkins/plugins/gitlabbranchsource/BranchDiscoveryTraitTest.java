package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import hudson.model.TaskListener;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceRequest;
import org.gitlab4j.api.models.MergeRequest;
import org.junit.Test;

public class BranchDiscoveryTraitTest {
    private GitLabSCMSourceContext context(int strategy, String regex) {
        BranchDiscoveryTrait trait = new BranchDiscoveryTrait(strategy);
        trait.setBranchesAlwaysIncludedRegex(regex);
        GitLabSCMSourceContext context = new GitLabSCMSourceContext(null, SCMHeadObserver.collect());
        trait.decorateContext(context);
        return context;
    }

    private GitLabSCMSourceRequest request(GitLabSCMSourceContext context, MergeRequest... mergeRequests) {
        GitLabSCMSourceRequest request = context.newRequest(mock(SCMSource.class), TaskListener.NULL);
        request.setMergeRequests(List.of(mergeRequests));
        return request;
    }

    private MergeRequest mr(long sourceProject, String branch) {
        MergeRequest request = new MergeRequest();
        request.setSourceProjectId(sourceProject);
        request.setTargetProjectId(1L);
        request.setSourceBranch(branch);
        return request;
    }

    @Test
    public void explicitBranchesUseFullCaseSensitiveRegexMatches() throws Exception {
        GitLabSCMSourceContext context = context(4, " main|release/.* ");
        assertTrue(context.wantBranches());
        assertFalse(context.wantMRs());
        assertEquals(1, context.filters().size());
        assertInstanceOf(
                BranchDiscoveryTrait.OnlyExplicitlyListedBranchesSCMHeadFilter.class,
                context.filters().get(0));
        try (GitLabSCMSourceRequest request = request(context)) {
            assertFalse(request.isExcluded(new BranchSCMHead("main")));
            assertFalse(request.isExcluded(new BranchSCMHead("release/1.0")));
            assertTrue(request.isExcluded(new BranchSCMHead("feature/main")));
            assertTrue(request.isExcluded(new BranchSCMHead("MAIN")));
            assertTrue(request.isExcluded(new BranchSCMHead("feature")));
        }
    }

    @Test
    public void missingOrBlankRegexExcludesAllExplicitBranches() throws Exception {
        for (String regex : new String[] {null, "", "  "}) {
            try (GitLabSCMSourceRequest request = request(context(4, regex))) {
                assertTrue(request.isExcluded(new BranchSCMHead("main")));
            }
        }
    }

    @Test
    public void regexChangesInvalidateCompiledPattern() {
        BranchDiscoveryTrait trait = new BranchDiscoveryTrait(4);
        trait.setBranchesAlwaysIncludedRegex("main");
        assertTrue(trait.getBranchesAlwaysIncludedRegexPattern().matcher("main").matches());
        trait.setBranchesAlwaysIncludedRegex("release/.*");
        assertFalse(
                trait.getBranchesAlwaysIncludedRegexPattern().matcher("main").matches());
        assertTrue(trait.getBranchesAlwaysIncludedRegexPattern()
                .matcher("release/1")
                .matches());
        trait.setBranchesAlwaysIncludedRegex("");
        assertNull(trait.getBranchesAlwaysIncludedRegex());
        assertNull(trait.getBranchesAlwaysIncludedRegexPattern());
        trait.setBranchesAlwaysIncludedRegex("[");
        assertThrows(PatternSyntaxException.class, trait::getBranchesAlwaysIncludedRegexPattern);
        trait.setBranchesAlwaysIncludedRegex("main");
        assertNotNull(trait.getBranchesAlwaysIncludedRegexPattern());
        trait.setBranchesAlwaysIncludedRegex(null);
        assertNull(trait.getBranchesAlwaysIncludedRegexPattern());
    }

    @Test
    public void branchFiltersPreserveSeparateMergeRequestAndTagDiscovery() throws Exception {
        for (int strategy : new int[] {1, 2, 3, 4}) {
            GitLabSCMSourceContext context = context(strategy, null);
            new OriginMergeRequestDiscoveryTrait(1).decorateContext(context);
            new TagDiscoveryTrait().decorateContext(context);
            assertTrue(context.wantOriginMRs());
            assertTrue(context.wantTags());
            try (GitLabSCMSourceRequest request = request(context, mr(1L, "feature"))) {
                assertTrue(request.isFetchOriginMRs());
                assertTrue(request.isFetchTags());
                assertFalse(request.isExcluded(new GitLabTagSCMHead("v1", 0)));
                assertFalse(request.isExcluded(new MergeRequestSCMHead(
                        "MR-1",
                        1L,
                        new BranchSCMHead("main"),
                        ChangeRequestCheckoutStrategy.MERGE,
                        SCMHeadOrigin.DEFAULT,
                        "owner",
                        "owner/project",
                        "feature",
                        "Title")));
            }
        }
    }

    @Test
    public void excludeOriginMergeRequestsIgnoresForksAndOtherBranches() throws Exception {
        GitLabSCMSourceContext context = context(1, null);
        assertTrue(context.wantOriginMRs());
        try (GitLabSCMSourceRequest request = request(context, mr(1L, "feature"), mr(2L, "fork"))) {
            assertTrue(request.isExcluded(new BranchSCMHead("FEATURE")));
            assertFalse(request.isExcluded(new BranchSCMHead("fork")));
            assertFalse(request.isExcluded(new BranchSCMHead("main")));
        }
        try (GitLabSCMSourceRequest request = request(context)) {
            assertFalse(request.isExcluded(new BranchSCMHead("main")));
        }
    }

    @Test
    public void onlyOriginMergeRequestsPreservesLegacyFiltering() throws Exception {
        GitLabSCMSourceContext context = context(2, null);
        assertTrue(context.wantOriginMRs());
        // Strategy 2 preserves legacy behavior: any different origin MR excludes the branch.
        for (List<MergeRequest> mrs : List.of(
                List.of(mr(1L, "other"), mr(1L, "feature"), mr(2L, "fork")),
                List.of(mr(1L, "feature"), mr(1L, "other"), mr(2L, "fork")))) {
            try (GitLabSCMSourceRequest request = request(context, mrs.toArray(new MergeRequest[0]))) {
                assertTrue(request.isExcluded(new BranchSCMHead("FEATURE")));
                assertTrue(request.isExcluded(new BranchSCMHead("main")));
                assertTrue(request.isExcluded(new BranchSCMHead("fork")));
            }
        }
        try (GitLabSCMSourceRequest request = request(context, mr(1L, "feature"))) {
            assertFalse(request.isExcluded(new BranchSCMHead("FEATURE")));
            assertTrue(request.isExcluded(new BranchSCMHead("main")));
        }
        try (GitLabSCMSourceRequest request = request(context)) {
            assertFalse(request.isExcluded(new BranchSCMHead("main")));
        }
        try (GitLabSCMSourceRequest request = request(context, mr(2L, "fork"))) {
            assertFalse(request.isExcluded(new BranchSCMHead("main")));
        }
    }

    @Test
    public void regexOverridesBothMergeRequestFilters() throws Exception {
        for (int strategy : new int[] {1, 2}) {
            try (GitLabSCMSourceRequest request = request(context(strategy, "main"), mr(1L, "main"), mr(1L, "other"))) {
                assertFalse(request.isExcluded(new BranchSCMHead("main")));
            }
            try (GitLabSCMSourceRequest request = request(context(strategy, "main"))) {
                assertFalse(request.isExcluded(new BranchSCMHead("main")));
            }
        }
    }

    @Test
    public void existingFiltersIgnoreOtherSourceRequestTypes() throws Exception {
        SCMSourceRequest request = mock(SCMSourceRequest.class);
        for (SCMHeadFilter filter : List.of(
                new BranchDiscoveryTrait.ExcludeOriginMRBranchesSCMHeadFilter(null),
                new BranchDiscoveryTrait.OnlyOriginMRBranchesSCMHeadFilter(null))) {
            assertFalse(filter.isExcluded(request, new BranchSCMHead("main")));
        }
    }

    @Test
    public void existingStrategyIdsAndLegacyBitFlagsArePreserved() throws Exception {
        assertEquals(
                List.of("1", "2", "4", "3"),
                new BranchDiscoveryTrait.DescriptorImpl()
                        .doFillStrategyIdItems().stream()
                                .map(option -> option.value)
                                .toList());
        for (int strategy = 0; strategy <= 4; strategy++) {
            BranchDiscoveryTrait trait = new BranchDiscoveryTrait(strategy);
            assertEquals(strategy, trait.getStrategyId());
            assertEquals((strategy & 1) != 0, trait.isBuildBranch());
            assertEquals((strategy & 2) != 0, trait.isBuildBranchesWithMR());
        }
        assertEquals(0, new BranchDiscoveryTrait(false, false).getStrategyId());
        assertEquals(1, new BranchDiscoveryTrait(true, false).getStrategyId());
        assertEquals(2, new BranchDiscoveryTrait(false, true).getStrategyId());
        assertEquals(3, new BranchDiscoveryTrait(true, true).getStrategyId());
        for (int strategy : new int[] {0, 3}) {
            GitLabSCMSourceContext context = context(strategy, "main");
            assertFalse(context.wantMRs());
            assertTrue(context.filters().isEmpty());
            try (GitLabSCMSourceRequest request = request(context)) {
                assertFalse(request.isExcluded(new BranchSCMHead("feature")));
            }
        }
    }
}
