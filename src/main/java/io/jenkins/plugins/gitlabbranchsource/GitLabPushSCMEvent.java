package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Map;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.gitlab4j.api.webhook.PushEvent;

public class GitLabPushSCMEvent extends AbstractGitLabSCMHeadEvent<PushEvent> {

    public GitLabPushSCMEvent(PushEvent pushEvent, String origin) {
        super(typeOf(pushEvent), pushEvent, origin);
    }

    private static Type typeOf(PushEvent pushEvent) {
        if (!pushEvent.getCommits().get(0).getAdded().isEmpty()) {
            return Type.CREATED;
        }
        if (!pushEvent.getCommits().get(0).getRemoved().isEmpty()) {
            return Type.REMOVED;
        }
        return Type.UPDATED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String descriptionFor(@NonNull SCMNavigator navigator) {
        String ref = getPayload().getRef();
        ref = ref.startsWith(Constants.R_HEADS) ? ref.substring(Constants.R_HEADS.length()) : ref;
        return "Push event to branch " + ref + " in project " + getPayload().getRepository().getName();
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
        return navigator.getNavigatorProjects().contains(getPayload().getProject().getPathWithNamespace());
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMSource source) {
        return getPayload().getProject().getId().equals(source.getProjectId());
    }

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
}
