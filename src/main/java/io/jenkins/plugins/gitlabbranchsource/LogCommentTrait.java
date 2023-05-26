package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class LogCommentTrait extends SCMSourceTrait {

    @NonNull
    private String sudoUser = "";

    private boolean logSuccess;

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public LogCommentTrait() {
        // empty
    }

    /**
     * Setter for stapler to enable logging of successful builds.
     */
    @DataBoundSetter
    public void setLogSuccess(boolean logSuccess) {
        this.logSuccess = logSuccess;
    }

    /**
     * Setter for stapler to set the username of the sudo user.
     */
    @DataBoundSetter
    public void setSudoUser(@NonNull String sudoUser) {
        this.sudoUser = sudoUser;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withLogCommentEnabled(true);
            ctx.withLogSuccess(getLogSuccess());
            ctx.withSudoUser(getSudoUser());
        }
    }

    /**
     * Getter method for username of sudo user.
     *
     * @return username of sudo user.
     */
    @NonNull
    public String getSudoUser() {
        return sudoUser;
    }

    /**
     * Getter method for logging successful build.
     *
     * @return true if logs of successful build required.
     */
    public boolean getLogSuccess() {
        return logSuccess;
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("logComment")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.LogCommentTrait_displayName();
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
