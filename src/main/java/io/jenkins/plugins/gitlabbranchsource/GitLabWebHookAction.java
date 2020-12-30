package io.jenkins.plugins.gitlabbranchsource;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import hudson.util.HttpResponses;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jenkins.scm.api.SCMEvent;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.webhook.WebHookManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public final class GitLabWebHookAction extends CrumbExclusion implements UnprotectedRootAction {

    public static final Logger LOGGER = Logger.getLogger(GitLabWebHookAction.class.getName());

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "gitlab-webhook";
    }

    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
        throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/" + getUrlName() + "/post")) {
            chain.doFilter(req, resp);
            return true;
        }
        return false;
    }

    public HttpResponse doPost(StaplerRequest request) throws IOException, GitLabApiException {
        if (!request.getMethod().equals("POST")) {
            return HttpResponses
                .error(HttpServletResponse.SC_BAD_REQUEST,
                    "Only POST requests are supported, this was a " + request.getMethod()
                        + " request");
        }
        if (!"application/json".equals(request.getContentType())) {
            return HttpResponses
                .error(HttpServletResponse.SC_BAD_REQUEST,
                    "Only application/json content is supported, this was " + request
                        .getContentType());
        }
        String type = request.getHeader("X-Gitlab-Event");
        if (StringUtils.isBlank(type)) {
            return HttpResponses.error(HttpServletResponse.SC_BAD_REQUEST,
                "Expecting a GitLab event, missing expected X-Gitlab-Event header");
        }
        String secretToken = request.getHeader("X-Gitlab-Token");
        if(!isValidToken(secretToken)) {
            return HttpResponses.error(HttpServletResponse.SC_UNAUTHORIZED,
                "Expecting a valid secret token");
        }
        String origin = SCMEvent.originOf(request);
        WebHookManager webHookManager = new WebHookManager();
        webHookManager.addListener(new GitLabWebHookListener(origin));
        webHookManager.handleEvent(request);
        return HttpResponses.ok(); // TODO find a better response
    }

    private boolean isValidToken(String secretToken) {
        try {
            List<GitLabServer> servers = GitLabServers.get().getServers();
            for(GitLabServer server: servers) {
                if(server.getSecretTokenAsPlainText().equals(secretToken) || (server.getSecretTokenAsPlainText().isEmpty() && secretToken == null)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error while validating token: %s", e.getMessage()));
        }
        return false;
    }
}
