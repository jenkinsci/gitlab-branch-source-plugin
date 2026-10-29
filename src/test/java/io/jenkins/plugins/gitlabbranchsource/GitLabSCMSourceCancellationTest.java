package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damnhandy.uri.template.UriTemplate;
import hudson.model.TaskListener;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.models.Constants.MergeRequestState;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;

public class GitLabSCMSourceCancellationTest {
    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void cancellationBetweenProjectLookupAndPublicationDoesNotChangeSavedMetadata() throws Exception {
        for (boolean heads : new boolean[] {true, false}) {
            for (boolean timeout : new boolean[] {true, false}) {
                GitLabSCMSource source = source();
                source.setIndexingTimeout(timeout ? 1 : 0);
                Project project = mock(Project.class);
                CountDownLatch snapshotStarted = new CountDownLatch(1);
                CountDownLatch releaseSnapshot = new CountDownLatch(1);
                when(project.getSshUrlToRepo()).thenAnswer(invocation -> {
                    snapshotStarted.countDown();
                    awaitIgnoringInterrupts(releaseSnapshot);
                    return "git@gitlab.example:group/project.git";
                });
                when(project.getHttpUrlToRepo()).thenReturn("https://gitlab.example/group/project.git");
                when(project.getId()).thenReturn(42L);
                GitLabApi api = api(project);
                SCMSourceOwner owner = mock(SCMSourceOwner.class);
                source.setOwner(owner);
                doAnswer(invocation -> {
                            assertNull(source.getProjectId());
                            assertNull(source.getSshRemote());
                            assertNull(source.getHttpRemote());
                            return null;
                        })
                        .when(owner)
                        .save();
                try (RetrievalRun run = new RetrievalRun(source, api, heads)) {
                    assertTrue(snapshotStarted.await(30, TimeUnit.SECONDS));
                    if (!timeout) {
                        run.caller.interrupt();
                    }
                    Throwable failure = assertThrows(
                                    ExecutionException.class, () -> run.result.get(30, TimeUnit.SECONDS))
                            .getCause();
                    assertTrue(timeout ? failure instanceof IOException : failure instanceof InterruptedException);
                    verify(owner).save();
                    releaseSnapshot.countDown();
                    assertTrue(run.worker.awaitTermination(30, TimeUnit.SECONDS));
                    assertNull(source.getProjectId());
                    assertNull(source.getSshRemote());
                    assertNull(source.getHttpRemote());
                    // A cancelled lookup must not populate the cached project either.
                    GitLabApi recoveredApi = api(new Project().withId(84L));
                    assertEquals(
                            Long.valueOf(84L),
                            source.getGitlabProject(recoveredApi).getId());
                } finally {
                    releaseSnapshot.countDown();
                }
            }
        }
    }

