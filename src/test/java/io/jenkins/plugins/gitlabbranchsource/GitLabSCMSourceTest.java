package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import hudson.model.TaskListener;
import hudson.security.AccessControlled;
import hudson.util.StreamTaskListener;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import jenkins.branch.BranchSource;
import jenkins.scm.api.SCMHead;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.Project;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@WithJenkins
class GitLabSCMSourceTest {

    private static final String SERVER = "server";
    private static final String PROJECT_NAME = "project";
    private static final String SOURCE_ID = "id";

    private static JenkinsRule j;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void retrieveMRWithEmptyProjectSettings() throws GitLabApiException, IOException, InterruptedException {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        RepositoryApi repoApi = Mockito.mock(RepositoryApi.class);
        MergeRequestApi mrApi = Mockito.mock(MergeRequestApi.class);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        Mockito.when(gitLabApi.getMergeRequestApi()).thenReturn(mrApi);
        Mockito.when(gitLabApi.getRepositoryApi()).thenReturn(repoApi);
        Mockito.when(projectApi.getProject(any())).thenReturn(new Project());
        try (MockedStatic<GitLabHelper> utilities = Mockito.mockStatic(GitLabHelper.class)) {
            utilities
                    .when(() -> GitLabHelper.apiBuilder(any(AccessControlled.class), anyString()))
                    .thenReturn(gitLabApi);
            GitLabServers.get().addServer(new GitLabServer("", SERVER, ""));
            GitLabSCMSourceBuilder sb =
                    new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project");
            WorkflowMultiBranchProject project = j.createProject(WorkflowMultiBranchProject.class, PROJECT_NAME);
            BranchSource source = new BranchSource(sb.build());
            source.getSource()
                    .setTraits(Arrays.asList(new BranchDiscoveryTrait(0), new OriginMergeRequestDiscoveryTrait(1)));
            project.getSourcesList().add(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            final TaskListener listener = new StreamTaskListener(out, StandardCharsets.UTF_8);
            Set<SCMHead> scmHead = source.getSource().fetch(listener);
            assertEquals(0, scmHead.size());
        }
    }
}
