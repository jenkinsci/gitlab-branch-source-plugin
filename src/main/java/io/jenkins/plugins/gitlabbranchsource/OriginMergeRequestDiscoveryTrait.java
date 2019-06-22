package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;
import jenkins.scm.api.trait.SCMHeadAuthority;
import jenkins.scm.api.trait.SCMHeadAuthorityDescriptor;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.ChangeRequestSCMHeadCategory;
import jenkins.scm.impl.trait.Discovery;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.EnumSet;
import java.util.Set;

/**
 * A {@link Discovery} trait for GitLab that will discover merge requests originating from a branch in the repository
 * itself.
 */
public class OriginMergeRequestDiscoveryTrait extends SCMSourceTrait {
    /**
     * The strategy encoded as a bit-field.
     */
    private int strategyId;

    /**
     * Constructor for stapler.
     *
     * @param strategyId the strategy id.
     */
    @DataBoundConstructor
    public OriginMergeRequestDiscoveryTrait(int strategyId) {
        this.strategyId = strategyId;
    }

    /**
     * Constructor for programmatic instantiation.
     *
     * @param strategies the {@link ChangeRequestCheckoutStrategy} instances.
     */
    public OriginMergeRequestDiscoveryTrait(Set<ChangeRequestCheckoutStrategy> strategies) {
        this((strategies.contains(ChangeRequestCheckoutStrategy.MERGE) ? 1 : 0)
                + (strategies.contains(ChangeRequestCheckoutStrategy.HEAD) ? 2 : 0));
    }

    /**
     * Gets the strategy id.
     *
     * @return the strategy id.
     */
    public int getStrategyId() {
        return strategyId;
    }

    /**
     * Returns the strategies.
     *
     * @return the strategies.
     */
    @NonNull
    public Set<ChangeRequestCheckoutStrategy> getStrategies() {
        switch (strategyId) {
            case 1:
                return EnumSet.of(ChangeRequestCheckoutStrategy.MERGE);
            case 2:
                return EnumSet.of(ChangeRequestCheckoutStrategy.HEAD);
            case 3:
                return EnumSet.of(ChangeRequestCheckoutStrategy.HEAD, ChangeRequestCheckoutStrategy.MERGE);
            default:
                return EnumSet.noneOf(ChangeRequestCheckoutStrategy.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
        ctx.wantOriginPRs(true);
        ctx.withAuthority(new OriginChangeRequestSCMHeadAuthority());
        ctx.withOriginPRStrategies(getStrategies());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeCategory(@NonNull SCMHeadCategory category) {
        return category instanceof ChangeRequestSCMHeadCategory;
    }

    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "Discover merge requests from origin";
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
            result.add(Messages.ForkMergeRequestDiscoveryTrait_mergeOnly(), "1");
            result.add(Messages.ForkMergeRequestDiscoveryTrait_headOnly(), "2");
            result.add(Messages.ForkMergeRequestDiscoveryTrait_headAndMerge(), "3");
            return result;
        }
    }

    /**
     * A {@link SCMHeadAuthority} that trusts origin merge requests
     */
    public static class OriginChangeRequestSCMHeadAuthority
            extends SCMHeadAuthority<SCMSourceRequest, ChangeRequestSCMHead2, SCMRevision> {
        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull SCMSourceRequest request, @NonNull ChangeRequestSCMHead2 head) {
            return SCMHeadOrigin.DEFAULT.equals(head.getOrigin());
        }

        /**
         * Our descriptor.
         */
        @Extension
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Default.class.isAssignableFrom(originClass);
            }            /**
             * {@inheritDoc}
             */
            @Override
            public String getDisplayName() {
                return Messages.OriginMergeRequestDiscoveryTrait_authorityDisplayName();
            }


        }
    }

}
