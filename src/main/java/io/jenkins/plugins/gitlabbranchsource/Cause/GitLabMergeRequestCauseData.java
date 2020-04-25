package io.jenkins.plugins.gitlabbranchsource.Cause;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabMergeRequestCauseData {

    MergeRequestEvent mergeRequestEvent;

    public GitLabMergeRequestCauseData(MergeRequestEvent mergeRequestEvent) {
        this.mergeRequestEvent = mergeRequestEvent;
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("triggeredBy", mergeRequestEvent.getUser().getName());
        return variables;
    }
}
