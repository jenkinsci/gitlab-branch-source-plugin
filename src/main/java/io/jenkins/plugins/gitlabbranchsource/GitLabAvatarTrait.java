package io.jenkins.plugins.gitlabbranchsource;

import hudson.Extension;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class GitLabAvatarTrait extends SCMSourceTrait {

    /**
     * Stores true or false to disable avatar for projects (but not owner)
     */
    private boolean disableProjectAvatar = false;

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public GitLabAvatarTrait() {
        //empty
    }

    @DataBoundSetter
    public void setDisableProjectAvatar(boolean disableProjectAvatar) {
        this.disableProjectAvatar = disableProjectAvatar;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withProjectAvatarDisabled(isDisableProjectAvatar());
        }
    }

    public boolean isDisableProjectAvatar() {
        return disableProjectAvatar;
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("gitlabAvatar")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.GitLabAvatarTrait_displayName();
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
