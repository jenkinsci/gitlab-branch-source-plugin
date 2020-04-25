package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.PushEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabPushCauseData {

    PushEvent pushEvent;

    public GitLabPushCauseData(PushEvent pushEvent) {
        this.pushEvent = pushEvent;
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("triggeredBy", pushEvent.getUserName());
        return variables;
    }
}
