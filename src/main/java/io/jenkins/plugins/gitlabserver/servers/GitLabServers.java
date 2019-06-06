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
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the global configuration of GitLab servers.
 */
@Extension
public class GitLabServers extends GlobalConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabServers.class);

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

    /**
     * Gets descriptor of {@link GitLabPersonalAccessTokenCreator}
     *
     * returns the list of descriptors
     */
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
    }

    /**
     * Adds an endpoint
     * Checks if the GitLab Server name is unique
     *
     * @param endpoint the endpoint to add.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean addServer(@Nonnull GitLabServer endpoint) {
        List<GitLabServer> endpoints = new ArrayList<>(getServers());
        for (GitLabServer ep : endpoints) {
            if (Util.fixNull(ep.getName()).equals(Util.fixNull(endpoint.getName()))) {
                return false;
            }
        }
        endpoints.add(endpoint);
        setServers(endpoints);
        return true;
    }

    /**
     * Updates an existing endpoint (or adds if missing)
     * Checks if the GitLab Server name is matched
     *
     * @param endpoint the endpoint to update.
     */
    public void updateServer(@Nonnull GitLabServer endpoint) {
        List<GitLabServer> endpoints = new ArrayList<>(getServers());
        boolean found = false;
        for (int i = 0; i < endpoints.size(); i++) {
            GitLabServer ep = endpoints.get(i);
            if (Util.fixNull(ep.getName()).equals(Util.fixNull(endpoint.getName()))) {
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

    /**
     * Removes an endpoint.
     * Checks if the GitLab Server name is matched
     *
     * @param name the server URL to remove.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean removeServer(@CheckForNull String name) {
        boolean modified = false;
        List<GitLabServer> endpoints = new ArrayList<>(getServers());
        for (Iterator<GitLabServer> iterator = endpoints.iterator(); iterator.hasNext(); ) {
            if (Util.fixNull(name).equals(Util.fixNull(iterator.next().getName()))) {
                iterator.remove();
                modified = true;
            }
        }
        setServers(endpoints);
        return modified;
    }
}
