package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.util.ListBoxModel;
import java.util.regex.Pattern;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMHeadAuthority;
import jenkins.scm.api.trait.SCMHeadAuthorityDescriptor;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.gitlab4j.api.models.MergeRequest;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * A {@link Discovery} trait for GitLab that will discover branches on the repository.
 */
public class BranchDiscoveryTrait extends SCMSourceTrait {

    /**
     * The strategy encoded as a bit-field.
     */
    private int strategyId;

    /**
     * Regex of branches that should always be included regardless of whether a merge request exists or not.
     */
    private String branchesAlwaysIncludedRegex;

    /**
     * The compiled {@link Pattern} of the branchesAlwaysIncludedRegex.
     */
    @CheckForNull
    private transient Pattern branchesAlwaysIncludedRegexPattern;

    /**
     * Constructor for stapler.
     *
     * @param strategyId the strategy id.
     * @param branchesAlwaysIncludedRegex the branchesAlwaysIncludedRegex.
     */
    @DataBoundConstructor
    public BranchDiscoveryTrait(int strategyId) {
        this.strategyId = strategyId;
    }

    /**
     * Constructor for legacy code.
     *
     * @param buildBranch       build branches that are not filed as a MR.
     * @param buildBranchWithMr build branches that are also MRs.
     */
    public BranchDiscoveryTrait(boolean buildBranch, boolean buildBranchWithMr) {
        this.strategyId = (buildBranch ? 1 : 0) + (buildBranchWithMr ? 2 : 0);
    }

    /**
     * Returns the strategy id.
     *
     * @return the strategy id.
     */
    public int getStrategyId() {
        return strategyId;
    }

    /**
     * Returns the branchesAlwaysIncludedRegex.
     *
     * @return the branchesAlwaysIncludedRegex.
     */
    public String getBranchesAlwaysIncludedRegex() {
        return branchesAlwaysIncludedRegex;
    }

    @DataBoundSetter
    public void setBranchesAlwaysIncludedRegex(@CheckForNull String branchesAlwaysIncludedRegex) {
        this.branchesAlwaysIncludedRegex = Util.fixEmptyAndTrim(branchesAlwaysIncludedRegex);
    }

    /**
     * Returns the compiled {@link Pattern} of the branchesAlwaysIncludedRegex.
     *
     * @return the branchesAlwaysIncludedRegexPattern.
     */
    public Pattern getBranchesAlwaysIncludedRegexPattern() {
        if (branchesAlwaysIncludedRegex != null && branchesAlwaysIncludedRegexPattern == null) {
            branchesAlwaysIncludedRegexPattern = Pattern.compile(branchesAlwaysIncludedRegex);
        }

        return branchesAlwaysIncludedRegexPattern;
    }

    /**
     * Returns {@code true} if building branches that are not filed as a MR.
     *
     * @return {@code true} if building branches that are not filed as a MR.
     */
    @Restricted(NoExternalUse.class)
    public boolean isBuildBranch() {
        return (strategyId & 1) != 0;
    }

    /**
     * Returns {@code true} if building branches that are filed as a MR.
     *
     * @return {@code true} if building branches that are filed as a MR.
     */
    @Restricted(NoExternalUse.class)
    public boolean isBuildBranchesWithMR() {
        return (strategyId & 2) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
        ctx.wantBranches(true);
        ctx.withAuthority(new BranchSCMHeadAuthority());
        switch (strategyId) {
            case 1:
                ctx.wantOriginMRs(true);
                ctx.withFilter(new ExcludeOriginMRBranchesSCMHeadFilter(getBranchesAlwaysIncludedRegexPattern()));
                break;
            case 2:
                ctx.wantOriginMRs(true);
                ctx.withFilter(new OnlyOriginMRBranchesSCMHeadFilter(getBranchesAlwaysIncludedRegexPattern()));
                break;
            case 3:
            default:
                // we don't care if it is a MR or not, we're taking them all, no need to ask for MRs and no need
                // to filter
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeCategory(@NonNull SCMHeadCategory category) {
        return category.isUncategorized();
    }

    /**
     * Our descriptor.
     */
    @Symbol("gitLabBranchDiscovery")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.BranchDiscoveryTrait_displayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitLabSCMSource.class;
        }

        /**
         * Populates the strategy options.
         *
         * @return the stategy options.
         */
        @NonNull
        @Restricted(NoExternalUse.class)
        @SuppressWarnings("unused") // stapler
        public ListBoxModel doFillStrategyIdItems() {
            ListBoxModel result = new ListBoxModel();
            result.add(Messages.BranchDiscoveryTrait_excludeMRs(), "1");
            result.add(Messages.BranchDiscoveryTrait_onlyMRs(), "2");
            result.add(Messages.BranchDiscoveryTrait_allBranches(), "3");
            return result;
        }
    }

