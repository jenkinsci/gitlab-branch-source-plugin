package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.scm.SCM;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.gitlab4j.api.webhook.AbstractPushEvent;

public abstract class AbstractGitLabSCMHeadEvent<E> extends SCMHeadEvent<E> {

    private static final Logger LOGGER = Logger.getLogger(AbstractGitLabSCMHeadEvent.class.getName());

    private static final Pattern NONE_HASH_PATTERN = Pattern.compile("^0+$");

    static <E extends AbstractPushEvent> Type typeOf(E pushEvent) {
        Type result;
        boolean hasBefore = isPresent(pushEvent.getBefore());
        boolean hasAfter = isPresent(pushEvent.getAfter());
        if (hasBefore && hasAfter) {
            result = Type.UPDATED;
        } else if (hasAfter) {
            result = Type.CREATED;
        } else if (hasBefore) {
            result = Type.REMOVED;
        } else {
            LOGGER.warning("Received push event with both \"before\" and \"after\" set to non-existing revision. Assuming removal.");
            result = Type.REMOVED;
        }
        return result;
    }

    private static boolean isPresent(String ref) {
        return !(NONE_HASH_PATTERN.matcher(ref).matches());
    }

    public AbstractGitLabSCMHeadEvent(Type type, E createEvent, String origin) {
        super(type, createEvent, origin);
    }

    @Override
    public boolean isMatch(@NonNull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    public abstract boolean isMatch(@NonNull GitLabSCMNavigator navigator);

    @Override
    public boolean isMatch(@NonNull SCMSource source) {
        return source instanceof GitLabSCMSource && isMatch((GitLabSCMSource) source);
    }

    public abstract boolean isMatch(@NonNull GitLabSCMSource source);

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

    public abstract GitLabWebHookCause getCause();

}
