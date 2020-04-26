package io.jenkins.plugins.gitlabbranchsource.environment;

import hudson.model.EnvironmentContributor;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceBuilder;
import java.util.logging.Logger;
import jenkins.branch.BranchNameContributor;
import jenkins.branch.BranchSource;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class GitLabWebHookEnvironmentContributorTest {
    private static final Logger LOGGER = Logger.getLogger(GitLabWebHookEnvironmentContributorTest.class.getName());
    private static final String PROJECT_NAME = "project";
    private static final String SOURCE_ID = "id";

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Before
    public void cleanOutAllItems() throws Exception {
        for (TopLevelItem i : j.getInstance().getItems()) {
            i.delete();
        }
    }

    @Test
    public void buildEnvironmentFor() throws Exception {
        BranchNameContributor instance =
            j.jenkins.getExtensionList(EnvironmentContributor.class)
                .get(BranchNameContributor.class);
        assertThat("The extension is registered", instance, notNullValue());
        GitLabSCMSourceBuilder sb = new GitLabSCMSourceBuilder(SOURCE_ID, "server", "creds", "po", "group/project", "project");
        WorkflowMultiBranchProject project = j.createProject(WorkflowMultiBranchProject.class, PROJECT_NAME);
//        Event pushEvent = JSONUtils.unmarshalResource(PushEvent.class, "push-event.json");
//        GitLabWebHookCause gitLabWebHookCause = new GitLabWebHookCause().fromPush()
        project.getSourcesList().add(new BranchSource(sb.build()));
        project.scheduleBuild2(0 ).getFuture().get();
    }

}
