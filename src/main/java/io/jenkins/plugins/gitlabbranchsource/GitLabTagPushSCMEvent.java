package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Map;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.eclipse.jgit.lib.Constants;
import org.gitlab4j.api.webhook.TagPushEvent;

import static jenkins.scm.api.SCMEvent.Type.CREATED;
import static jenkins.scm.api.SCMEvent.Type.REMOVED;

public class GitLabTagPushSCMEvent extends AbstractGitLabSCMHeadEvent<TagPushEvent> {

    private static final String NONE_HASH_PATTERN = "^0+$";

    public GitLabTagPushSCMEvent(TagPushEvent tagPushEvent, String origin) {
        super(typeOf(tagPushEvent), tagPushEvent, origin);
    }

    private static Type typeOf(TagPushEvent tagPushEvent) {
        if (tagPushEvent.getAfter().matches(NONE_HASH_PATTERN)) {
            return REMOVED;
        }
        return Type.CREATED;
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
        ref = ref.startsWith(Constants.R_TAGS) ? ref.substring(Constants.R_TAGS.length()) : ref;
        return "Tag push event of tag " + ref + " in project " + getPayload().getProject()
            .getPathWithNamespace();
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMNavigator navigator) {
        return navigator.getNavigatorProjects()
            .contains(getPayload().getProject().getPathWithNamespace());
    }

    @Override
    public boolean isMatch(@NonNull GitLabSCMSource source) {
        return getPayload().getProject().getId().equals(source.getProjectId());
    }

    @NonNull
    @Override
    public Map<SCMHead, SCMRevision> headsFor(GitLabSCMSource source) {
        String ref = getPayload().getRef();
        ref = ref.startsWith(Constants.R_TAGS) ? ref.substring(Constants.R_TAGS.length()) : ref;
        long time = 0L;
        if (getType() == CREATED) {
            time = getPayload().getCommits().get(0).getTimestamp().getTime();
        }
        GitLabTagSCMHead h = new GitLabTagSCMHead(ref, time);
        String hash = getPayload().getCheckoutSha();
        return Collections.<SCMHead, SCMRevision>singletonMap(h,
            (getType() == CREATED)
                ? new GitTagSCMRevision(h, hash) : null);
    }

}
