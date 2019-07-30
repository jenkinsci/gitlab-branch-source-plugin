package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.scm.SCM;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

public abstract class AbstractGitLabSCMHeadEvent<E> extends SCMHeadEvent<E> {

    public AbstractGitLabSCMHeadEvent(Type type, E createEvent, String origin) {
        super(type, createEvent, origin);
    }

    // TODO: improve check
    @Override
    public boolean isMatch(@NonNull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    public abstract boolean isMatch(@NonNull GitLabSCMNavigator navigator);

    // TODO: improve check
    @Override
    public boolean isMatch(@NonNull SCMSource source) {
        return source instanceof GitLabSCMSource && isMatch((GitLabSCMSource) source);
    }

    public abstract boolean isMatch(@NonNull GitLabSCMSource source);

    // TODO: improve check
    @Nonnull
    @Override
    public final Map<SCMHead, SCMRevision> heads(@Nonnull SCMSource source) {
        Map<SCMHead, SCMRevision> heads = new HashMap<>();
        if (source instanceof GitLabSCMSource) {
            return headsFor((GitLabSCMSource) source);
        }
        return heads;
    }

    @Override
    public boolean isMatch(@NonNull SCM scm) {
        return false;
    }

    @NonNull
    protected abstract Map<SCMHead, SCMRevision> headsFor(GitLabSCMSource source);

}
