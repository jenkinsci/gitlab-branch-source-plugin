package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Allows a GitLab merge request comment to trigger an immediate build based on a comment string.
 */
public class TriggerMRCommentTrait extends SCMSourceTrait {

    /**
     * The comment body to trigger a new build on.
     */
    private final String commentBody;

    /**
     * Only comment trigger by trusted members only (members having access to project)
     */
    private final boolean onlyTrustedMembersCanTrigger;

    /**
     * Constructor.
     *
     * @param commentBody the comment body to trigger a new build on
     * @param onlyTrustedMembersCanTrigger
     */
    @DataBoundConstructor
    public TriggerMRCommentTrait(String commentBody, boolean onlyTrustedMembersCanTrigger) {
        this.commentBody = commentBody;
        this.onlyTrustedMembersCanTrigger = onlyTrustedMembersCanTrigger;
    }

    /**
     * The comment body to trigger a new build on.
     *
     * @return the comment body to use
     */
    public String getCommentBody() {
        if (commentBody == null || commentBody.isEmpty()) {
            return "^REBUILD$";
        }
        return commentBody;
    }

    /**
     * Allow trigger a new build by trusted members only.
     *
     * @return true if allow trusted members only
     * @since TODO
     */
    public boolean getOnlyTrustedMembersCanTrigger() {
        return onlyTrustedMembersCanTrigger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
        ctx.withMRCommentTriggerEnabled(true);
        ctx.withOnlyTrustedMembersCanTrigger(getOnlyTrustedMembersCanTrigger());
        ctx.withCommentBody(getCommentBody());
    }

    @Extension
    @Symbol("mrTriggerComment")
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.TriggerMRCommentTrait_displayName();
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
