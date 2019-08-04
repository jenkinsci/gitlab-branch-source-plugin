package io.jenkins.plugins.gitlabbranchsource;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;

/**
 * Enumeration of the different web hook/system hook registration modes.
 */
public enum GitLabHookRegistration {
    /**
     * Disable hook registration.
     */
     DISABLE,
    /**
     * Use the global system configuration for hook registration. (If the {@link GitLabServers}
     * does not have hook registration configured then this will be the same as {@link #DISABLE})
     */
     SYSTEM,
      /**
     * Use the item scoped credentials to register the hook.
     */
      ITEM
}
