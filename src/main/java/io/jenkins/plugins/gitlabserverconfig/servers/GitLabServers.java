package io.jenkins.plugins.gitlabserverconfig.servers;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.PersistentDescriptor;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabserverconfig.servers.helpers.GitLabPersonalAccessTokenCreator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import static hudson.Util.fixNull;

/**
 * Represents the global configuration of GitLab servers.
 */
@Extension
public class GitLabServers extends GlobalConfiguration implements PersistentDescriptor {

    private static final Logger LOGGER = Logger.getLogger(GitLabServers.class.getName());

    /**
     * The list of {@link GitLabServer}, this is subject to the constraint that there can only ever
     * be one entry for each {@link GitLabServer#getServerUrl()}.
     */
    private List<GitLabServer> servers;

    /**
     * Gets the {@link GitLabServers} singleton.
     *
     * @return the {@link GitLabServers} singleton.
     */
    public static GitLabServers get() {
        return ExtensionList.lookup(GlobalConfiguration.class).get(GitLabServers.class);
    }

    /**
     * Helper function to get predicate to filter servers based on their names
     *
     * @param keyExtractor the Function to filter
     * @param <T> In this case it is server
     * @return a predicate to filter servers list
     */
    private static <T> Predicate<T> distinctByKey(
        Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * Populates a {@link ListBoxModel} with the servers.
     *
     * @return A {@link ListBoxModel} with all the servers
     */
    public ListBoxModel getServerItems() {
        ListBoxModel result = new ListBoxModel();
        for (GitLabServer server : getServers()) {
            String serverUrl = server.getServerUrl();
            String serverName = server.getName(); // serverName or name or displayName
            result.add(
                StringUtils.isBlank(serverName) ? serverUrl : serverName + " (" + serverUrl + ")",
                serverName);
        }
        return result;
    }

    /**
     * Gets the list of endpoints.
     *
     * @return the list of endpoints
     */
    @NonNull
    public List<GitLabServer> getServers() {
        if (servers == null || servers.isEmpty()) {
            servers = new ArrayList<>();
            // Don't really need to create this manually. Having a default one makes it be easier for a new user
            servers.add(new GitLabServer(GitLabServer.GITLAB_SERVER_URL,
                GitLabServer.GITLAB_SERVER_DEFAULT_NAME, ""));
        }
        return Collections.unmodifiableList(servers);
    }

    /**
     * Sets the list of GitLab Servers
     *
     * @param servers the list of endpoints.
     */
    public void setServers(@CheckForNull List<? extends GitLabServer> servers) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        this.servers = fixNull(servers).stream()
            .filter(distinctByKey(GitLabServer::getName)).collect(Collectors.toList());
        save();
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return Messages.GitLabServers_displayName();
    }

    /**
     * Gets descriptor of {@link GitLabPersonalAccessTokenCreator}
     *
     * @return the list of descriptors
     */
    public List<Descriptor> actions() {
        return Collections
            .singletonList(Jenkins.get().getDescriptor(GitLabPersonalAccessTokenCreator.class));
    }

    /**
     * Adds an server Checks if the GitLab Server name is unique
     *
     * @param server the server to add.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean addServer(@NonNull GitLabServer server) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        GitLabServer gitLabServer = servers.stream()
            .filter(s -> s.getName().equals(server.getName()))
            .findAny()
            .orElse(null);
        if (gitLabServer != null) {
            return false;
        }
        servers.add(server);
        setServers(servers);
        return true;
    }

    /**
     * Updates an existing endpoint (or adds if missing) Checks if the GitLab Server name is
     * matched
     *
     * @param server the server to update.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean updateServer(@NonNull GitLabServer server) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        if (!servers.contains(server)) {
            return false;
        }
        servers = servers.stream()
            .map(oldServer -> oldServer.getName().equals(server.getName()) ? server : oldServer)
            .collect(Collectors.toList());
        setServers(servers);
        return true;
    }

    /**
     * Removes a server entry Checks if the GitLab Server name is matched
     *
     * @param name the server name to remove.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean removeServer(@CheckForNull String name) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        boolean removed = servers.removeIf(s -> s.getName().equals(name));
        if (removed) {
            setServers(servers);
        }
        return removed;
    }

    /**
     * Checks to see if the supplied server URL is defined in the global configuration.
     *
     * @param serverName the server url to check.
     * @return the global configuration for the specified server url or {@code null} if not defined.
     */
    @CheckForNull
    public GitLabServer findServer(@CheckForNull String serverName) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        return servers.stream()
            .filter(server -> server.getName().equals(serverName))
            .findAny()
            .orElse(null);
    }

}
