package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMHeadAuthority;
import jenkins.scm.api.trait.SCMHeadAuthorityDescriptor;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.TagSCMHeadCategory;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A {@link Discovery} trait for GitLab that will discover tags on the project.
 */
public class TagDiscoveryTrait extends SCMSourceTrait {
    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public TagDiscoveryTrait() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
        ctx.wantTags(true);
        ctx.withAuthority(new TagSCMHeadAuthority());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeCategory(@NonNull SCMHeadCategory category) {
        return category instanceof TagSCMHeadCategory;
    }

    /**
     * Our descriptor.
     */
    @Symbol("gitHubTagDiscovery")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.TagDiscoveryTrait_displayName();
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

    }

    /**
     * Trusts tags from the origin project.
     */
    public static class TagSCMHeadAuthority extends SCMHeadAuthority<SCMSourceRequest, GitLabTagSCMHead, GitTagSCMRevision> {
        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull SCMSourceRequest request, @NonNull GitLabTagSCMHead head) {
            return true;
        }

        /**
         * Out descriptor.
         */
        @Extension
        public static class DescriptorImpl extends SCMHeadAuthorityDescriptor {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDisplayName() {
                return Messages.TagDiscoveryTrait_authorityDisplayName();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isApplicableToOrigin(@NonNull Class<? extends SCMHeadOrigin> originClass) {
                return SCMHeadOrigin.Default.class.isAssignableFrom(originClass);
            }
        }
    }
}

