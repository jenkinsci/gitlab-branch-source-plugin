package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.SCMSourceOwner;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Project;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

public class GitLabSCMSourceRealExecutorTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void projectLookupDoesNotPublishMetadataAfterItsTimedOutTaskReturns() throws Exception {
        GitLabSCMSource source =
                new GitLabSCMSourceBuilder("id", "server", "", "group", "group/project", "project").build();
        source.setIndexingTimeout(1);
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        ProjectApi projectApi = Mockito.mock(ProjectApi.class);
        Project project = Mockito.mock(Project.class);
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch releaseRequest = new CountDownLatch(1);
        CountDownLatch requestReturned = new CountDownLatch(1);
        CountDownLatch metadataPublished = new CountDownLatch(1);
        Mockito.when(gitLabApi.getProjectApi()).thenReturn(projectApi);
        Mockito.when(projectApi.getProject(Mockito.any())).thenAnswer(invocation -> {
            requestStarted.countDown();
            boolean interrupted = false;
            while (true) {
                try {
                    releaseRequest.await();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            requestReturned.countDown();
            return project;
        });
        Mockito.when(project.getSshUrlToRepo()).thenReturn("git@gitlab.example:group/project.git");
        Mockito.when(project.getHttpUrlToRepo()).thenReturn("https://gitlab.example/group/project.git");
        Mockito.when(project.getId()).thenAnswer(invocation -> {
            metadataPublished.countDown();
            return 42L;
        });
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<RuntimeException> lookup = caller.submit(() -> {
                try {
                    source.getGitlabProject(gitLabApi);
                    return null;
                } catch (RuntimeException e) {
                    return e;
                }
            });

            assertTrue(requestStarted.await(5, TimeUnit.SECONDS));
            RuntimeException timeout = lookup.get(5, TimeUnit.SECONDS);
            assertTrue(timeout.getMessage().contains("timed out"));

            releaseRequest.countDown();
            assertTrue(requestReturned.await(5, TimeUnit.SECONDS));
            assertFalse(metadataPublished.await(1, TimeUnit.SECONDS));
            assertNull(source.getProjectId());
            assertNull(source.getSshRemote());
            assertNull(source.getHttpRemote());
        } finally {
            releaseRequest.countDown();
            caller.shutdownNow();
        }
    }

    @Test
    public void positiveTimeoutCancelsRetrievalAndSuppressesLateObserverCallbacks() throws Exception {
        try (GitLabFixture gitLab = new GitLabFixture(true)) {
            GitLabSCMSource source = newSource(gitLab);
            source.getGitlabProject();
            source.setIndexingTimeout(5);
            CountDownLatch observed = new CountDownLatch(1);
            ExecutorService caller = Executors.newSingleThreadExecutor();
            try {
                Future<IOException> retrieval = caller.submit(() -> expectIOException(source, observer(observed)));

                assertTrue(
                        "Timed retrieval did not reach the blocked branch request",
                        gitLab.branchRequestStarted.await(30, TimeUnit.SECONDS));
                IOException failure = retrieval.get(30, TimeUnit.SECONDS);
                assertEquals("Operation timed out while fetching data from GitLab", failure.getMessage());
                assertTrue(failure.getCause() instanceof TimeoutException);

                gitLab.releaseBranches();
                assertTrue(gitLab.branchResponseCompleted.await(30, TimeUnit.SECONDS));
                assertFalse(observed.await(1, TimeUnit.SECONDS));
            } finally {
                gitLab.releaseBranches();
                caller.shutdownNow();
            }
        }
    }

    @Test
    public void standaloneProjectTimeoutClosesClientAndTerminatesWorkerWithoutResponse() throws Exception {
        try (GitLabFixture gitLab = new GitLabFixture(false, true);
                GitLabApi api = Mockito.spy(new GitLabApi(
                        "http://127.0.0.1:" + gitLab.server.getAddress().getPort(), ""))) {
            GitLabSCMSource source = newSource(gitLab);
            source.setIndexingTimeout(5);
            ProjectApi projects = Mockito.spy(api.getProjectApi());
            Mockito.doReturn(projects).when(api).getProjectApi();
            AtomicReference<Thread> worker = new AtomicReference<>();
            Mockito.doAnswer(invocation -> {
                        worker.set(Thread.currentThread());
                        return invocation.callRealMethod();
                    })
                    .when(projects)
                    .getProject(Mockito.any());

            try (MockedStatic<GitLabHelper> helper = Mockito.mockStatic(GitLabHelper.class)) {
                helper.when(() -> GitLabHelper.apiBuilder(source.getOwner(), gitLab.serverName, ""))
                        .thenReturn(api);

                RuntimeException failure = assertThrows(
                        RuntimeException.class,
                        () -> source.retrieveActions((SCMSourceEvent<?>) null, TaskListener.NULL));
                assertTrue(failure.getCause() instanceof TimeoutException);
                assertTrue(gitLab.projectRequestStarted.await(30, TimeUnit.SECONDS));
                assertNotNull(worker.get());
                worker.get().join(TimeUnit.SECONDS.toMillis(30));
                assertFalse(
                        "Project lookup worker must exit without a response",
                        worker.get().isAlive());
                assertEquals(1L, gitLab.releaseProject.getCount());
                Mockito.verify(api).setRequestTimeout(5000, 5000);
                Mockito.verify(api).close();
                assertNull(source.getProjectId());
            } finally {
                gitLab.releaseProject.countDown();
                if (worker.get() != null) {
                    worker.get().join(TimeUnit.SECONDS.toMillis(30));
                }
            }
        }
    }

