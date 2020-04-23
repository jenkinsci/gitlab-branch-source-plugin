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

public class BuildStatusNameCustomPartTrait extends SCMSourceTrait {

    @NonNull
    private String buildStatusNameCustomPart = "";

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public BuildStatusNameCustomPartTrait() {
        // empty
    }

    /**
     * Setter for stapler to set the buildStatusNameCustomPart of the build status
     */
    @DataBoundSetter
    public void setBuildStatusNameCustomPart(@NonNull String buildStatusNameCustomPart) {
        this.buildStatusNameCustomPart = buildStatusNameCustomPart;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        if (context instanceof GitLabSCMSourceContext) {
            GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
            ctx.withBuildStatusNameCustomPart(getBuildStatusNameCustomPart());
        }
    }

    /**
     * Getter method for the build status context prefix
     *
     * @return build status context prefix
     */
    @NonNull
    public String getBuildStatusNameCustomPart() {
        return buildStatusNameCustomPart;
    }

    /**
     * Our descriptor.
     */
    @Extension
    @Symbol("buildStatusNameCustomPart")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.BuildStatusNameCustomPartTrait_displayName();
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
