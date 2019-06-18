package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.ChangeRequestSCMRevision;
import org.kohsuke.stapler.export.Exported;

public class MergeRequestSCMRevision extends ChangeRequestSCMRevision<MergeRequestSCMHead> {

    private BranchSCMRevision origin;

    /**
     * Constructor.
     *
     * @param head   the {@link MergeRequestSCMHead} that the {@link SCMRevision} belongs to.
     * @param target the {@link BranchSCMRevision} of the {@link MergeRequestSCMHead#getTarget()}.
     * @param origin the {@link BranchSCMRevision} of the {@link MergeRequestSCMHead#getOrigin()} head.
     */
    protected MergeRequestSCMRevision(
            @NonNull MergeRequestSCMHead head,
            @NonNull BranchSCMRevision target,
            @NonNull BranchSCMRevision origin) {
        super(head, target);
        this.origin = origin;
    }

    @Exported
    @NonNull
    public final BranchSCMRevision getOrigin() {
        return origin;
    }

    @Override
    public boolean equivalent(ChangeRequestSCMRevision<?> revision) {
        return (revision instanceof MergeRequestSCMRevision)
                && origin.equals(((MergeRequestSCMRevision) revision).getOrigin());
    }

    @Override
    protected int _hashCode() {
        return origin.hashCode();
    }

    @Override
    public String toString() {
        return (isMerge() ? ((BranchSCMRevision) getTarget()).getHash() + "+" : "") + origin.getHash();
    }
}