    @Test
    public void disabledTimeoutWaitsForRetrievalToComplete() throws Exception {
        try (GitLabFixture gitLab = new GitLabFixture(true)) {
            GitLabSCMSource source = newSource(gitLab);
            source.setIndexingTimeout(0);
            CountDownLatch observed = new CountDownLatch(1);
            ExecutorService caller = Executors.newSingleThreadExecutor();
            try {
                Future<Void> retrieval = caller.submit(() -> {
                    source.retrieve(null, observer(observed), null, TaskListener.NULL);
                    return null;
                });

                assertTrue(gitLab.branchRequestStarted.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> retrieval.get(1500, TimeUnit.MILLISECONDS));
                gitLab.releaseBranches();
                retrieval.get(5, TimeUnit.SECONDS);
                assertTrue(observed.await(1, TimeUnit.SECONDS));
            } finally {
                gitLab.releaseBranches();
                caller.shutdownNow();
            }
        }
    }

    @Test
    public void interruptionCancelsRetrievalAndSuppressesLateObserverCallbacks() throws Exception {
        try (GitLabFixture gitLab = new GitLabFixture(true)) {
            GitLabSCMSource source = newSource(gitLab);
            source.setIndexingTimeout(0);
            CountDownLatch observed = new CountDownLatch(1);
            AtomicReference<Thread> callerThread = new AtomicReference<>();
            ExecutorService caller = Executors.newSingleThreadExecutor();
            try {
                Future<InterruptedException> retrieval = caller.submit(() -> {
                    callerThread.set(Thread.currentThread());
                    return assertThrows(
                            InterruptedException.class,
                            () -> source.retrieve(null, observer(observed), null, TaskListener.NULL));
                });

                assertTrue(gitLab.branchRequestStarted.await(5, TimeUnit.SECONDS));
                callerThread.get().interrupt();
                retrieval.get(5, TimeUnit.SECONDS);

                gitLab.releaseBranches();
                assertTrue(gitLab.branchResponseCompleted.await(5, TimeUnit.SECONDS));
                assertFalse(observed.await(1, TimeUnit.SECONDS));
            } finally {
                gitLab.releaseBranches();
                caller.shutdownNow();
            }
        }
    }

