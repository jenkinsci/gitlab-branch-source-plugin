package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.sun.net.httpserver.HttpServer;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatarCache.CacheEntry;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatarCache.FetchImage;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatarCache.GitLabAvatarLocation;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessTokenImpl;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.ProjectApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitLabAvatarCacheFetchTest {
    private HttpServer http;
    private GitLabServer server;
    private String base;
    private byte[] png;
    private final List<String> paths = new ArrayList<>();
    private final List<String> tokens = new ArrayList<>();
    private int apiStatus = 200;
    private byte[] apiBody;
    private byte[] urlBody;
    private String urlContentType = "image/png";

    @BeforeEach
    void setUp(JenkinsRule r) throws Exception {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff123456);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        png = bytes.toByteArray();
        apiBody = urlBody = png;
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            paths.add(path);
            tokens.add(exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN"));
            boolean api = path.startsWith("/api/v4/");
            byte[] body = api ? apiBody : urlBody;
            // GitLab serves the API avatar as binary data rather than image/png.
            String type = api ? "application/octet-stream" : urlContentType;
            if (type != null) {
                exchange.getResponseHeaders().set("Content-Type", type);
            }
            exchange.sendResponseHeaders(api ? apiStatus : 200, body.length == 0 ? -1 : body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            } finally {
                exchange.close();
            }
        });
        http.start();
        base = "http://127.0.0.1:" + http.getAddress().getPort();
        server = new GitLabServer(base, "local", "avatar-token");
        server.setHooksRootUrl("https://jenkins.example.com");
        GitLabServers.get().addServer(server);
        PersonalAccessTokenImpl credential =
                new PersonalAccessTokenImpl(CredentialsScope.GLOBAL, "avatar-token", "test");
        credential.setToken("local-test-token");
        CredentialsProvider.lookupStores(r.jenkins).iterator().next().addCredentials(Domain.global(), credential);
    }

    @AfterEach
    void tearDown() {
        if (http != null) {
            http.stop(0);
        }
    }

    private String url(String type) {
        return base + "/uploads/-/system/" + type + "/avatar/42/logo.png";
    }

    private void assertImage(CacheEntry entry) {
        assertNotNull(entry.image);
        assertEquals(20, entry.image.getWidth());
        assertEquals(10, entry.image.getHeight());
        assertEquals(0xff123456, entry.image.getRGB(0, 0));
        assertTrue(entry.lastModified > 0);
        assertFalse(entry.isStale());
        assertFalse(entry.pending());
    }

    @ParameterizedTest
    @ValueSource(strings = {"project", "group"})
    void fetches_authenticated_project_and_group_images(String type) {
        assertImage(new FetchImage(url(type)).call());
        assertEquals(List.of("/api/v4/" + type + "s/42/avatar"), paths);
        assertEquals(List.of("local-test-token"), tokens);
    }

    @Test
    void api_failure_falls_back_without_sending_credentials_to_the_upload_url() {
        apiStatus = 404;
        assertImage(new FetchImage(url("project")).call());
        assertEquals(List.of("/api/v4/projects/42/avatar", "/uploads/-/system/project/avatar/42/logo.png"), paths);
        assertEquals("local-test-token", tokens.get(0));
        assertNull(tokens.get(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not an image", "\u0089PNG\r\n\u001a\n"})
    void unusable_api_body_falls_back(String body) {
        apiBody = body.getBytes(StandardCharsets.ISO_8859_1);
        assertImage(new FetchImage(url("project")).call());
        assertEquals(2, paths.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "text/html", "image/png"})
    void unusable_direct_response_completes_without_an_image(String contentType) {
        apiStatus = 404;
        urlContentType = "missing".equals(contentType) ? null : contentType;
        urlBody = "not an image".getBytes(StandardCharsets.UTF_8);
        CacheEntry entry = new FetchImage(url("group")).call();
        assertNull(entry.image);
        assertFalse(entry.pending());
        assertFalse(entry.isStale());
        assertEquals(2, paths.size());
    }

    @Test
    void publishes_a_completed_fetch_to_the_cache_and_keeps_it_after_a_failed_refresh() {
        String url = url("project");
        CacheEntry cached = new CacheEntry(url, CompletableFuture.completedFuture(new FetchImage(url).call()));
        assertFalse(cached.pending());
        assertImage(cached);
        BufferedImage previous = cached.image;

        apiStatus = 404;
        urlBody = new byte[0];
        cached.setFuture(CompletableFuture.completedFuture(new FetchImage(url).call()));
        assertFalse(cached.pending());
        assertSame(previous, cached.image);
        assertFalse(cached.isStale());
    }

    @ParameterizedTest
    @ValueSource(strings = {"image", "invalid", "empty", "throws", "null"})
    void closes_api_and_stream_on_success_and_failure(String response) throws Exception {
        GitLabAvatarLocation location = GitLabAvatarCache.matchAvatar(server, url("project"));
        GitLabApi api = mock(GitLabApi.class);
        ProjectApi projects = mock(ProjectApi.class);
        when(api.getProjectApi()).thenReturn(projects);
        byte[] body =
                switch (response) {
                    case "image", "throws" -> png;
                    case "invalid" -> new byte[] {1, 2, 3};
                    default -> new byte[0];
                };
        ByteArrayInputStream stream = spy(new ByteArrayInputStream(body));
        if ("throws".equals(response)) {
            doThrow(new IOException("read failed")).when(stream).read(any(byte[].class), anyInt(), anyInt());
        }
        when(projects.getAvatar(42L)).thenReturn("null".equals(response) ? null : stream);
        try (var cache = mockStatic(GitLabAvatarCache.class, CALLS_REAL_METHODS)) {
            cache.when(() -> GitLabAvatarCache.openApi(location)).thenReturn(api);
            assertImage(new FetchImage(url("project")).call());
        }
        verify(api).setRequestTimeout(10000, 30000);
        verify(api).close();
        if (!"null".equals(response)) {
            verify(stream, atLeastOnce()).close();
        }
        assertEquals("image".equals(response) ? 0 : 1, paths.size());
    }
}
