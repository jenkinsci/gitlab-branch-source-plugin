package io.jenkins.plugins.gitlabserver.servers;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabserver.servers.helpers.GitLabPersonalAccessTokenCreator;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents the global configuration of GitLab servers.
 */

@Extension
public class GitLabServers extends GlobalConfiguration {

    /**
     * The list of {@link GitLabServer}, this is subject to the constraint that there can only ever be
     * one entry for each {@link GitLabServer#getServerUrl()}.
     */
    private List<GitLabServer> servers;

    /**
     * Constructor.
     */
    public GitLabServers() {
        load();
    }

    /**
     * {@inheritDoc}
     */
    // TODO: understand what configure does
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        servers = req.bindJSONToList(GitLabServer.class, json.get("servers"));
        save();
        return super.configure(req, json);
    }

    /**
     * Gets the {@link GitLabServers} singleton.
     *
     * @return the {@link GitLabServers} singleton.
     */
    public static GitLabServers get() {
        return ExtensionList.lookup(GlobalConfiguration.class).get(GitLabServers.class);
    }


    /**
     * Returns {@code true} if and only if there is more than one configured endpoint.
     *
     * @return {@code true} if and only if there is more than one configured endpoint.
     */
    public boolean isEndpointSelectable() {
        return getServers().size() > 1;
    }

    /**
     * Populates a {@link ListBoxModel} with the endpoints.
     *
     * @return A {@link ListBoxModel} with all the endpoints
     */
    public ListBoxModel getServerItems() {
        ListBoxModel result = new ListBoxModel();
        for (GitLabServer endpoint : getServers()) {
            String serverUrl = endpoint.getServerUrl();
            String displayName = endpoint.getName();
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

    public List<Descriptor> actions() {
        return Collections.singletonList(Jenkins.getInstance().getDescriptor(GitLabPersonalAccessTokenCreator.class));
    }

    /**
     * Sets the list of endpoints.
     *
     * @param endpoints the list of endpoints.
     */
    public void setServers(@CheckForNull List<? extends GitLabServer> endpoints) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        servers = new ArrayList<>(Util.fixNull(endpoints));
//        for(GitLabServer server: servers) {
//            connectionMap.put(server.getName(), server);
//        }
    }

    /**
     * Adds an endpoint.
     *
     * @param endpoint the endpoint to add.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean addServer(@Nonnull GitLabServer endpoint) {
        List<GitLabServer> endpoints = new ArrayList<>(getServers());
        for (GitLabServer ep : endpoints) {
            if (ep.getServerUrl().equals(endpoint.getServerUrl())) {
                return false;
            }
        }
        endpoints.add(endpoint);
        setServers(endpoints);
        return true;
    }

    /**
     * Updates an existing endpoint (or adds if missing).
     *
     * @param endpoint the endpoint to update.
     */
    public void updateServer(@Nonnull GitLabServer endpoint) {
        List<GitLabServer> endpoints = new ArrayList<>(getServers());
        boolean found = false;
        for (int i = 0; i < endpoints.size(); i++) {
            GitLabServer ep = endpoints.get(i);
            if (ep.getServerUrl().equals(endpoint.getServerUrl())) {
                endpoints.set(i, endpoint);
                found = true;
                break;
            }
        }
        if (!found) {
            endpoints.add(endpoint);
        }
        setServers(endpoints);
    }
}
