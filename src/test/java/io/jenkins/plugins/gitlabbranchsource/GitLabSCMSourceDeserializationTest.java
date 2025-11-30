package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import jenkins.branch.BranchSource;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitLabSCMSourceDeserializationTest {

    private static final String PROJECT_NAME = "project";
    private static final String SOURCE_ID = "id";

    @Test
    void afterRestartingJenkinsTransientFieldsAreNotNull(JenkinsRule j) throws Throwable {
        GitLabSCMSourceBuilder sb =
                new GitLabSCMSourceBuilder(SOURCE_ID, "server", "creds", "po", "group/project", "project");
        WorkflowMultiBranchProject project = j.createProject(WorkflowMultiBranchProject.class, PROJECT_NAME);
        project.getSourcesList().add(new BranchSource(sb.build()));

        j.restart();

        SCMSource source = j.getInstance().getAllItems(WorkflowMultiBranchProject.class).stream()
                .filter(p -> PROJECT_NAME.equals(p.getName()))
                .map(p -> p.getSCMSource(SOURCE_ID))
                .findFirst()
                .orElseThrow();

        Class<? extends SCMSource> clazz = source.getClass();
        Field mergeRequestContributorCache = clazz.getDeclaredField("mergeRequestContributorCache");
        mergeRequestContributorCache.setAccessible(true);
        Field mergeRequestMetadataCache = clazz.getDeclaredField("mergeRequestMetadataCache");
        mergeRequestMetadataCache.setAccessible(true);
        assertNotNull(mergeRequestMetadataCache.get(source));
        assertNotNull(mergeRequestContributorCache.get(source));
    }

    @Test
    void projectIdSurvivesConfigRoundtrip(JenkinsRule j) throws Exception {
        GitLabSCMSourceBuilder sb =
                new GitLabSCMSourceBuilder(SOURCE_ID, "server", "creds", "po", "group/project", "project");
        WorkflowMultiBranchProject project = j.createProject(WorkflowMultiBranchProject.class, PROJECT_NAME);
        GitLabSCMSource source = sb.build();
        project.getSourcesList().add(new BranchSource(source));
        long p = 42;
        source.setProjectId(p);
        j.configRoundtrip(project);

        WorkflowMultiBranchProject item = j.jenkins.getItemByFullName(PROJECT_NAME, WorkflowMultiBranchProject.class);
        assertNotNull(item);
        GitLabSCMSource scmSource = (GitLabSCMSource) item.getSCMSource(SOURCE_ID);
        assertNotNull(scmSource);
        assertEquals(Long.valueOf(p), scmSource.getProjectId());
    }
}
