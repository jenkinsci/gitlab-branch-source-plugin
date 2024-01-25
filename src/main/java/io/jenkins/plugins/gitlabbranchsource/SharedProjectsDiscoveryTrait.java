package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.trait.SCMNavigatorContext;
import jenkins.scm.api.trait.SCMNavigatorTrait;
import jenkins.scm.api.trait.SCMNavigatorTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class SharedProjectsDiscoveryTrait extends SCMNavigatorTrait {

    @DataBoundConstructor
    public SharedProjectsDiscoveryTrait() {}

    @Override
    protected void decorateContext(SCMNavigatorContext<?, ?> context) {
        if (context instanceof GitLabSCMNavigatorContext) {
            GitLabSCMNavigatorContext ctx = (GitLabSCMNavigatorContext) context;
            ctx.wantSharedProjects(true);
        }
    }

    /**
     * Our descriptor.
     */
    @Symbol("gitLabSharedProjectsDiscovery")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMNavigatorTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.SharedProjectsDiscoveryTrait_displayName();
        }

        @Override
        public Class<? extends SCMNavigator> getNavigatorClass() {
            return GitLabSCMNavigator.class;
        }
    }
}
