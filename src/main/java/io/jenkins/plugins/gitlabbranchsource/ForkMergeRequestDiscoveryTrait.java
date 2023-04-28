package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
import org.gitlab4j.api.models.AccessLevel;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * A {@link Discovery} trait for GitLab that will discover merge requests from
 * forks of the
 * project.
 */
public class ForkMergeRequestDiscoveryTrait extends SCMSourceTrait {

    /**
     * The strategy encoded as a bit-field.
     */
    private final int strategyId;
    /**
     * The authority.
     */
    @NonNull
    private final SCMHeadAuthority<
                    ? super GitLabSCMSourceRequest, ? extends ChangeRequestSCMHead2, ? extends SCMRevision>
            trust;
    /**
     * Build MR for Forks that are not Mirrors
     */
    private boolean buildMRForksNotMirror = false;

    /**
     * Constructor for stapler.
     *
     * @param strategyId            the strategy id.
     * @param trust                 the authority to use.
     * @param buildMRForksNotMirror the buildMRForksNotMirror flag
     */
    @DataBoundConstructor
    public ForkMergeRequestDiscoveryTrait(
            int strategyId,
            @NonNull
                    SCMHeadAuthority<
                                    ? super GitLabSCMSourceRequest,
                                    ? extends ChangeRequestSCMHead2,
                                    ? extends SCMRevision>
                            trust,
            boolean buildMRForksNotMirror) {
        this.strategyId = strategyId;
        this.trust = trust;
        this.buildMRForksNotMirror = buildMRForksNotMirror;
    }

