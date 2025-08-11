package io.jenkins.plugins.gitlabbranchsource;

import hudson.triggers.SCMTrigger.SCMTriggerCause;
import io.jenkins.plugins.gitlabbranchsource.Cause.GitLabMergeRequestCauseData;
import io.jenkins.plugins.gitlabbranchsource.Cause.GitLabPushCauseData;
import io.jenkins.plugins.gitlabbranchsource.Cause.GitLabTagPushCauseData;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;

public class GitLabWebHookCause extends SCMTriggerCause {

    private String description;
    // possible NPEs
    private GitLabPushCauseData gitLabPushCauseData;
    private GitLabMergeRequestCauseData gitLabMergeRequestCauseData;
    private GitLabTagPushCauseData gitLabTagPushCauseData;

    public GitLabWebHookCause() {
        super("");
    }

    public GitLabWebHookCause fromPush(PushEvent pushEvent) {
        String userName = pushEvent.getUserName();
        if (StringUtils.isBlank(userName)) {
            description = Messages.GitLabWebHookCause_ShortDescription_Push_noUser();
        } else {
            description = Messages.GitLabWebHookCause_ShortDescription_Push(userName);
        }
        this.gitLabPushCauseData = new GitLabPushCauseData(pushEvent);
        return this;
    }

    public GitLabWebHookCause fromMergeRequest(MergeRequestEvent mergeRequestEvent) {
        ObjectAttributes objectAttributes = mergeRequestEvent.getObjectAttributes();
        String id = String.valueOf(objectAttributes.getIid());
        String sourceNameSpace = objectAttributes.getSource().getNamespace();
        String targetNameSpace = objectAttributes.getTarget().getNamespace();
        String nameSpace = StringUtils.equals(sourceNameSpace, targetNameSpace) ? "" : sourceNameSpace + "/";
        String source = String.format("%s%s", nameSpace, objectAttributes.getSourceBranch());
        description = Messages.GitLabWebHookCause_ShortDescription_MergeRequestHook(
                id, source, objectAttributes.getTargetBranch());
        this.gitLabMergeRequestCauseData = new GitLabMergeRequestCauseData(mergeRequestEvent);
        return this;
    }

    public GitLabWebHookCause fromTag(TagPushEvent tagPushEvent) {
        String userName = tagPushEvent.getUserName();
        if (StringUtils.isBlank(userName)) {
            description = Messages.GitLabWebHookCause_ShortDescription_Push_noUser();
        } else {
            description = Messages.GitLabWebHookCause_ShortDescription_Push(userName);
        }
        this.gitLabTagPushCauseData = new GitLabTagPushCauseData(tagPushEvent);
        return this;
    }

    @Override
    public String getShortDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        return o instanceof GitLabWebHookCause;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public GitLabPushCauseData getGitLabPushCauseData() {
        return gitLabPushCauseData;
    }

    public GitLabMergeRequestCauseData getGitLabMergeRequestCauseData() {
        return gitLabMergeRequestCauseData;
    }

    public GitLabTagPushCauseData getGitLabTagPushCauseData() {
        return gitLabTagPushCauseData;
    }
}
