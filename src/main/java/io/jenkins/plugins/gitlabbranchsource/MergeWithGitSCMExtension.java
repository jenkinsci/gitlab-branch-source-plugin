package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ObjectStreamException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Retained for data migration.
 *
 * @deprecated use {@link jenkins.plugins.git.MergeWithGitSCMExtension}
 */
@Deprecated
@Restricted(NoExternalUse.class)
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class MergeWithGitSCMExtension extends jenkins.plugins.git.MergeWithGitSCMExtension {

    MergeWithGitSCMExtension(@NonNull String baseName, @CheckForNull String baseHash) {
        super(baseName, baseHash);
    }

    private Object readResolve() throws ObjectStreamException {
        return new jenkins.plugins.git.MergeWithGitSCMExtension(getBaseName(), getBaseHash());
    }
}
