package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabMergeRequestCauseData {

    private Map<String, String> variables = new HashMap<>();

    public GitLabMergeRequestCauseData(MergeRequestEvent mergeRequestEvent) {
        this.variables.put("triggeredBy", mergeRequestEvent.getUser().getName());
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        return variables;
    }
}
