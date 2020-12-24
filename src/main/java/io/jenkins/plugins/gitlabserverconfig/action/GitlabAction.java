package io.jenkins.plugins.gitlabserverconfig.action;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.util.HttpResponses;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Provide an API for Jenkins integration purpose
 */
@Extension
@Restricted(NoExternalUse.class)
public class GitlabAction implements RootAction {
    private static final Logger LOGGER = Logger.getLogger(GitlabAction.class.getName());

    @RequirePOST
    public HttpResponse doServerList() {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return HttpResponses.errorJSON("no permission to get Gitlab server list");
        }

        JSONArray servers = new JSONArray();
        GitLabServers.get().getServers().forEach(server -> {
            JSONObject serverObj = new JSONObject();
            serverObj.put("name", server.getName());
            serverObj.put("url", server.getServerUrl());
            servers.add(serverObj);
        });

        return HttpResponses.okJSON(servers);
    }

    @RequirePOST
    public HttpResponse doProjectList(@QueryParameter String server,
        @QueryParameter String owner) {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return HttpResponses.errorJSON("no permission to get Gitlab server list");
        }

        if (StringUtils.isEmpty(server) || StringUtils.isEmpty(owner)) {
            return HttpResponses.errorJSON("server or owner is empty");
        }

        JSONArray servers = new JSONArray();

        GitLabApi gitLabApi = GitLabHelper.apiBuilder(server);
        try {
            for (Project project : gitLabApi.getProjectApi().getUserProjects(owner,
                new ProjectFilter().withOwned(true))) {
                servers.add(project.getPathWithNamespace());
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.FINE, String.format("errors when get projects from %s/%s as a user", server, owner), e);
        }

        try {
            for (Project project : gitLabApi.getGroupApi().getProjects(owner)) {
                servers.add(project.getPathWithNamespace());
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.FINE, String.format("errors when get projects from %s/%s as a group", server, owner), e);
        }

        return HttpResponses.okJSON(servers);
    }


    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "/gitlab";
    }
}
