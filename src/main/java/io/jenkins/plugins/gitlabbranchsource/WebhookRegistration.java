package io.jenkins.plugins.gitlabbranchsource;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;

/**
 * Enumeration of the different webhook registration modes.
 */
public enum WebhookRegistration {
    /**
     * Disable webhook registration.
     */
    DISABLE,
    /**
     * Use the global system configuration for webhook registration. (If the {@link GitLabServers}
     * does not have webhook registration configured then this will be the same as {@link #DISABLE})
     */
    SYSTEM,
    /**
     * Use the item scoped credentials to register the webhook.
     */
    ITEM
}
