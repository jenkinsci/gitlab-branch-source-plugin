package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import javax.annotation.Nonnull;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;


public class LogCommentTrait extends SCMSourceTrait {

    @NonNull
    private String sudoUser;

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public LogCommentTrait(@Nonnull String sudoUser) {
        this.sudoUser = sudoUser;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.witLogCommentEnabled(true);
            ctx.withSudoUser(getSudoUser());
        }
    }

    @NonNull
    public String getSudoUser() {
        return sudoUser;
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("logComment")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

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