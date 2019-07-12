package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.plugins.git.GitTagSCMHead;
import jenkins.scm.api.mixin.TagSCMHead;

public class GitLabTagSCMHead extends GitTagSCMHead implements TagSCMHead {
    /**
     * Constructor.
     *
     * @param name      the name.
     * @param timestamp the tag timestamp;
     */
    public GitLabTagSCMHead(@NonNull String name, long timestamp) {
        super(name, timestamp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPronoun() {
        return Messages.GitLabTagSCMHead_Pronoun();
    }
}
