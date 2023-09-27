package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.trait.SCMNavigatorContext;
import jenkins.scm.api.trait.SCMNavigatorTrait;
import jenkins.scm.api.trait.SCMNavigatorTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ProjectNamingStrategyTrait extends SCMNavigatorTrait {

    private int strategyId = 1;

    @DataBoundConstructor
    public ProjectNamingStrategyTrait() {
        // empty
    }

    public int getStrategyId() {
        return strategyId;
    }

    @DataBoundSetter
    public void setStrategyId(int strategyId) {
        this.strategyId = strategyId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMNavigatorContext<?, ?> context) {
        if (context instanceof GitLabSCMNavigatorContext) {
            GitLabSCMNavigatorContext ctx = (GitLabSCMNavigatorContext) context;
            ctx.withProjectNamingStrategy(getStrategyId());
        }
    }

    /**
     * Our descriptor.
     */
    @Symbol("gitLabProjectNamingStrategy")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMNavigatorTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.ProjectNamingStrategyTrait_displayName();
        }

        @Override
        public Class<? extends SCMNavigator> getNavigatorClass() {
            return GitLabSCMNavigator.class;
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
            result.add(Messages.ProjectNamingStrategyTrait_fullProjectPath(), "1");
            result.add(Messages.ProjectNamingStrategyTrait_projectName(), "2");
            result.add(Messages.ProjectNamingStrategyTrait_contextualProjectPath(), "3");
            result.add(Messages.ProjectNamingStrategyTrait_simpleProjectPath(), "4");
            return result;
        }
    }
}
