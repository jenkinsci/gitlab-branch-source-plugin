package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A {@link SCMSourceTrait} for {@link GitLabSCMSource} that overrides the {@link GitLabServers}
 * settings for webhook registration.
 */
public class WebhookRegistrationTrait extends SCMSourceTrait {

    /**
     * The mode of registration to apply.
     */
    @NonNull
    private final WebhookRegistration mode;

    /**
     * Constructor.
     *
     * @param mode the mode of registration to apply.
     */
    @DataBoundConstructor
    public WebhookRegistrationTrait(@NonNull String mode) {
        this(WebhookRegistration.valueOf(mode));
    }

    /**
     * Constructor.
     *
     * @param mode the mode of registration to apply.
     */
    public WebhookRegistrationTrait(@NonNull WebhookRegistration mode) {
        this.mode = mode;
    }

    /**
     * Gets the mode of registration to apply.
     *
     * @return the mode of registration to apply.
     */
    @NonNull
    public final WebhookRegistration getMode() {
        return mode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        ((GitLabSCMSourceContext) context).webhookRegistration(getMode());
    }

    /**
     * Our constructor.
     */
    @Extension
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.WebhookRegistrationTrait_displayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitLabSCMSource.class;
        }

        /**
         * Form completion.
         *
         * @return the mode options.
         */
        @Restricted(NoExternalUse.class)
        @SuppressWarnings("unused") // stapler form binding
        public ListBoxModel doFillModeItems() {
            ListBoxModel result = new ListBoxModel();
            result.add(Messages.WebhookRegistrationTrait_disableHook(), WebhookRegistration.DISABLE.toString());
            result.add(Messages.WebhookRegistrationTrait_useItemHook(), WebhookRegistration.ITEM.toString());
            return result;
        }

    }
}
