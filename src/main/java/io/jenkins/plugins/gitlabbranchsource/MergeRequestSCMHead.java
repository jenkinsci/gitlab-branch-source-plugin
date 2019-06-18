package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;

public class MergeRequestSCMHead extends SCMHead implements ChangeRequestSCMHead2 {
    private final long id;
    private final BranchSCMHead target;
    private final ChangeRequestCheckoutStrategy strategy;
    private final String originName;
    private final String originOwner;
    private String originProject;
    private final SCMHeadOrigin origin;

    /**
     * Constructor.
     *
     * @param id               the merge request id.
     * @param name             the name of the head.
     * @param target           the target of this merge request.
     * @param strategy         the checkout strategy
     * @param origin           the origin of the merge request
     * @param originOwner      the name of the owner of the origin project
     * @param originProject the name of the origin project
     * @param originName       the name of the branch in the origin project
     */
    public MergeRequestSCMHead(@NonNull String name, long id, BranchSCMHead target,
                              ChangeRequestCheckoutStrategy strategy, SCMHeadOrigin origin, String originOwner,
                              String originProject, String originName) {
        super(name);
        this.id = id;
        this.target = target;
        this.strategy = strategy;
        this.origin = origin;
        this.originOwner = originOwner;
        this.originProject = originProject;
        this.originName = originName;
    }

    @Override
    public String getPronoun() {
        return Messages.MergeRequestSCMHead_Pronoun();
    }

    @NonNull
    @Override
    public ChangeRequestCheckoutStrategy getCheckoutStrategy() {
        return strategy;
    }

    @NonNull
    @Override
    public String getOriginName() {
        return originName;
    }

    @NonNull
    @Override
    public String getId() {
        return Long.toString(id);
    }

    @NonNull
    @Override
    public BranchSCMHead getTarget() {
        return target;
    }

    @NonNull
    @Override
    public SCMHeadOrigin getOrigin() {
        return origin;
    }

    public String getOriginOwner() {
        return originOwner;
    }

    public String getOriginProject() {
        return originProject;
    }

}
