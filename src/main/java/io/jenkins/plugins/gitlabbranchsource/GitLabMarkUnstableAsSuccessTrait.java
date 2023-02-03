package io.jenkins.plugins.gitlabbranchsource;

import hudson.Extension;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class GitLabMarkUnstableAsSuccessTrait extends SCMSourceTrait {

    private boolean markUnstableAsSuccess = false;

    @DataBoundConstructor
    public GitLabMarkUnstableAsSuccessTrait() {
        //empty
    }

    @DataBoundSetter
    public void setMarkUnstableAsSuccess(boolean markUnstableAsSuccess) {
        this.markUnstableAsSuccess = markUnstableAsSuccess;
    }

    public boolean getMarkUnstableAsSuccess() {
        return markUnstableAsSuccess;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withMarkUnstableAsSuccess(doMarkUnstableAsSuccess());
        }
    }

    public boolean doMarkUnstableAsSuccess() {
        return markUnstableAsSuccess;
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("gitlabMarkUnstableAsSuccess")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.GitLabMarkUnstableAsSuccessTrait_displayName();
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

}