    @Test
    public void interruptionWaitsForRunningObserverCallbackToFinish() throws Exception {
        try (GitLabFixture gitLab = new GitLabFixture(false)) {
            GitLabSCMSource source = newSource(gitLab);
            CountDownLatch callbackStarted = new CountDownLatch(1);
            CountDownLatch callbackInterrupted = new CountDownLatch(1);
            CountDownLatch releaseCallback = new CountDownLatch(1);
            CountDownLatch callbackFinished = new CountDownLatch(1);
            AtomicReference<Thread> callerThread = new AtomicReference<>();
            SCMHeadObserver observer = new SCMHeadObserver() {
                @Override
                public void observe(SCMHead head, SCMRevision revision) {
                    callbackStarted.countDown();
                    boolean interrupted = false;
                    while (true) {
                        try {
                            releaseCallback.await();
                            break;
                        } catch (InterruptedException e) {
                            interrupted = true;
                            callbackInterrupted.countDown();
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    callbackFinished.countDown();
                }
            };
            ExecutorService caller = Executors.newSingleThreadExecutor();
            try {
                Future<InterruptedException> retrieval = caller.submit(() -> {
                    callerThread.set(Thread.currentThread());
                    return assertThrows(
                            InterruptedException.class, () -> source.retrieve(null, observer, null, TaskListener.NULL));
                });

                assertTrue(callbackStarted.await(30, TimeUnit.SECONDS));
                callerThread.get().interrupt();
                assertTrue(callbackInterrupted.await(30, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> retrieval.get(200, TimeUnit.MILLISECONDS));

                releaseCallback.countDown();
                retrieval.get(30, TimeUnit.SECONDS);
                assertEquals(0L, callbackFinished.getCount());
            } finally {
                releaseCallback.countDown();
                caller.shutdownNow();
                assertTrue(caller.awaitTermination(30, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    public void retrievalPreservesCallerAuthenticationOnWorkerThread() throws Exception {
        try (GitLabFixture gitLab = new GitLabFixture(false)) {
            GitLabSCMSource source = newSource(gitLab);
            source.setIndexingTimeout(5);
            AtomicReference<Authentication> observedAuthentication = new AtomicReference<>();
            AtomicReference<Thread> observerThread = new AtomicReference<>();
            Authentication callerAuthentication = new UsernamePasswordAuthenticationToken(
                    "scm-indexer", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
            SCMHeadObserver observer = new SCMHeadObserver() {
                @Override
                public void observe(SCMHead head, SCMRevision revision) {
                    observedAuthentication.set(Jenkins.getAuthentication2());
                    observerThread.set(Thread.currentThread());
                }
            };

            try (ACLContext ignored = ACL.as2(callerAuthentication)) {
                source.retrieve(null, observer, null, TaskListener.NULL);
            }

            assertEquals(callerAuthentication, observedAuthentication.get());
            assertNotSame(Thread.currentThread(), observerThread.get());
        }
    }

    private static GitLabSCMSource newSource(GitLabFixture gitLab) {
        GitLabSCMSource source =
                new GitLabSCMSourceBuilder("id", gitLab.serverName, "", "group", "group/project", "project").build();
        source.setOwner(Mockito.mock(SCMSourceOwner.class));
        source.setTraits(List.of(new BranchDiscoveryTrait(3)));
        return source;
    }

    private static SCMHeadObserver observer(CountDownLatch observed) {
        return new SCMHeadObserver() {
            @Override
            public void observe(SCMHead head, SCMRevision revision) {
                observed.countDown();
            }
        };
    }

    private static IOException expectIOException(GitLabSCMSource source, SCMHeadObserver observer) throws Exception {
        try {
            source.retrieve(null, observer, null, TaskListener.NULL);
        } catch (IOException e) {
            return e;
        }
        throw new AssertionError("Retrieval should fail");
    }

    private static class GitLabFixture implements AutoCloseable {
        private static final String PROJECT_PATH = "/api/v4/projects/group/project";
        private static final String BRANCHES_PATH = "/api/v4/projects/42/repository/branches";
        private static final String PROJECT_RESPONSE = """
                {"id":42,"name":"project","path_with_namespace":"group/project",
                 "web_url":"https://gitlab.example/group/project",
                 "ssh_url_to_repo":"git@gitlab.example:group/project.git",
                 "http_url_to_repo":"https://gitlab.example/group/project.git",
                 "merge_requests_enabled":false}
                """;
        private static final String BRANCHES_RESPONSE = """
                [{"name":"master","commit":{"id":"0123456789012345678901234567890123456789"}}]
                """;

        private final HttpServer server;
        private final boolean delayBranches;
        private final String serverName;
        private final CountDownLatch projectRequestStarted = new CountDownLatch(1);
        private final CountDownLatch releaseProject;
        private final CountDownLatch branchRequestStarted = new CountDownLatch(1);
        private final CountDownLatch releaseBranches = new CountDownLatch(1);
        private final CountDownLatch branchResponseCompleted = new CountDownLatch(1);

        private GitLabFixture(boolean delayBranches) throws IOException {
            this(delayBranches, false);
        }

        private GitLabFixture(boolean delayBranches, boolean delayProject) throws IOException {
            this.delayBranches = delayBranches;
            releaseProject = new CountDownLatch(delayProject ? 1 : 0);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            serverName = "real-executor-" + server.getAddress().getPort();
            server.createContext("/", this::respond);
            server.start();
            GitLabServers.get()
                    .addServer(new GitLabServer(
                            "http://127.0.0.1:" + server.getAddress().getPort(), serverName, ""));
            if (!delayBranches) {
                releaseBranches.countDown();
            }
        }

        private void respond(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (BRANCHES_PATH.equals(path)) {
                branchRequestStarted.countDown();
                if (delayBranches) {
                    try {
                        releaseBranches.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        branchResponseCompleted.countDown();
                        return;
                    }
                }
                try {
                    send(exchange, 200, BRANCHES_RESPONSE);
                } finally {
                    branchResponseCompleted.countDown();
                }
                return;
            }

            if (PROJECT_PATH.equals(path)) {
                projectRequestStarted.countDown();
                try {
                    releaseProject.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                    return;
                }
                send(exchange, 200, PROJECT_RESPONSE);
            } else if ((PROJECT_PATH + "/members/all").equals(path)) {
                send(exchange, 200, "[]");
            } else {
                send(exchange, 404, "{\"message\":\"Unexpected API request\"}");
            }
        }

        private void releaseBranches() {
            releaseBranches.countDown();
        }

        private static void send(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (exchange) {
                exchange.getResponseBody().write(bytes);
            }
        }

        @Override
        public void close() {
            releaseProject.countDown();
            releaseBranches();
            GitLabServers.get().removeServer(serverName);
            server.stop(0);
        }
    }
}
