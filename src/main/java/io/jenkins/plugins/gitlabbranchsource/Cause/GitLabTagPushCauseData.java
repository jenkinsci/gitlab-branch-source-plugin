package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.TagPushEvent;

public class GitLabTagPushCauseData {

    TagPushEvent tagPushEvent;

    public GitLabTagPushCauseData(TagPushEvent tagPushEvent) {
        this.tagPushEvent = tagPushEvent;
    }

    public Map<String, String> getBuildVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("triggeredBy", tagPushEvent.getUserName());
        return variables;
    }
}
