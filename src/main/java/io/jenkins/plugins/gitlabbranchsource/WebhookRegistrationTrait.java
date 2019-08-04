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
 * settings for web hook & system hook registration.
 */
public class WebhookRegistrationTrait extends SCMSourceTrait {

    /**
     * The web hook mode of registration to apply.
     */
    @NonNull
    private final GitLabHookRegistration webHookMode;

    /**
     * The system hook mode of registration to apply.
     */
    @NonNull
    private GitLabHookRegistration systemHookMode;

    /**
     * Constructor.
     *
     * @param webHookMode the mode of registration to apply.
     */
    @DataBoundConstructor
    public WebhookRegistrationTrait(@NonNull String webHookMode, @NonNull String systemHookMode) {
        this(GitLabHookRegistration.valueOf(webHookMode), GitLabHookRegistration.valueOf(systemHookMode));
    }

    /**
     * Constructor.
     *
     * @param webHookMode the web hook mode of registration to apply.
     * @param systemHookMode the system hook mode of registration to apply.
     */
    public WebhookRegistrationTrait(@NonNull GitLabHookRegistration webHookMode, @NonNull GitLabHookRegistration systemHookMode) {
        this.webHookMode = webHookMode;
        this.systemHookMode = systemHookMode;
    }

    /**
     * Gets the web hook mode of registration to apply.
     *
     * @return the web hook mode of registration to apply.
     */
    @NonNull
    public final GitLabHookRegistration getWebHookMode() {
        return webHookMode;
    }

    /**
     * Gets the system hook mode of registration to apply.
     *
     * @return the system hook mode of registration to apply.
     */
    @NonNull
    public final GitLabHookRegistration getSystemHookMode() {
        return systemHookMode;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitLabSCMSourceContext ctx = (GitLabSCMSourceContext) context;
        ctx.webhookRegistration(getWebHookMode());
        ctx.systemhookRegistration(getSystemHookMode());
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
         * @return the web hook mode options.
         */
        @Restricted(NoExternalUse.class)
        @SuppressWarnings("unused") // stapler form binding
        public ListBoxModel doFillWebHookModeItems() {
            return getOptions(true);
        }

        /**
        * Form completion.
        *
        * @return the system hook mode options.
        */
        @Restricted(NoExternalUse.class)
        @SuppressWarnings("unused") // stapler form binding
        public ListBoxModel doFillSystemHookModeItems() {
            return getOptions(false);
        }

        private ListBoxModel getOptions(boolean isWebHook) {
            ListBoxModel result = new ListBoxModel();
            String pronoun = isWebHook ? "Web Hook" : "System Hook";
            result.add(Messages.WebhookRegistrationTrait_disable(pronoun), GitLabHookRegistration.DISABLE.toString());
            result.add(Messages.WebhookRegistrationTrait_useSystem(pronoun), GitLabHookRegistration.SYSTEM.toString());
            result.add(Messages.WebhookRegistrationTrait_useItem(pronoun), GitLabHookRegistration.ITEM.toString());
            return result;
        }

    }
}
