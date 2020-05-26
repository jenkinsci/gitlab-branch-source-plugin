package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class LogCommentTrait extends SCMSourceTrait {

    private final String sudoUser;

    private final boolean logSuccessOnly;

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public LogCommentTrait(String sudoUser, boolean logSuccessOnly) {
        this.logSuccessOnly = logSuccessOnly;
        this.sudoUser = sudoUser;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withLogCommentEnabled(true);
            ctx.withLogSuccess(logSuccessOnly);
            ctx.withSudoUser(sudoUser);
        }
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
