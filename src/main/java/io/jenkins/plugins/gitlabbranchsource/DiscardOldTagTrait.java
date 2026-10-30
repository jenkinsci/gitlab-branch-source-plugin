package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.time.LocalDate;
import java.time.ZoneOffset;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.TagSCMHead;
import jenkins.scm.api.trait.SCMHeadPrefilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Discard all tags with creation date older than the configured days.
 */
public class DiscardOldTagTrait extends SCMSourceTrait {

    private int keepForDays = 1;

    @DataBoundConstructor
    public DiscardOldTagTrait(int keepForDays) {
        this.keepForDays = keepForDays;
    }

    public int getKeepForDays() {
        return keepForDays;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        context.withPrefilter(new ExcludeOldSCMTag(keepForDays));
    }

    static final class ExcludeOldSCMTag extends SCMHeadPrefilter {

        private final int keepForDays;

        public ExcludeOldSCMTag(int keepForDays) {
            this.keepForDays = keepForDays;
        }

        @Override
        public boolean isExcluded(@NonNull SCMSource source, @NonNull SCMHead head) {
            if (!(head instanceof TagSCMHead tagHead) || tagHead.getTimestamp() == 0) {
                return false;
            }

            LocalDate commitDate = asLocalDate(tagHead.getTimestamp());
            LocalDate expiryDate = LocalDate.now(ZoneOffset.UTC).minusDays(keepForDays);
            return commitDate.isBefore(expiryDate);
        }

        @NonNull
        private LocalDate asLocalDate(long milliseconds) {
            return new java.sql.Date(milliseconds).toLocalDate();
        }
    }

    @Symbol("gitLabDiscardOldTag")
    @Extension
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.DiscardOldTagTrait_displayName();
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }
    }
}
