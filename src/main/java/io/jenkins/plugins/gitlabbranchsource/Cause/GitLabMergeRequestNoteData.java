package io.jenkins.plugins.gitlabbranchsource.Cause;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.HashMap;
import java.util.Map;
import org.gitlab4j.api.webhook.NoteEvent;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabMergeRequestNoteData {

    private final Map<String, String> variables = new HashMap<>();

    public GitLabMergeRequestNoteData(NoteEvent noteEvent) {
        this.variables.put("GITLAB_OBJECT_KIND", defaultString(NoteEvent.OBJECT_KIND));
        this.variables.put(
                "GITLAB_COMMENT_TRIGGER",
                defaultString(noteEvent.getObjectAttributes().getNote()));
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        return variables;
    }
}
