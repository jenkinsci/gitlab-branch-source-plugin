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
    private final SCMHeadOrigin origin;
    private String originProjectPath;
    private String title;

    /**
     * Constructor.
     *
     * @param id the merge request id.
     * @param name the name of the head.
     * @param target the target of this merge request.
     * @param strategy the checkout strategy
     * @param origin the origin of the merge request
     * @param originOwner the name of the owner of the origin project
     * @param originProjectPath the name of the origin project path
     * @param originName the name of the branch in the origin project
     * @param title the title of the merge request
     */
    public MergeRequestSCMHead(@NonNull String name, long id, BranchSCMHead target,
        ChangeRequestCheckoutStrategy strategy, SCMHeadOrigin origin, String originOwner,
        String originProjectPath, String originName, String title) {
        super(name);
        this.id = id;
        this.target = target;
        this.strategy = strategy;
        this.origin = origin;
        this.originOwner = originOwner;
        this.originProjectPath = originProjectPath;
        this.originName = originName;
        this.title = title;
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

    public String getOriginProjectPath() {
        return originProjectPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
