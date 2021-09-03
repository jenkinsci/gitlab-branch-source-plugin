package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.model.TaskListener;
import java.util.EnumSet;
import java.util.Set;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceContext;

public class GitLabSCMSourceContext
    extends SCMSourceContext<GitLabSCMSourceContext, GitLabSCMSourceRequest> {

    private boolean wantBranches;
    private boolean wantTags;
    private boolean wantOriginMRs;
    private boolean wantForkMRs;

    @NonNull
    private Set<ChangeRequestCheckoutStrategy> originMRStrategies = EnumSet
        .noneOf(ChangeRequestCheckoutStrategy.class);
    @NonNull
    private Set<ChangeRequestCheckoutStrategy> forkMRStrategies = EnumSet
        .noneOf(ChangeRequestCheckoutStrategy.class);
    @NonNull
    private GitLabHookRegistration webhookRegistration = GitLabHookRegistration.SYSTEM;
    @NonNull
    private GitLabHookRegistration systemhookRegistration = GitLabHookRegistration.SYSTEM;

    private boolean notificationsDisabled;

    private boolean logCommentEnabled;

    private String sudoUser = "";

    private boolean logSuccess;

    private boolean mrCommentTriggerEnabled;

    private boolean onlyTrustedMembersCanTrigger;

    private String commentBody = "";

    private boolean projectAvatarDisabled;

    private String buildStatusNameCustomPart = "";

    private boolean alwaysBuildMROpen = true;

    private boolean alwaysBuildMRReOpen = true;

    private boolean alwaysIgnoreMRApprove = false;

    private boolean alwaysIgnoreMRUnApprove = false;

    private boolean alwaysIgnoreNonCodeRelatedUpdates = false;

    public GitLabSCMSourceContext(@CheckForNull SCMSourceCriteria criteria,
        @NonNull SCMHeadObserver observer) {
        super(criteria, observer);
    }

    public final boolean wantBranches() {
        return wantBranches;
    }

    public final boolean wantTags() {
        return wantTags;
    }

    public final boolean wantMRs() {
        return wantOriginMRs || wantForkMRs;
    }

    public final boolean wantOriginMRs() {
        return wantOriginMRs;
    }

    public final boolean wantForkMRs() {
        return wantForkMRs;
    }

    @NonNull
    public final Set<ChangeRequestCheckoutStrategy> originMRStrategies() {
        return originMRStrategies;
    }

    @NonNull
    public final Set<ChangeRequestCheckoutStrategy> forkMRStrategies() {
        return forkMRStrategies;
    }

    @NonNull
    public final GitLabHookRegistration webhookRegistration() {
        return webhookRegistration;
    }

    @NonNull
    public final GitLabHookRegistration systemhookRegistration() {
        return systemhookRegistration;
    }

    public final boolean notificationsDisabled() {
        return notificationsDisabled;
    }

    public final boolean projectAvatarDisabled() {
        return projectAvatarDisabled;
    }

    public final boolean logCommentEnabled() {
        return logCommentEnabled;
    }

    public final String getSudoUser() {
        return sudoUser;
    }

    public boolean doLogSuccess() {
        return logSuccess;
    }

    public final boolean mrCommentTriggerEnabled() {
        return mrCommentTriggerEnabled;
    }

    public final boolean getOnlyTrustedMembersCanTrigger() { return onlyTrustedMembersCanTrigger; }

    public boolean alwaysBuildMROpen() {
        return alwaysBuildMROpen;
    }

    public boolean alwaysBuildMRReOpen() {
        return alwaysBuildMRReOpen;
    }

    public boolean alwaysIgnoreMRApprove() {
        return alwaysIgnoreMRApprove;
    }

    public boolean alwaysIgnoreMRUnApprove() {
        return alwaysIgnoreMRUnApprove;
    }

    public boolean alwaysIgnoreNonCodeRelatedUpdates() {
        return alwaysIgnoreNonCodeRelatedUpdates;
    }

    public final String getCommentBody() {
        return commentBody;
    }

    public final String getBuildStatusNameCustomPart() {
        return buildStatusNameCustomPart;
    }

    @NonNull
    public GitLabSCMSourceContext wantBranches(boolean include) {
        wantBranches = wantBranches || include;
        return this;
    }

    @NonNull
    public GitLabSCMSourceContext wantTags(boolean include) {
        wantTags = wantTags || include;
        return this;
    }

    @NonNull
    public GitLabSCMSourceContext wantOriginMRs(boolean include) {
        wantOriginMRs = wantOriginMRs || include;
        return this;
    }

    @NonNull
    public GitLabSCMSourceContext wantForkMRs(boolean include) {
        wantForkMRs = wantForkMRs || include;
        return this;
    }

    @NonNull
    public GitLabSCMSourceContext withOriginMRStrategies(
        Set<ChangeRequestCheckoutStrategy> strategies) {
        originMRStrategies.addAll(strategies);
        return this;
    }

    @NonNull
    public GitLabSCMSourceContext withForkMRStrategies(
        Set<ChangeRequestCheckoutStrategy> strategies) {
        forkMRStrategies.addAll(strategies);
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext webhookRegistration(GitLabHookRegistration mode) {
        webhookRegistration = mode;
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext systemhookRegistration(GitLabHookRegistration mode) {
        systemhookRegistration = mode;
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext withNotificationsDisabled(boolean disabled) {
        this.notificationsDisabled = disabled;
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext withProjectAvatarDisabled(boolean disabled) {
        this.projectAvatarDisabled = disabled;
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext withLogCommentEnabled(boolean enabled) {
        this.logCommentEnabled = enabled;
        return this;
    }

    public final GitLabSCMSourceContext withMRCommentTriggerEnabled(boolean enabled) {
        this.mrCommentTriggerEnabled = enabled;
        return this;
    }

    public final GitLabSCMSourceContext withOnlyTrustedMembersCanTrigger(boolean enabled) {
        this.onlyTrustedMembersCanTrigger = enabled;
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext withSudoUser(String sudoUser) {
        this.sudoUser = Util.fixNull(sudoUser);
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext withLogSuccess(boolean enabled) {
        this.logSuccess = enabled;
        return this;
    }

    public final GitLabSCMSourceContext withCommentBody(String commentBody) {
        this.commentBody = commentBody;
        return this;
    }

    @NonNull
    public final GitLabSCMSourceContext withBuildStatusNameCustomPart(final String buildStatusNameCustomPart) {
        this.buildStatusNameCustomPart = Util.fixNull(buildStatusNameCustomPart);
        return this;
    }

    @NonNull
    @Override
    public GitLabSCMSourceRequest newRequest(@NonNull SCMSource source,
        @CheckForNull TaskListener listener) {
        return new GitLabSCMSourceRequest(source, this, listener);
    }

    public final GitLabSCMSourceContext withAlwaysBuildMROpen(boolean enabled) {
        this.alwaysBuildMROpen = enabled;
        return this;
    }

    public final GitLabSCMSourceContext withAlwaysBuildMRReOpen(boolean enabled) {
        this.alwaysBuildMRReOpen = enabled;
        return this;
    }

    public final GitLabSCMSourceContext withAlwaysIgnoreMRApprove(boolean enabled) {
        this.alwaysIgnoreMRApprove = enabled;
        return this;
    }

    public final GitLabSCMSourceContext withAlwaysIgnoreMRUnApprove(boolean enabled) {
        this.alwaysIgnoreMRUnApprove = enabled;
        return this;
    }

    public final GitLabSCMSourceContext withAlwaysIgnoreNonCodeRelatedUpdates(boolean enabled) {
        this.alwaysIgnoreNonCodeRelatedUpdates = enabled;
        return this;
    }
}
