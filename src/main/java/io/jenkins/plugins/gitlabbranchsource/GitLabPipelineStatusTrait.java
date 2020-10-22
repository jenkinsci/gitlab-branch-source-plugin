package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabPipelineStatusStrategy;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

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
     * List of branches for which set status
     */
    private String pipelineStatusIncludeRef = "";

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public GitLabPipelineStatusTrait() {
        // empty
    }

    @DataBoundSetter
    public void setStrategyId(int strategyId) {
        this.strategyId = strategyId;
    }

    @DataBoundSetter
    public void setMarkUnstableAsSuccess(boolean markUnstableAsSuccess) {
        this.markUnstableAsSuccess = markUnstableAsSuccess;
    }

    @DataBoundSetter
    public void setPipelineStatusIncludeRef(String pipelineStatusIncludeRef) {
        this.pipelineStatusIncludeRef = pipelineStatusIncludeRef;
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
     * Gets the pipelineStatusIncludeRef.
     *
     * @return the pipelineStatusIncludeRef.
     */
    public String getPipelineStatusIncludeRef() {
        return pipelineStatusIncludeRef;
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
            ctx.withPipelineStatusIncludeRef(getPipelineStatusIncludeRef());
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

        /**
         * Performs form validation for a proposed
         *
         * @param value the value to check.
         * @return the validation results.
         */
        public FormValidation doCheckPipelineStatusIncludeRef(@QueryParameter String value) {
            value = StringUtils.trimToEmpty(value);
            try {
                Pattern.compile(value);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.error(e.getDescription());
            }
        }
    }

}
