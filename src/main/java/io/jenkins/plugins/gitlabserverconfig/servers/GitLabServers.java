package io.jenkins.plugins.gitlabserverconfig.servers;

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
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static hudson.Util.fixNull;

/**
 * Represents the global configuration of GitLab servers.
 */
@Extension
public class GitLabServers extends GlobalConfiguration implements PersistentDescriptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabServers.class);

    /**
     * The list of {@link GitLabServer}, this is subject to the constraint that there can only ever be
     * one entry for each {@link GitLabServer#getServerUrl()}.
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
     * Populates a {@link ListBoxModel} with the servers.
     *
     * @return A {@link ListBoxModel} with all the servers
     */
    public ListBoxModel getServerItems() {
        ListBoxModel result = new ListBoxModel();
        for (GitLabServer server : getServers()) {
            String serverUrl = server.getServerUrl();
            String displayName = server.getName();
            result.add(StringUtils.isBlank(displayName) ? serverUrl : displayName + " (" + serverUrl + ")", serverUrl);
        }
        return result;
    }

    /**
     * Gets the list of endpoints.
     *
     * @return the list of endpoints
     */
    @Nonnull
    public List<GitLabServer> getServers() {
        return servers == null || servers.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(servers);
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return Messages.GitLabServers_displayName();
    }

    /**
     * Gets descriptor of {@link GitLabPersonalAccessTokenCreator}
     *
     * returns the list of descriptors
     */
    public List<Descriptor> actions() {
        return Collections.singletonList(Jenkins.get().getDescriptor(GitLabPersonalAccessTokenCreator.class));
    }

    /**
     * Helper function to get predicate to filter servers
     * based on their names
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

    /**
     * Adds an server
     * Checks if the GitLab Server name is unique
     *
     * @param server the server to add.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean addServer(@Nonnull GitLabServer server) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        GitLabServer s = servers.stream()
                .filter(server1 -> server1.getName().equals(server.getName()))
                .findAny()
                .orElse(null);
        if(s != null) {
            return false;
        }
        servers.add(server);
        setServers(servers);
        return true;
    }

    /**
     * Updates an existing endpoint (or adds if missing)
     * Checks if the GitLab Server name is matched
     *
     * @param server the server to update.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean updateServer(@Nonnull GitLabServer server) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        if(!servers.contains(server)) {
            return false;
        }
        servers = servers.stream()
                .map(oldServer -> oldServer.getName().equals(server.getName()) ? server : oldServer)
                .collect(Collectors.toList());
        setServers(servers);
        return true;
    }

    /**
     * Removes a server entry
     * Checks if the GitLab Server name is matched
     *
     * @param name the server name to remove.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean removeServer(@CheckForNull String name) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        boolean removed = servers.removeIf(s -> s.getName().equals(name));
        if(removed) {
            setServers(servers);
        }
        return removed;
    }

    /**
     * Checks to see if the supplied server URL is defined in the global configuration.
     *
     * @param serverUrl the server url to check.
     * @return the global configuration for the specified server url or {@code null} if not defined.
     */
    @CheckForNull
    public  GitLabServer findServer(@CheckForNull String serverUrl) {
        List<GitLabServer> servers = new ArrayList<>(getServers());
        return servers.stream()
                .filter(server1 -> server1.getName().equals(serverUrl))
                .findAny()
                .orElse(null);
    }

}
