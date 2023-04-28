package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class GitLabSkipNotificationsTrait extends SCMSourceTrait {

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public GitLabSkipNotificationsTrait() {
        // empty
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withNotificationsDisabled(true);
        }
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("gitlabSkipNotifications")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.GitLabSkipNotificationsTrait_displayName();
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
