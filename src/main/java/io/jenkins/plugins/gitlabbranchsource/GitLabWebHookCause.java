package io.jenkins.plugins.gitlabbranchsource;

import hudson.triggers.SCMTrigger.SCMTriggerCause;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;

public class GitLabWebHookCause extends SCMTriggerCause {

    private String description;

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
        return this;
    }

    public GitLabWebHookCause fromMergeRequest(MergeRequestEvent mergeRequestEvent) {
        ObjectAttributes objectAttributes = mergeRequestEvent.getObjectAttributes();
        String id = String.valueOf(objectAttributes.getIid());
        String sourceNameSpace = objectAttributes.getSource().getNamespace();
        String targetNameSpace = objectAttributes.getTarget().getNamespace();
        String nameSpace =
            StringUtils.equals(sourceNameSpace, targetNameSpace) ? "" : sourceNameSpace + "/";
        String source = String.format("%s%s", namespace, objectAttributes.getSourceBranch());
        description = Messages.GitLabWebHookCause_ShortDescription_MergeRequestHook(
            id, source, objectAttributes.getTargetBranch());
        return this;
    }

    public GitLabWebHookCause fromTag(TagPushEvent tagPushEvent) {
        String userName = tagPushEvent.getUserName();
        if (StringUtils.isBlank(userName)) {
            description = Messages.GitLabWebHookCause_ShortDescription_Push_noUser();
        } else {
            description = Messages.GitLabWebHookCause_ShortDescription_Push(userName);
        }
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

}
