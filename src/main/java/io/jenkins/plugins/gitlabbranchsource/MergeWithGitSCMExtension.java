package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.PreBuildMerge;
import hudson.plugins.git.util.MergeRecord;
import java.io.IOException;
import java.io.ObjectStreamException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.CheckoutCommand;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.MergeCommand;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Retained for data migration.
 *
 * @deprecated use {@link jenkins.plugins.git.MergeWithGitSCMExtension}
 */
@Deprecated
@Restricted(DoNotUse.class)
public class MergeWithGitSCMExtension extends jenkins.plugins.git.MergeWithGitSCMExtension {

    MergeWithGitSCMExtension(@NonNull String baseName, @CheckForNull String baseHash) {
        super(baseName, baseHash);
    }

    private Object readResolve() throws ObjectStreamException {
        return new jenkins.plugins.git.MergeWithGitSCMExtension(getBaseName(), getBaseHash());
    }

}