    @Test
    public void cancelledMergeRequestLoopDoesNotOverwriteMetadataFromNextScan() throws Exception {
        GitLabSCMSource source = source();
        source.setTraits(List.of(new OriginMergeRequestDiscoveryTrait(1)));
        Project project = new Project().withId(42L).withWebUrl("https://gitlab.example/group/project");
        GitLabApi oldApi = api(project);
        List<MergeRequest> oldRequests = List.of(mergeRequest(1L, "first"), mergeRequest(2L, "stale title"));
        when(oldApi.getMergeRequestApi().getMergeRequests(project, MergeRequestState.OPENED))
                .thenReturn(oldRequests);
        CountDownLatch branchStarted = new CountDownLatch(1);
        CountDownLatch releaseBranch = new CountDownLatch(1);
        when(oldApi.getRepositoryApi().getBranch(42L, "main")).thenAnswer(invocation -> {
            branchStarted.countDown();
            awaitIgnoringInterrupts(releaseBranch);
            throw new GitLabApiException("Request failed after cancellation");
        });
        try (RetrievalRun oldRun = new RetrievalRun(source, oldApi, true)) {
            if (!branchStarted.await(5, TimeUnit.SECONDS)) {
                oldRun.result.get(1, TimeUnit.SECONDS);
                throw new AssertionError("Retrieval did not reach the target branch lookup");
            }
            oldRun.caller.interrupt();
            assertTrue(
                    assertThrows(ExecutionException.class, () -> oldRun.result.get(30, TimeUnit.SECONDS))
                                    .getCause()
                            instanceof InterruptedException);

            GitLabApi newApi = api(project);
            List<MergeRequest> newRequests = List.of(mergeRequest(2L, "current title"));
            when(newApi.getMergeRequestApi().getMergeRequests(project, MergeRequestState.OPENED))
                    .thenReturn(newRequests);
            try (RetrievalRun newRun = new RetrievalRun(source, newApi, true)) {
                newRun.result.get(30, TimeUnit.SECONDS);
            }
            releaseBranch.countDown();
            assertTrue(oldRun.worker.awaitTermination(30, TimeUnit.SECONDS));
            MergeRequestSCMHead head =
                    new MergeRequestSCMHead("MR-2", 2, new BranchSCMHead("main"), null, null, null, null, null, null);
            ObjectMetadataAction metadata = source.retrieveActions(head, null, TaskListener.NULL).stream()
                    .filter(ObjectMetadataAction.class::isInstance)
                    .map(ObjectMetadataAction.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals("current title", metadata.getObjectDisplayName());
            verify(oldApi.getRepositoryApi()).getBranch(42L, "main");
        } finally {
            releaseBranch.countDown();
        }
    }

    private static GitLabSCMSource source() {
        GitLabSCMSource source =
                new GitLabSCMSourceBuilder("id", "server", "", "group", "group/project", "project").build();
        source.setOwner(mock(SCMSourceOwner.class));
        source.setTraits(List.of(new BranchDiscoveryTrait(3)));
        return source;
    }

    private static GitLabApi api(Project project) throws Exception {
        GitLabApi api = mock(GitLabApi.class, RETURNS_DEEP_STUBS);
        when(api.getProjectApi().getProject("group/project")).thenReturn(project);
        Branch branch = new Branch().withName("main").withCommit(new Commit().withId("abcdef"));
        when(api.getRepositoryApi().getBranch(42L, "main")).thenReturn(branch);
        return api;
    }

    private static MergeRequest mergeRequest(long iid, String title) {
        MergeRequest mr = mock(MergeRequest.class, RETURNS_DEEP_STUBS);
        when(mr.getIid()).thenReturn(iid);
        when(mr.getSourceProjectId()).thenReturn(42L);
        when(mr.getTargetProjectId()).thenReturn(42L);
        when(mr.getSourceBranch()).thenReturn("feature-" + iid);
        when(mr.getTargetBranch()).thenReturn("main");
        when(mr.getTitle()).thenReturn(title);
        when(mr.getSha()).thenReturn("abcdef");
        return mr;
    }

    private static void awaitIgnoringInterrupts(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** Runs real worker threads with a mocked API, including a request that ignores interruption. */
    private static class RetrievalRun implements AutoCloseable {
        private final ExecutorService worker;
        private final FutureTask<Void> result;
        private final Thread caller;

        private RetrievalRun(GitLabSCMSource source, GitLabApi api, boolean heads) {
            worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
                @Override
                public void execute(Runnable command) {
                    super.execute(() -> {
                        try (MockedStatic<GitLabHelper> helper = mockStatic(GitLabHelper.class)) {
                            helper.when(() -> GitLabHelper.apiBuilder(source.getOwner(), "server", ""))
                                    .thenReturn(api);
                            helper.when(() -> GitLabHelper.mergeRequestUriTemplate(anyString()))
                                    .thenAnswer(invocation -> UriTemplate.buildFromTemplate(
                                                    invocation.<String>getArgument(0))
                                            .template("/-/merge_requests/{iid}")
                                            .build());
                            command.run();
                        }
                    });
                }
            };
            result = new FutureTask<>(() -> {
                try (MockedStatic<Executors> executors = mockStatic(Executors.class)) {
                    executors.when(Executors::newSingleThreadExecutor).thenReturn(worker);
                    if (heads) {
                        source.retrieve(null, SCMHeadObserver.collect(), null, TaskListener.NULL);
                    } else {
                        source.retrieve(new BranchSCMHead("main"), TaskListener.NULL);
                    }
                }
                return null;
            });
            caller = new Thread(result, "retrieval-caller");
            caller.start();
        }

        @Override
        public void close() throws Exception {
            caller.interrupt();
            caller.join(TimeUnit.SECONDS.toMillis(30));
            worker.shutdownNow();
        }
    }
}
