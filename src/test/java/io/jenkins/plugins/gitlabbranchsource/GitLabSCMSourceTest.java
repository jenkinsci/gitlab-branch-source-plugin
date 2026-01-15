package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import hudson.security.AccessControlled;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabbranchsource.helpers.Sleeper;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jenkins.scm.api.SCMSourceOwner;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class GitLabSCMSourceTest {

    private static final String SERVER = "server";
    private static final String SOURCE_ID = "id";

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    private MockedStatic<GitLabHelper> utilities;

    private MockedConstruction<Sleeper> sleeperMockedConstruction;

    @Before
    public void setUp() {
        utilities = Mockito.mockStatic(GitLabHelper.class);
        sleeperMockedConstruction = Mockito.mockConstruction(Sleeper.class);
    }

    @After
    public void tearDown() {
        utilities.close();
        sleeperMockedConstruction.close();
    }

    @Test
    public void testGetMembersWithNoRetries() throws GitLabApiException {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        Member mockMember = Mockito.mock(Member.class);
        Mockito.when(mockMember.getUsername()).thenReturn("example.user");
        Mockito.when(mockMember.getAccessLevel()).thenReturn(AccessLevel.DEVELOPER);
        SCMSourceOwner mockOwner = Mockito.mock(SCMSourceOwner.class);
        Mockito.when(projectApi.getAllMembers("group/project")).thenReturn(List.of(mockMember));
        utilities
                .when(() -> GitLabHelper.apiBuilder(any(AccessControlled.class), anyString(), anyString()))
                .thenReturn(gitLabApi);
        GitLabServers.get().addServer(new GitLabServer("", SERVER, ""));
        GitLabSCMSourceBuilder sb =
                new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project");
        GitLabSCMSource source = sb.build();
        source.setOwner(mockOwner);
        assertEquals(Map.of("example.user", AccessLevel.DEVELOPER), source.getMembers());
        Sleeper sleeper = sleeperMockedConstruction.constructed().get(0);
        Mockito.verifyNoInteractions(sleeper);
    }

    @Test
    public void testGetMembersWithAllRetries() throws GitLabApiException, InterruptedException {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        SCMSourceOwner mockOwner = Mockito.mock(SCMSourceOwner.class);
        GitLabApiException rateLimitException = new GitLabApiException("Rate limit", 429);
        Mockito.when(projectApi.getAllMembers("group/project")).thenThrow(rateLimitException);
        utilities
                .when(() -> GitLabHelper.apiBuilder(any(AccessControlled.class), anyString(), anyString()))
                .thenReturn(gitLabApi);
        GitLabServers.get().addServer(new GitLabServer("", SERVER, ""));
        GitLabSCMSourceBuilder sb =
                new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project");
        GitLabSCMSource source = sb.build();
        source.setOwner(mockOwner);
        assertEquals(Map.of(), source.getMembers());
        Sleeper sleeper = sleeperMockedConstruction.constructed().get(0);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(5000);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(10000);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(20000);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(40000);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(80000);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(160000);
        Mockito.verifyNoMoreInteractions(sleeper);
    }

    @Test
    public void testGetMembersWithSomeRetries() throws GitLabApiException, InterruptedException {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        GitLabApiException rateLimitException = new GitLabApiException("Rate limit", 429);
        Member mockMember = Mockito.mock(Member.class);
        Mockito.when(mockMember.getUsername()).thenReturn("example.user");
        Mockito.when(mockMember.getAccessLevel()).thenReturn(AccessLevel.DEVELOPER);
        SCMSourceOwner mockOwner = Mockito.mock(SCMSourceOwner.class);
        AtomicInteger counter = new AtomicInteger();
        Mockito.when(projectApi.getAllMembers("group/project")).thenAnswer((input) -> {
            if (counter.getAndIncrement() < 3) {
                throw rateLimitException;
            }
            return List.of(mockMember);
        });
        utilities
                .when(() -> GitLabHelper.apiBuilder(any(AccessControlled.class), anyString(), anyString()))
                .thenReturn(gitLabApi);
        GitLabServers.get().addServer(new GitLabServer("", SERVER, ""));
        GitLabSCMSourceBuilder sb =
                new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project");
        GitLabSCMSource source = sb.build();
        source.setOwner(mockOwner);
        assertEquals(Map.of("example.user", AccessLevel.DEVELOPER), source.getMembers());
        Sleeper sleeper = sleeperMockedConstruction.constructed().get(0);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(5000);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(10000);
        Mockito.verify(sleeper, Mockito.times(1)).sleep(20000);
        Mockito.verifyNoMoreInteractions(sleeper);
    }
}
