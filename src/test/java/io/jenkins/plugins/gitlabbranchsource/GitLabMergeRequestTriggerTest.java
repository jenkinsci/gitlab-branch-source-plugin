package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.models.utils.JacksonJson;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitLabMergeRequestTriggerTest {

    private static MergeRequestEvent retargetEvent() throws Exception {
        try (Reader in = new InputStreamReader(
                GitLabMergeRequestTriggerTest.class.getResourceAsStream("mr-retarget-event.json"),
                StandardCharsets.UTF_8)) {
            return new JacksonJson().unmarshal(MergeRequestEvent.class, in);
        }
    }

    private static MergeRequestEvent titleUpdateEvent() throws Exception {
        MergeRequestEvent event = retargetEvent();
        event.getChanges().any().remove("target_branch");
        return event;
    }

    private static boolean isMatch(MergeRequestEvent event, WebhookListenerBuildConditionsTrait trait) {
        GitLabSCMSource source =
                new GitLabSCMSourceBuilder("id", "server", "creds", "group", "group/webapp", "webapp").build();
        source.setProjectId(42L);
        source.setTraits(List.of(trait));
        return new GitLabMergeRequestTrigger(event, "test").isMatch(source);
    }

    @Test
    void retargetIsProcessedByDefault(JenkinsRule j) throws Exception {
        assertTrue(isMatch(retargetEvent(), new WebhookListenerBuildConditionsTrait()));
    }

    @Test
    void retargetIsProcessedWhenIgnoringNonCodeRelatedUpdates(JenkinsRule j) throws Exception {
        WebhookListenerBuildConditionsTrait trait = new WebhookListenerBuildConditionsTrait();
        trait.setAlwaysIgnoreNonCodeRelatedUpdates(true);
        assertTrue(isMatch(retargetEvent(), trait));
    }

    @Test
    void retargetIsIgnoredWhenIgnoringRetargets(JenkinsRule j) throws Exception {
        WebhookListenerBuildConditionsTrait trait = new WebhookListenerBuildConditionsTrait();
        trait.setAlwaysIgnoreMRRetarget(true);
        assertFalse(isMatch(retargetEvent(), trait));
    }

    @Test
    void nonCodeRelatedUpdateIsStillIgnored(JenkinsRule j) throws Exception {
        WebhookListenerBuildConditionsTrait trait = new WebhookListenerBuildConditionsTrait();
        trait.setAlwaysIgnoreNonCodeRelatedUpdates(true);
        assertFalse(isMatch(titleUpdateEvent(), trait));
    }

    @Test
    void ignoringRetargetsDoesNotAffectOtherUpdates(JenkinsRule j) throws Exception {
        WebhookListenerBuildConditionsTrait trait = new WebhookListenerBuildConditionsTrait();
        trait.setAlwaysIgnoreMRRetarget(true);
        assertTrue(isMatch(titleUpdateEvent(), trait));
    }
}
