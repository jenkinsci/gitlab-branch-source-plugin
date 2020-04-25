package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.PushEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabPushCauseData {

    private Map<String, String> variables = new HashMap<>();

    public GitLabPushCauseData(PushEvent pushEvent) {
        this.variables.put("triggeredBy", pushEvent.getUserName());
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        return variables;
    }
}
