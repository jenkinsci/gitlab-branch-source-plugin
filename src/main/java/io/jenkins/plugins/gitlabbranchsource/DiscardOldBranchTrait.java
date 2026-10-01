package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.time.LocalDate;
import java.time.ZoneOffset;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.gitlab4j.api.models.Branch;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Discard all branches with head commit older than the configured days.
 */
public class DiscardOldBranchTrait extends SCMSourceTrait {

    private int keepForDays = 1;

    @DataBoundConstructor
    public DiscardOldBranchTrait(int keepForDays) {
        this.keepForDays = keepForDays;
    }

    public int getKeepForDays() {
        return keepForDays;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        context.withFilter(new ExcludeOldSCMHeadBranch(keepForDays));
    }

    static final class ExcludeOldSCMHeadBranch extends SCMHeadFilter {

        private final int keepForDays;

        public ExcludeOldSCMHeadBranch(int keepForDays) {
            this.keepForDays = keepForDays;
        }

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head) {
            GitLabSCMSourceRequest glRequest = (GitLabSCMSourceRequest) request;
            String branchName = head.getName();
            if (head instanceof MergeRequestSCMHead mrHead) {
                branchName = mrHead.getOriginName();
            }

            for (Branch branch : glRequest.getBranches()) {
                if (branchName.equals(branch.getName())) {
                    LocalDate commitDate = LocalDate.ofInstant(
                            branch.getCommit().getCommittedDate().toInstant(), ZoneOffset.UTC);
                    LocalDate expiryDate = LocalDate.now(ZoneOffset.UTC).minusDays(keepForDays);
                    return commitDate.isBefore(expiryDate);
                }
            }
            return false;
        }
    }

    @Symbol("gitLabDiscardOldBranch")
    @Extension
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.DiscardOldBranchTrait_displayName();
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }
    }
}
