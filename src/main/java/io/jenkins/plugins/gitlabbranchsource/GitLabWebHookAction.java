package io.jenkins.plugins.gitlabbranchsource;

import static java.nio.charset.StandardCharsets.UTF_8;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import hudson.util.HttpResponses;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMEvent;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.WebHookManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;

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

    public HttpResponse doPost(StaplerRequest2 request) throws IOException, GitLabApiException {
        if (!request.getMethod().equals("POST")) {
            return HttpResponses.error(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Only POST requests are supported, this was a " + request.getMethod() + " request");
        }
        if (!"application/json".equals(request.getContentType())) {
            return HttpResponses.error(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Only application/json content is supported, this was " + request.getContentType());
        }
        String type = request.getHeader("X-Gitlab-Event");
        if (StringUtils.isBlank(type)) {
            return HttpResponses.error(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Expecting a GitLab event, missing expected X-Gitlab-Event header");
        }
        String secretToken = request.getHeader("X-Gitlab-Token");
        if (!isValidToken(secretToken)) {
            return HttpResponses.error(HttpServletResponse.SC_UNAUTHORIZED, "Expecting a valid secret token");
        }
        String origin = SCMEvent.originOf(request);
        WebHookManager webHookManager = new WebHookManager();
        webHookManager.addListener(new GitLabWebHookListener(origin));
        webHookManager.handleEvent(request);
        return HttpResponses.ok(); // TODO find a better response
    }

    @SuppressFBWarnings(
            value = "NP_NULL_PARAM_DEREF",
            justification = "MessageDigest.isEqual does handle null and spotbugs is wrong")
    private boolean isValidToken(String secretToken) {
        try {
            List<GitLabServer> servers = GitLabServers.get().getServers();
            byte[] secretTokenBytes = secretToken != null ? secretToken.getBytes(UTF_8) : null;
            for (GitLabServer server : servers) {
                String secretTokenAsPlainText = server.getSecretTokenAsPlainText();
                byte[] secretTokenAsPlainTextBytes =
                        secretTokenAsPlainText != null ? secretTokenAsPlainText.getBytes(UTF_8) : null;
                if (MessageDigest.isEqual(secretTokenBytes, secretTokenAsPlainTextBytes)
                        || (secretTokenAsPlainText != null
                                && secretTokenAsPlainText.isEmpty()
                                && secretToken == null)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Error while validating token: %s", e.getMessage()));
        }
        return false;
    }
}
