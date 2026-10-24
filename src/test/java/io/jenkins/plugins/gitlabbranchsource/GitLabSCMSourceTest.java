package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.damnhandy.uri.template.UriTemplate;
import hudson.Util;
import hudson.console.ConsoleNote;
import hudson.model.Items;
import hudson.model.TaskListener;
import hudson.security.AccessControlled;
import hudson.util.StreamTaskListener;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabbranchsource.helpers.Sleeper;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jenkins.branch.BranchSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.models.Constants.MergeRequestState;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
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
    private static final String PROJECT_NAME = "project";
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
    public void failToGetGitlabProject() throws GitLabApiException {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        Mockito.when(projectApi.getProject(any())).thenThrow(new GitLabApiException("GitLab unreachable"));

        GitLabSCMSource gitLabSCMSource =
                new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project").build();

        assertThrows(GitLabApiException.class, () -> gitLabSCMSource.getGitlabProject(gitLabApi));
    }

    @Test
    public void failToRetrieve() throws GitLabApiException, IOException {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        SCMSourceOwner owner = Mockito.mock(SCMSourceOwner.class);

        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        Mockito.when(projectApi.getProject(any())).thenThrow(new GitLabApiException("GitLab unreachable"));

        GitLabSCMSource gitLabSCMSource =
                new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project").build();
        gitLabSCMSource.setOwner(owner);
        utilities
                .when(() -> GitLabHelper.apiBuilder(
                        gitLabSCMSource.getOwner(),
                        gitLabSCMSource.getServerName(),
                        gitLabSCMSource.getCredentialsId()))
                .thenReturn(gitLabApi);

        assertThrows(
                IOException.class,
                () -> gitLabSCMSource.retrieve("", () -> new PrintStream(PrintStream.nullOutputStream())));
        Mockito.verify(owner).save();
    }

    @Test
    public void failedHeadEnumerationSavesTimestampAndSuccessfulRetryKeepsDigestStable() throws Exception {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        RepositoryApi repositoryApi = Mockito.mock(RepositoryApi.class);
        Project project = new Project().withId(42L);
        GitLabApiException failure = new GitLabApiException("Branch enumeration unavailable", 503);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        Mockito.when(gitLabApi.getRepositoryApi()).thenReturn(repositoryApi);
        Mockito.when(projectApi.getProject("group/project")).thenReturn(project);
        Mockito.when(repositoryApi.getBranches(project)).thenThrow(failure).thenReturn(List.of());

        GitLabSCMSource source =
                new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project").build();
        source.setTraits(List.of(new BranchDiscoveryTrait(3)));
        SCMSourceOwner owner = Mockito.mock(SCMSourceOwner.class);
        source.setOwner(owner);
        utilities.when(() -> GitLabHelper.apiBuilder(owner, SERVER, "creds")).thenReturn(gitLabApi);
        assertNull(source.getLastRetrieveTimestamp());

        IOException error = assertThrows(
                IOException.class, () -> source.retrieve(null, SCMHeadObserver.collect(), null, TaskListener.NULL));

        assertEquals("Failed to fetch latest heads", error.getMessage());
        assertSame(failure, error.getCause());
        assertEquals(Long.valueOf(42L), source.getProjectId());
        Long timestamp = source.getLastRetrieveTimestamp();
        assertNotNull(timestamp);
        Mockito.verify(owner, Mockito.atLeastOnce()).save();
        String digest = Util.getDigestOf(Items.XSTREAM2.toXML(List.of(new BranchSource(source))));

        source.retrieve(null, SCMHeadObserver.collect(), null, TaskListener.NULL);

        assertEquals(timestamp, source.getLastRetrieveTimestamp());
        assertEquals(digest, Util.getDigestOf(Items.XSTREAM2.toXML(List.of(new BranchSource(source)))));
        Mockito.verify(repositoryApi, Mockito.times(2)).getBranches(project);
    }

    @Test
    public void retrieveMRWithEmptyProjectSettings() throws GitLabApiException, IOException, InterruptedException {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        RepositoryApi repoApi = Mockito.mock(RepositoryApi.class);
        MergeRequestApi mrApi = Mockito.mock(MergeRequestApi.class);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        Mockito.when(gitLabApi.getMergeRequestApi()).thenReturn(mrApi);
        Mockito.when(gitLabApi.getRepositoryApi()).thenReturn(repoApi);
        Mockito.when(projectApi.getProject(any())).thenReturn(new Project());
        utilities
                .when(() -> GitLabHelper.apiBuilder(any(AccessControlled.class), anyString(), anyString()))
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

    @Test
    public void retrieveMergeRequestsReusesTargetBranchRevision() throws Exception {
        MergeRequestFixture fixture = new MergeRequestFixture(
                List.of(mergeRequest(3, "dev"), mergeRequest(2, "dev"), mergeRequest(1, "master")));

        SCMHeadObserver.Collector collector = SCMHeadObserver.collect();
        fixture.source.retrieve(null, collector, null, fixture.listener);

        Map<SCMHead, SCMRevision> heads = collector.result();
        assertEquals(3, heads.size());
        for (Map.Entry<SCMHead, SCMRevision> head : heads.entrySet()) {
            MergeRequestSCMRevision revision = (MergeRequestSCMRevision) head.getValue();
            assertEquals(
                    "target-"
                            + ((MergeRequestSCMHead) head.getKey()).getTarget().getName(),
                    revision.getBaseHash());
        }
        // dev twice, master once: one branch lookup per distinct target branch, not per merge request.
        Mockito.verify(fixture.repoApi, Mockito.times(2)).getBranch(any(), anyString());
        Mockito.verify(fixture.repoApi).getBranch(42L, "dev");
        Mockito.verify(fixture.repoApi).getBranch(42L, "master");
    }

    @Test
    public void retrieveScopedToMergeRequestsOnlyInspectsThose() throws Exception {
        MergeRequestFixture fixture = new MergeRequestFixture(
                List.of(mergeRequest(3, "dev"), mergeRequest(2, "dev"), mergeRequest(1, "master")));
        // What a merge request event for !2 scopes the retrieve to (see GitLabSCMSourceRequest).
        MergeRequestSCMHead wanted = new MergeRequestSCMHead(
                "MR-2",
                2L,
                new BranchSCMHead("dev"),
                ChangeRequestCheckoutStrategy.MERGE,
                SCMHeadOrigin.DEFAULT,
                "author",
                "group/project",
                "feature-2",
                "MR 2");

        SCMHeadObserver.Collector collector = SCMHeadObserver.collect();
        fixture.source.retrieve(null, new SCMHeadObserver.Filter<>(collector, wanted), null, fixture.listener);

        assertEquals(Set.of(wanted), collector.result().keySet());
        // The two other merge requests are never checked: no log line, no API call.
        String log = fixture.log();
        assertTrue(log.contains("scoped to: !2"), log);
        assertTrue(log.contains("Checking merge request !2"), log);
        assertFalse(log.contains("Checking merge request !3"), log);
        assertFalse(log.contains("Checking merge request !1"), log);
        assertTrue(log.contains("1 merge requests were processed"), log);
        Mockito.verify(fixture.repoApi, Mockito.times(1)).getBranch(any(), anyString());
        Mockito.verify(fixture.repoApi).getBranch(42L, "dev");
    }

    private static MergeRequest mergeRequest(long iid, String targetBranch) {
        Author author = new Author();
        author.setUsername("author");
        author.setName("Author");
        author.setEmail("author@example.com");
        MergeRequest mr = new MergeRequest();
        mr.setIid(iid);
        mr.setTitle("MR " + iid);
        mr.setDescription("Description " + iid);
        mr.setWebUrl("https://gitlab.example.com/group/project/-/merge_requests/" + iid);
        mr.setSourceProjectId(42L);
        mr.setTargetProjectId(42L);
        mr.setSourceBranch("feature-" + iid);
        mr.setTargetBranch(targetBranch);
        mr.setSha("source-" + iid);
        mr.setWorkInProgress(false);
        mr.setAuthor(author);
        return mr;
    }

    /**
     * A source over project 42 whose open merge requests are the given list, with every target branch
     * resolving to the revision {@code target-<branch>} and no branch or tag discovery.
     */
    private final class MergeRequestFixture {
        final GitLabSCMSource source;
        final RepositoryApi repoApi;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TaskListener listener = new StreamTaskListener(out, StandardCharsets.UTF_8);

        MergeRequestFixture(List<MergeRequest> mergeRequests) throws GitLabApiException {
            GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
            ProjectApi projectApi = Mockito.mock(ProjectApi.class);
            MergeRequestApi mrApi = Mockito.mock(MergeRequestApi.class);
            repoApi = Mockito.mock(RepositoryApi.class);
            Project project = new Project().withId(42L);
            Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
            Mockito.when(gitLabApi.getMergeRequestApi()).thenReturn(mrApi);
            Mockito.when(gitLabApi.getRepositoryApi()).thenReturn(repoApi);
            Mockito.when(projectApi.getProject("group/project")).thenReturn(project);
            Mockito.when(mrApi.getMergeRequests(project, MergeRequestState.OPENED))
                    .thenReturn(new ArrayList<>(mergeRequests));
            Mockito.when(repoApi.getBranch(any(), anyString())).thenAnswer(invocation -> {
                Branch branch = new Branch();
                branch.setCommit(new Commit().withId("target-" + invocation.getArgument(1)));
                return branch;
            });
            utilities
                    .when(() -> GitLabHelper.mergeRequestUriTemplate(any()))
                    .thenReturn(UriTemplate.fromTemplate(
                            "https://gitlab.example.com/group/project/-/merge_requests/{iid}"));

            source = new GitLabSCMSourceBuilder(SOURCE_ID, SERVER, "creds", "po", "group/project", "project").build();
            source.setTraits(List.of(new OriginMergeRequestDiscoveryTrait(1)));
            SCMSourceOwner owner = Mockito.mock(SCMSourceOwner.class);
            source.setOwner(owner);
            utilities
                    .when(() -> GitLabHelper.apiBuilder(owner, SERVER, "creds"))
                    .thenReturn(gitLabApi);
        }

        String log() {
            return ConsoleNote.removeNotes(out.toString(StandardCharsets.UTF_8));
        }
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
