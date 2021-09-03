package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class WebhookListenerBuildConditionsTrait extends SCMSourceTrait {

    /**
     * Always fire build trigger on MR Open event
     */
    private boolean alwaysBuildMROpen = true;

    /**
     * Always fire build trigger on MR Re-Open event
     */
    private boolean alwaysBuildMRReOpen = true;

    /**
     * Always ignore webhook if it's an approval event
     */
    private boolean alwaysIgnoreMRApprove = false;

    /**
     * Always ignore webhook if it's an un-approval event
     */
    private boolean alwaysIgnoreMRUnApprove = false;

    /**
     * Always ignore webhook if it's a non code related update such as title change
     */
    private boolean alwaysIgnoreNonCodeRelatedUpdates = false;

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public WebhookListenerBuildConditionsTrait(boolean alwaysBuildMROpen, boolean alwaysBuildMRReOpen, boolean alwaysIgnoreMRApprove, boolean alwaysIgnoreMRUnApprove, boolean alwaysIgnoreNonCodeRelatedUpdates) {
        this.alwaysBuildMROpen = alwaysBuildMROpen;
        this.alwaysBuildMRReOpen = alwaysBuildMRReOpen;
        this.alwaysIgnoreMRApprove = alwaysIgnoreMRApprove;
        this.alwaysIgnoreMRUnApprove = alwaysIgnoreMRUnApprove;
        this.alwaysIgnoreNonCodeRelatedUpdates = alwaysIgnoreNonCodeRelatedUpdates;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withAlwaysBuildMROpen(getAlwaysBuildMROpen())
                .withAlwaysBuildMRReOpen(getAlwaysBuildMRReOpen())
                .withAlwaysIgnoreMRApprove(getAlwaysIgnoreMRApprove())
                .withAlwaysIgnoreMRUnApprove(getAlwaysIgnoreMRUnApprove())
                .withAlwaysIgnoreNonCodeRelatedUpdates(getAlwaysIgnoreNonCodeRelatedUpdates());
        }
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("WebhookListenerBuildConditionsTrait")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.WebhookListenerBuildConditionsTrait_displayName();
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }

        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitLabSCMSource.class;
        }
    }

    /**
     * Run build on MR open
     *
     * @return true to fire trigger on MR Open
     */
    public boolean getAlwaysBuildMROpen() {
        return alwaysBuildMROpen;
    }

    /**
     * Run build on MR re-open
     *
     * @return true to fire trigger on MR Re-Open
     */
    public boolean getAlwaysBuildMRReOpen() {
        return alwaysBuildMRReOpen;
    }

    /**
     * Run build on MR approval
     *
     * @return false to run build on MR approval
     */
    public boolean getAlwaysIgnoreMRApprove() {
        return alwaysIgnoreMRApprove;
    }

    /**
     * Run build on MR un-approval
     *
     * @return false to run build on non-code related MR updates
     */
    public boolean getAlwaysIgnoreMRUnApprove() {
        return alwaysIgnoreMRUnApprove;
    }

    /**
     * Run build on MR non-code related updates e.g. MR title update
     *
     * @return false to run build on non-code related MR updates
     */
    public boolean getAlwaysIgnoreNonCodeRelatedUpdates() {
        return alwaysIgnoreNonCodeRelatedUpdates;
    }

}
