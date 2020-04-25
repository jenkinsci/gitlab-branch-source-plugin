package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabTagPushCauseData {

    private Map<String, String> variables = new HashMap<>();

    public GitLabTagPushCauseData(TagPushEvent tagPushEvent) {
        this.variables.put("triggeredBy", tagPushEvent.getUserName());
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        return variables;
    }
}