    /**
     * Trusts branches from the origin repository.
     */
    public static class BranchSCMHeadAuthority extends SCMHeadAuthority<SCMSourceRequest, BranchSCMHead, SCMRevision> {

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull SCMSourceRequest request, @NonNull BranchSCMHead head) {
            return true;
        }

        /**
         * Out descriptor.
         */
        @Extension
        @Symbol("gitLabBranchHeadAuthority")
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Default.class.isAssignableFrom(originClass);
            }

            /**
             * {@inheritDoc}
             */
            @NonNull
            @Override
            public String getDisplayName() {
                return Messages.BranchDiscoveryTrait_authorityDisplayName();
            }
        }
    }

    /**
     * Filter that excludes branches that are also filed as a merge request.
     */
    public static class ExcludeOriginMRBranchesSCMHeadFilter extends SCMHeadFilter {

        /**
         * The compiled {@link Pattern} of the branchesAlwaysIncludedRegex.
         */
        private final Pattern branchesAlwaysIncludedRegexPattern;

        /**
         * Constructor
         *
         * @param branchesAlwaysIncludedRegexPattern the branchesAlwaysIncludedRegexPattern.
         */
        public ExcludeOriginMRBranchesSCMHeadFilter(Pattern branchesAlwaysIncludedRegexPattern) {
            this.branchesAlwaysIncludedRegexPattern = branchesAlwaysIncludedRegexPattern;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head) {
            if (head instanceof BranchSCMHead && request instanceof GitLabSCMSourceRequest) {
                if (branchesAlwaysIncludedRegexPattern != null
                        && branchesAlwaysIncludedRegexPattern
                                .matcher(head.getName())
                                .matches()) {
                    return false;
                }

                for (MergeRequest m : ((GitLabSCMSourceRequest) request).getMergeRequests()) {
                    // only match if the merge request is an origin merge request
                    if (m.getSourceProjectId().equals(m.getTargetProjectId())
                            && m.getSourceBranch().equalsIgnoreCase(head.getName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Filter that excludes branches that are not also filed as a merge request.
     */
    public static class OnlyOriginMRBranchesSCMHeadFilter extends SCMHeadFilter {

        /**
         * The compiled {@link Pattern} of the branchesAlwaysIncludedRegex.
         */
        private final Pattern branchesAlwaysIncludedRegexPattern;

        /**
         * Constructor
         *
         * @param branchesAlwaysIncludedRegexPattern the branchesAlwaysIncludedRegexPattern.
         */
        public OnlyOriginMRBranchesSCMHeadFilter(Pattern branchesAlwaysIncludedRegexPattern) {
            this.branchesAlwaysIncludedRegexPattern = branchesAlwaysIncludedRegexPattern;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head) {
            if (head instanceof BranchSCMHead && request instanceof GitLabSCMSourceRequest) {
                if (branchesAlwaysIncludedRegexPattern != null
                        && branchesAlwaysIncludedRegexPattern
                                .matcher(head.getName())
                                .matches()) {
                    return false;
                }

                for (MergeRequest m : ((GitLabSCMSourceRequest) request).getMergeRequests()) {
                    if (m.getSourceProjectId().equals(m.getTargetProjectId())
                            && !m.getSourceBranch().equalsIgnoreCase(head.getName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
