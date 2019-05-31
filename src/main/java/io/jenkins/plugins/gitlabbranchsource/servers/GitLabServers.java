package io.jenkins.plugins.gitlabbranchsource.servers;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.servers.helpers.GitLabPersonalAccessTokenCreator;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
     * {@inheritDoc}
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        return true;
    }

    /**
     * Gets the list of endpoints.
     *
     * @return the list of endpoints
     */
    @Nonnull
    public synchronized List<GitLabServer> getServers() {
        return servers == null || servers.isEmpty()
                ? Collections.<GitLabServer>emptyList()
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
     * @param servers the list of endpoints.
     */

    public synchronized void setServers(@CheckForNull List<? extends GitLabServer> servers) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        List<GitLabServer> eps = new ArrayList<>(Util.fixNull(servers));
        // remove duplicates and empty urls
        Set<String> serverUrls = new HashSet<String>();
        for (ListIterator<GitLabServer> iterator = eps.listIterator(); iterator.hasNext(); ) {
            GitLabServer endpoint = iterator.next();
            String serverUrl = endpoint.getServerUrl();
            if (StringUtils.isBlank(serverUrl) || serverUrls.contains(serverUrl)) {
                iterator.remove();
                continue;
            }
            serverUrls.add(serverUrl);
        }
        this.servers = eps;
        save();
    }

    /**
     * Adds an endpoint.
     *
     * @param endpoint the endpoint to add.
     * @return {@code true} if the list of endpoints was modified
     */
    public synchronized boolean addServer(@Nonnull GitLabServer endpoint) {
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
    public synchronized void updateServer(@Nonnull GitLabServer endpoint) {
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

    /**
     * Removes an endpoint.
     *
     * @param endpoint the endpoint to remove.
     * @return {@code true} if the list of endpoints was modified
     */
    public boolean removeServer(@Nonnull GitLabServer endpoint) {
        return removeServer(endpoint.getServerUrl());
    }

    /**
     * Removes an endpoint.
     *
     * @param serverUrl the server URL to remove.
     * @return {@code true} if the list of endpoints was modified
     */
    private synchronized boolean removeServer(@CheckForNull String serverUrl) { // when passing predicate add an argument GitLabServerPredicate
        boolean modified = false;
        List<GitLabServer> endpoints = new ArrayList<>(getServers());
        for (Iterator<GitLabServer> iterator = endpoints.iterator(); iterator.hasNext(); ) {
            if (serverUrl.equals(iterator.next().getServerUrl())) {
                iterator.remove();
                modified = true;
            }
        }

        // TODO: Use predicate in place of iterator
        // Pass new GitLabServerEqualPredicate()

//        for(GitLabServer endpoint : endpoints) {
//            if(p.test(serverUrl, endpoint)) {
//                endpoints.remove(endpoint);
//                modified = true;
//            }
//        }
//
        setServers(endpoints);
        return modified;
    }

    /**
     * Checks to see if the supplied server URL is defined in the global configuration.
     *
     * @param serverUrl the server url to check.
     * @return the global configuration for the specified server url or {@code null} if not defined.
     */
    @CheckForNull
    public synchronized GitLabServer findServer(@CheckForNull String serverUrl) {
        for (GitLabServer endpoint : getServers()) {
            if (serverUrl.equals(endpoint.getServerUrl())) { // TODO: fix server url NPE, maybe use optional here
                return endpoint;
            }
        }
        return null;
    }

}
