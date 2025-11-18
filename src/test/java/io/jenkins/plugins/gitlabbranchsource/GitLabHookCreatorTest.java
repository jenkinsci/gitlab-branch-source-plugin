package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.util.stream.Stream;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.SystemHooksApi;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.api.models.SystemHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.ArgumentCaptor;

@WithJenkins
class GitLabHookCreatorTest {

    private static final String GITLAB_SERVER_NAME = "test";

    @BeforeEach
    void setUp(JenkinsRule r) {
        GitLabServer gitLabServer = new GitLabServer("", GITLAB_SERVER_NAME, "");
        gitLabServer.setHooksRootUrl("https://jenkins.example.com");
        GitLabServers.get().addServer(gitLabServer);
    }

    @Test
    void shouldCreateSystemHookWhenMissing() throws Exception {
        GitLabApi gitLabApi = mock(GitLabApi.class);
        SystemHooksApi systemHooksApi = mock(SystemHooksApi.class);
        when(gitLabApi.getSystemHooksApi()).thenReturn(systemHooksApi);
        GitLabServer server = GitLabServers.get().findServer(GITLAB_SERVER_NAME);

        GitLabHookCreator.createSystemHookWhenMissing(server, gitLabApi);

        verify(systemHooksApi)
                .addSystemHook("https://jenkins.example.com/gitlab-systemhook/post", null, false, false, true);
    }

    @Test
    void shouldNotCreateSystemHookIfAlreadyExist() throws Exception {
        GitLabApi gitLabApi = mock(GitLabApi.class);
        SystemHooksApi systemHooksApi = mock(SystemHooksApi.class);
        when(gitLabApi.getSystemHooksApi()).thenReturn(systemHooksApi);
        SystemHook existingSystemHook = new SystemHook();
        existingSystemHook.setUrl("https://jenkins.example.com/gitlab-systemhook/post");
        when(systemHooksApi.getSystemHookStream()).thenReturn(Stream.of(existingSystemHook));
        GitLabServer server = GitLabServers.get().findServer(GITLAB_SERVER_NAME);

        GitLabHookCreator.createSystemHookWhenMissing(server, gitLabApi);

        verify(systemHooksApi, never()).addSystemHook(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void shouldCreateWebHookWhenMissing() throws Exception {
        GitLabApi gitLabApi = mock(GitLabApi.class);
        ProjectApi projectApi = mock(ProjectApi.class);
        when(gitLabApi.getProjectApi()).thenReturn(projectApi);

        String result = GitLabHookCreator.createWebHookWhenMissing(
                gitLabApi, "project", "https://jenkins.example.com/gitlab-systemhook/post", "secret");

        assertEquals("created", result);
        ArgumentCaptor<ProjectHook> projectHookCaptor = ArgumentCaptor.forClass(ProjectHook.class);
        verify(projectApi)
                .addHook(
                        eq("project"),
                        eq("https://jenkins.example.com/gitlab-systemhook/post"),
                        projectHookCaptor.capture(),
                        eq(true),
                        eq("secret"));
        ProjectHook createdProjectHook = projectHookCaptor.getValue();

        assertTrue(createdProjectHook.getPushEvents());
        assertTrue(createdProjectHook.getMergeRequestsEvents());
        assertTrue(createdProjectHook.getTagPushEvents());
        assertTrue(createdProjectHook.getNoteEvents());
    }

    @Test
    void shouldUpdateSecretIfWebhookExistAndSecretsDiffers() throws Exception {
        GitLabApi gitLabApi = mock(GitLabApi.class);
        ProjectApi projectApi = mock(ProjectApi.class);
        ProjectHook existingHook = new ProjectHook();
        existingHook.setId(4711L);
        existingHook.setUrl("https://jenkins.example.com/gitlab-webhook/post");
        existingHook.setToken("existingToken");
        when(projectApi.getHooksStream("project")).thenReturn(Stream.of(existingHook));
        when(gitLabApi.getProjectApi()).thenReturn(projectApi);

        String result = GitLabHookCreator.createWebHookWhenMissing(
                gitLabApi, "project", "https://jenkins.example.com/gitlab-webhook/post", "newSecret");

        assertEquals("modified", result);
        assertEquals("newSecret", existingHook.getToken());
        verify(projectApi).modifyHook(existingHook);
    }

    @Test
    void shouldDoNothingIfWebhookAlreadyExist() throws Exception {
        GitLabApi gitLabApi = mock(GitLabApi.class);
        ProjectApi projectApi = mock(ProjectApi.class);
        ProjectHook existingHook = new ProjectHook();
        existingHook.setId(4711L);
        existingHook.setUrl("https://jenkins.example.com/gitlab-webhook/post");
        existingHook.setToken("secret");
        when(projectApi.getHooksStream("project")).thenReturn(Stream.of(existingHook));
        when(gitLabApi.getProjectApi()).thenReturn(projectApi);

        String result = GitLabHookCreator.createWebHookWhenMissing(
                gitLabApi, "project", "https://jenkins.example.com/gitlab-webhook/post", "secret");

        assertEquals("already created", result);
        verify(projectApi).getHooksStream("project");
        verifyNoMoreInteractions(projectApi);
    }
}
