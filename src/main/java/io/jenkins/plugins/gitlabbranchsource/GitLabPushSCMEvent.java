package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.gitlab4j.api.webhook.PushEvent;

public class GitLabPushSCMEvent extends AbstractGitLabSCMHeadEvent<PushEvent> {

    static final Logger LOGGER = Logger.getLogger(GitLabPushSCMEvent.class.getName());

    public GitLabPushSCMEvent(PushEvent pushEvent, String origin) {
        super(typeOf(pushEvent), pushEvent, origin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String descriptionFor(@NonNull SCMNavigator navigator) {
        return description();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getSourceName() {
        return getPayload().getProject().getPathWithNamespace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String descriptionFor(SCMSource source) {
        String ref = getPayload().getRef();
        ref = ref.startsWith(Constants.R_HEADS) ? ref.substring(Constants.R_HEADS.length()) : ref;
        return "Push event to branch " + ref;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String description() {
        String ref = getPayload().getRef();
        ref = ref.startsWith(Constants.R_HEADS) ? ref.substring(Constants.R_HEADS.length()) : ref;
        return "Push event to branch " + ref + " in project " +
            getPayload().getProject().getPathWithNamespace();
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMNavigator navigator) {
        return navigator.getNavigatorProjects()
            .contains(getPayload().getProject().getPathWithNamespace());
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMSource source) {
        try {
            return getPayload().getProject().getId().equals(source.getProjectId());
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Can't get an id from your project webhook object, using project path");
            LOGGER.log(Level.WARNING, "--> project path {0} ", getPayload().getProject().getPathWithNamespace());
            LOGGER.log(Level.WARNING, "--> source path {0} ", source.getProjectPath());
            return getPayload().getProject().getPathWithNamespace().equals(source.getProjectPath());
        }    }

    @NonNull
    @Override
    public Map<SCMHead, SCMRevision> headsFor(GitLabSCMSource source) {
        String ref = getPayload().getRef();
        ref = ref.startsWith(Constants.R_HEADS) ? ref.substring(Constants.R_HEADS.length()) : ref;
        BranchSCMHead h = new BranchSCMHead(ref);
        return Collections.<SCMHead, SCMRevision>singletonMap(h,
            StringUtils.isNotBlank(getPayload().getAfter())
                ? new BranchSCMRevision(h, getPayload().getAfter()) : null);
    }

    @Override
    public GitLabWebHookCause getCause() {
        return new GitLabWebHookCause().fromPush(getPayload());
    }

}
