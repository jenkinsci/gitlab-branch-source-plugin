package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GitLabJobDslTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void unchangedSeedRetriesAfterMissingServerIsConfigured() throws Exception {
        try (GitLabFixture gitlab = new GitLabFixture()) {
            FreeStyleProject seed = createSeed();
            runSeed(seed);
            WorkflowMultiBranchProject project = failedProject();

            // Change only global server configuration; the seed script stays identical.
            gitlab.configureServer();
            runSeed(seed);

            assertRecovered(project, gitlab);
        }
    }

    @Test
    public void unchangedSeedRetriesAfterGitLabOutage() throws Exception {
        try (GitLabFixture gitlab = new GitLabFixture()) {
            gitlab.configureServer();
            gitlab.available = false;
            FreeStyleProject seed = createSeed();
            runSeed(seed);
            WorkflowMultiBranchProject project = failedProject();
            int firstAttemptRequests = gitlab.projectRequests.get();
            assertTrue(firstAttemptRequests > 0);

            // Another seed run during the outage must still attempt retrieval.
            runSeed(seed);
            assertEquals(Result.FAILURE, project.getComputation().getResult());
            assertTrue(gitlab.projectRequests.get() > firstAttemptRequests);

            // Restore only the external API, leaving the seed definition untouched.
            gitlab.available = true;
            runSeed(seed);

            assertRecovered(project, gitlab);
        }
    }

    private WorkflowMultiBranchProject failedProject() {
        WorkflowMultiBranchProject project = j.jenkins.getItemByFullName("project", WorkflowMultiBranchProject.class);
        assertNotNull(project);
        assertEquals(Result.FAILURE, project.getComputation().getResult());
        assertNull(source(project).getProjectId());
        return project;
    }

    private static void assertRecovered(WorkflowMultiBranchProject project, GitLabFixture gitlab) throws IOException {
        assertEquals(
                Files.readString(project.getComputation().getLogFile().toPath()),
                Result.SUCCESS,
                project.getComputation().getResult());
        assertEquals(Long.valueOf(42), source(project).getProjectId());
        assertEquals("git@gitlab.example:group/project.git", source(project).getSshRemote());
        assertEquals("https://gitlab.example/group/project.git", source(project).getHttpRemote());
        assertTrue(project.getConfigFile().asString().contains("<projectId>42</projectId>"));
        assertEquals(1, gitlab.branchRequests.get());
    }

    private FreeStyleProject createSeed() throws Exception {
        FreeStyleProject seed = j.createFreeStyleProject("seed");
        ExecuteDslScripts dsl = new ExecuteDslScripts();
        dsl.setScriptText("""
                multibranchPipelineJob('project') {
                    branchSources {
                        branchSource {
                            source {
                                gitlab {
                                    id('gitlab-source')
                                    serverName('gitlab')
                                    projectOwner('group')
                                    projectPath('group/project')
                                    traits {
                                        gitLabBranchDiscovery { strategyId(3) }
                                    }
                                }
                            }
                        }
                    }
                }
                """);
        seed.getBuildersList().add(dsl);
        return seed;
    }

    private void runSeed(FreeStyleProject seed) throws Exception {
        j.buildAndAssertSuccess(seed);
        j.waitUntilNoActivity();
    }

    private static GitLabSCMSource source(WorkflowMultiBranchProject project) {
        return (GitLabSCMSource) project.getSCMSource("gitlab-source");
    }

    private static class GitLabFixture implements AutoCloseable {
        private static final String PROJECT_PATH = "/api/v4/projects/group/project";
        private static final String BRANCHES_PATH = "/api/v4/projects/42/repository/branches";
        private static final Map<String, String> RESPONSES =
                Map.of(PROJECT_PATH, """
                {"id":42,"name":"project","path_with_namespace":"group/project",
                 "web_url":"https://gitlab.example/group/project",
                 "ssh_url_to_repo":"git@gitlab.example:group/project.git",
                 "http_url_to_repo":"https://gitlab.example/group/project.git"}
                """, PROJECT_PATH + "/members/all", "[]", BRANCHES_PATH, "[]");

        private final HttpServer server;
        private final AtomicInteger projectRequests = new AtomicInteger();
        private final AtomicInteger branchRequests = new AtomicInteger();
        private volatile boolean available = true;

        private GitLabFixture() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::respond);
            server.start();
        }

        private void configureServer() {
            GitLabServers.get()
                    .addServer(new GitLabServer(
                            "http://127.0.0.1:" + server.getAddress().getPort(), "gitlab", ""));
        }

        private void respond(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (PROJECT_PATH.equals(path)) {
                projectRequests.incrementAndGet();
            }
            if (BRANCHES_PATH.equals(path)) {
                branchRequests.incrementAndGet();
            }
            if (!available) {
                send(exchange, 503, "{\"message\":\"GitLab unavailable\"}");
                return;
            }
            if (!RESPONSES.containsKey(path)) {
                send(exchange, 404, "{\"message\":\"Unexpected API request\"}");
                return;
            }
            send(exchange, 200, RESPONSES.get(path));
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
            server.stop(0);
        }
    }
}
