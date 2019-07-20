package io.jenkins.plugins.gitlabbranchsource;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jenkins.scm.api.SCMEvent;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public final class GitLabWebhookAction extends CrumbExclusion implements UnprotectedRootAction {

    public static final Logger LOGGER = Logger.getLogger(GitLabWebhookAction.class.getName());

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

    public HttpResponse doPost(StaplerRequest request) throws IOException {
        if (!request.getMethod().equals("POST")) {
            return HttpResponses
                    .error(HttpServletResponse.SC_BAD_REQUEST,
                            "Only POST requests are supported, this was a " + request.getMethod() + " request");
        }
        if (!"application/json".equals(request.getContentType())) {
            return HttpResponses
                    .error(HttpServletResponse.SC_BAD_REQUEST,
                            "Only application/json content is supported, this was " + request.getContentType());
        }
        String type = request.getHeader("X-Gitlab-Event");
        if (StringUtils.isBlank(type)) {
            return HttpResponses.error(HttpServletResponse.SC_BAD_REQUEST,
                    "Expecting a GitLab event, missing expected X-Gitlab-Event header");
        }
        String origin = SCMEvent.originOf(request);
        boolean processed = false;
//        for (GiteaWebhookHandler<?, ?> h : ExtensionList.lookup(GiteaWebhookHandler.class)) {
//            if (h.matches(type)) {
//                h.process(request.getInputStream(), origin);
//                processed = true;
//            }
//        }
        LOGGER.info("ORIGIN: "+ origin);
        LOGGER.info("Request: "+request.getInputStream().toString());
        return HttpResponses.text(processed ? "Processed" : "Ignored");
    }
}
