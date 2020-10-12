package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabPipelineStatusStrategy;
import java.util.EnumSet;
import java.util.Set;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class GitLabPipelineStatusTrait extends SCMSourceTrait {

    /**
     * The pipeline publish strategy encoded as a bit-field.
     */
    private int strategyId = 1;

    /**
     * Whether Unstable builds should be considered success/failure
     */
    private boolean markUnstableAsSuccess = false;

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public GitLabPipelineStatusTrait() {
    }

    @DataBoundSetter
    public void setStrategyId(int strategyId) {
        this.strategyId = strategyId;
    }

    @DataBoundSetter
    public void setMarkUnstableAsSuccess(boolean markUnstableAsSuccess) {
        this.markUnstableAsSuccess = markUnstableAsSuccess;
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
     * Gets the mark unstable as success.
     *
     * @return the markUnstableAsSuccess.
     */
    public boolean isMarkUnstableAsSuccess() {
        return markUnstableAsSuccess;
    }

    /**
     * Returns the strategies.
     *
     * @return the strategies.
     */
    @NonNull
    public Set<GitLabPipelineStatusStrategy> getStrategy() {
        switch (strategyId) {
            case 2:
                return EnumSet.of(GitLabPipelineStatusStrategy.STAGES);
            case 3:
                return EnumSet.of(GitLabPipelineStatusStrategy.NONE);
            default:
                return EnumSet.of(GitLabPipelineStatusStrategy.RESULT);
        }
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withPipelineStatusStrategies(getStrategy());
            ctx.withMarkUnstableAsSuccess(isMarkUnstableAsSuccess());
        }
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("gitlabPipelineStatus")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.GitLabPipelineStatusTrait_displayName();
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }

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
            result.add(Messages.GitLabPipelineStatusTrait_result(), "1");
            result.add(Messages.GitLabPipelineStatusTrait_stages(), "2");
            result.add(Messages.GitLabPipelineStatusTrait_none(), "3");
            return result;
        }

    }

}