    /**
     * Constructor for programmatic instantiation.
     *
     * @param strategies            the {@link ChangeRequestCheckoutStrategy}
     *                              instances.
     * @param trust                 the authority.
     * @param buildMRForksNotMirror the buildMRForksNotMirror flag
     */
    public ForkMergeRequestDiscoveryTrait(
            @NonNull Set<ChangeRequestCheckoutStrategy> strategies,
            @NonNull
                    SCMHeadAuthority<
                                    ? super GitLabSCMSourceRequest,
                                    ? extends ChangeRequestSCMHead2,
                                    ? extends SCMRevision>
                            trust,
            boolean buildMRForksNotMirror) {
        this(
                (strategies.contains(ChangeRequestCheckoutStrategy.MERGE) ? 1 : 0)
                        + (strategies.contains(ChangeRequestCheckoutStrategy.HEAD) ? 2 : 0),
                trust,
                buildMRForksNotMirror);
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
     * Gets the authority.
     *
     * @return the authority.
     */
    @NonNull
    public SCMHeadAuthority<? super GitLabSCMSourceRequest, ? extends ChangeRequestSCMHead2, ? extends SCMRevision>
            getTrust() {
        return trust;
    }

    /**
     * Gets the buildMRForksNotMirror
     *
     * @return true to build MR for Forks that are not Mirror
     */
    public boolean getBuildMRForksNotMirror() {
        return buildMRForksNotMirror;
    }

    /**
     * Setter for stapler to set the buildMRForksNotMirror
     */
    @DataBoundSetter
    public void setBuildMRForksNotMirror(boolean buildMRForksNotMirror) {
        this.buildMRForksNotMirror = buildMRForksNotMirror;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
        ctx.wantForkMRs(true);
        ctx.withAuthority(trust);
        ctx.withForkMRStrategies(getStrategies());
        ctx.withBuildMRForksNotMirror(getBuildMRForksNotMirror());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeCategory(@NonNull SCMHeadCategory category) {
        return category instanceof ChangeRequestSCMHeadCategory;
    }

    /**
     * Our descriptor.
     */
    @Symbol("gitLabForkDiscovery")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.ForkMergeRequestDiscoveryTrait_displayName();
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

        /**
         * Returns the list of appropriate {@link SCMHeadAuthorityDescriptor} instances.
         *
         * @return the list of appropriate {@link SCMHeadAuthorityDescriptor} instances.
         */
        @NonNull
        @SuppressWarnings("unused") // stapler
        public List<SCMHeadAuthorityDescriptor> getTrustDescriptors() {
            return SCMHeadAuthority._for(
                    GitLabSCMSourceRequest.class,
                    MergeRequestSCMHead.class,
                    MergeRequestSCMRevision.class,
                    SCMHeadOrigin.Fork.class);
        }

        /**
         * Returns the default trust for new instances of
         * {@link ForkMergeRequestDiscoveryTrait}.
         *
         * @return the default trust for new instances of
         *         {@link ForkMergeRequestDiscoveryTrait}.
         */
        @NonNull
        @SuppressWarnings("unused") // stapler
        public SCMHeadAuthority<?, ?, ?> getDefaultTrust() {
            return new TrustPermission();
        }
    }

    /**
     * An {@link SCMHeadAuthority} that trusts nothing.
     */
    public static class TrustNobody extends SCMHeadAuthority<SCMSourceRequest, ChangeRequestSCMHead2, SCMRevision> {

        /**
         * Constructor.
         */
        @DataBoundConstructor
        public TrustNobody() {}

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean checkTrusted(@NonNull SCMSourceRequest request, @NonNull ChangeRequestSCMHead2 head) {
            return false;
        }

        /**
         * Our descriptor.
         */
        @Symbol("gitLabTrustNobody")
        @Extension
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Fork.class.isAssignableFrom(originClass);
            }

            /**
             * {@inheritDoc}
             */
            @NonNull
            @Override
            public String getDisplayName() {
                return Messages.ForkMergeRequestDiscoveryTrait_nobodyDisplayName();
            }
        }
    }

    /**
     * An {@link SCMHeadAuthority} that trusts Members to the project.
     */
    public static class TrustMembers
            extends SCMHeadAuthority<GitLabSCMSourceRequest, MergeRequestSCMHead, MergeRequestSCMRevision> {

        /**
         * Constructor.
         */
        @DataBoundConstructor
        public TrustMembers() {}

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull GitLabSCMSourceRequest request, @NonNull MergeRequestSCMHead head) {
            if (head.getOrigin().equals(SCMHeadOrigin.DEFAULT)) {
                return false;
            }
            assert request.getMembers() != null;
            return request.isMember(head.getOriginOwner());
        }

        /**
         * Our descriptor.
         */
        @Symbol("gitLabTrustMembers")
        @Extension
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {

            /**
             * {@inheritDoc}
             */
            @NonNull
            @Override
            public String getDisplayName() {
                return Messages.ForkMergeRequestDiscoveryTrait_contributorsDisplayName();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Fork.class.isAssignableFrom(originClass);
            }
        }
    }

    /**
     * An {@link SCMHeadAuthority} that trusts those with required permission to the
     * project.
     */
    public static class TrustPermission
            extends SCMHeadAuthority<GitLabSCMSourceRequest, MergeRequestSCMHead, MergeRequestSCMRevision> {

        /**
         * Constructor.
         */
        @DataBoundConstructor
        public TrustPermission() {}

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull GitLabSCMSourceRequest request, @NonNull MergeRequestSCMHead head) {
            if (!head.getOrigin().equals(SCMHeadOrigin.DEFAULT)) {
                AccessLevel permission = request.getPermission(head.getOriginOwner());
                if (permission != null) {
                    switch (permission) {
                        case MAINTAINER:
                        case DEVELOPER:
                        case OWNER:
                            return true;
                        default:
                            return false;
                    }
                }
            }
            return false;
        }

        /**
         * Our descriptor.
         */
        @Symbol("gitLabTrustPermissions")
        @Extension
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {

            /**
             * {@inheritDoc}
             */
            @NonNull
            @Override
            public String getDisplayName() {
                return Messages.ForkMergeRequestDiscoveryTrait_permissionsDisplayName();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Fork.class.isAssignableFrom(originClass);
            }
        }
    }

    /**
     * An {@link SCMHeadAuthority} that trusts everyone.
     */
    public static class TrustEveryone extends SCMHeadAuthority<SCMSourceRequest, ChangeRequestSCMHead2, SCMRevision> {

        /**
         * Constructor.
         */
        @DataBoundConstructor
        public TrustEveryone() {}

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull SCMSourceRequest request, @NonNull ChangeRequestSCMHead2 head) {
            return true;
        }

        /**
         * Our descriptor.
         */
        @Symbol("gitLabTrustEveryone")
        @Extension
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {

            /**
             * {@inheritDoc}
             */
            @NonNull
            @Override
            public String getDisplayName() {
                return Messages.ForkMergeRequestDiscoveryTrait_everyoneDisplayName();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Fork.class.isAssignableFrom(originClass);
            }
        }
    }
}
