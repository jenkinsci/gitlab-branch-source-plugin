package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.Items;
import hudson.model.RootAction;
import hudson.util.HttpResponses;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatarCache.CacheEntry;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import jenkins.model.Jenkins;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponse;

@WithJenkins
class GitLabAvatarCacheAccessTest {
    private String avatar;
    private JenkinsRule r;
    private String path;
    private long timestamp;

    @BeforeEach
    void setUp(JenkinsRule r) throws Exception {
        this.r = r;
        avatar = new URL(r.getURL(), "avatar-origin").toString();
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        // Readers deliberately have no item permissions: avatars are shared display metadata.
        r.jenkins.setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(Jenkins.READ).everywhere().to("reader"));
        path = seed("selected");
    }

    private String seed(String serverName) throws Exception {
        String key = GitLabAvatarCache.cacheKey(avatar, serverName);
        timestamp = (System.currentTimeMillis() / 1000) * 1000 + 987;
        CacheEntry downloaded =
                new CacheEntry(avatar, new BufferedImage(48, 48, BufferedImage.TYPE_INT_RGB), timestamp);
        CacheEntry entry = new CacheEntry(avatar, CompletableFuture.completedFuture(downloaded), serverName);
        assertFalse(entry.pending());
        GitLabAvatarCache cache =
                r.jenkins.getExtensionList(GitLabAvatarCache.class).get(0);
        Field field = GitLabAvatarCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, CacheEntry> entries = (Map<String, CacheEntry>) field.get(cache);
        entries.put(key, entry);
        return "gitlab-avatar/" + key + "?size=48x48";
    }

    private WebResponse request(String user, String since) throws Exception {
        try (JenkinsRule.WebClient client = r.createWebClient()) {
            client.setThrowExceptionOnFailingStatusCode(false);
            if (user != null) {
                client.login(user);
            }
            WebRequest request = new WebRequest(new URL(r.getURL(), path));
            if (since != null) {
                request.setAdditionalHeader("If-Modified-Since", since);
            }
            return client.getPage(request).getWebResponse();
        }
    }

    @Test
    void api_avatars_require_overall_read_even_for_conditional_requests() throws Exception {
        WebResponse authorized = request("reader", null);
        assertEquals(200, authorized.getStatusCode());
        assertEquals("image/png", authorized.getContentType());
        assertEquals("private, no-store", authorized.getResponseHeaderValue("Cache-Control"));
        String modified = authorized.getResponseHeaderValue("Last-Modified");
        assertNotNull(modified);
        for (String user : new String[] {null, "outsider"}) {
            WebResponse denied = request(user, null);
            assertEquals(404, denied.getStatusCode());
            assertEquals("private, no-store", denied.getResponseHeaderValue("Cache-Control"));
            assertEquals(404, request(user, modified).getStatusCode());
        }
    }

    @Test
    void fresh_metadata_seeds_a_read_protected_cache_entry() throws Exception {
        path = render(new GitLabAvatar("generated-avatar", "selected"));
        String key = GitLabAvatarCache.cacheKey("generated-avatar", "selected");
        assertEquals("gitlab-avatar/" + key + "?size=48x48", path);
        assertEquals(404, request(null, null).getStatusCode());
        WebResponse image = request("reader", null);
        assertEquals(200, image.getStatusCode());
        assertEquals("private, no-store", image.getResponseHeaderValue("Cache-Control"));
    }

    private String render(GitLabAvatar metadata) throws Exception {
        r.jenkins.getActions().removeIf(SeedAvatar.class::isInstance);
        r.jenkins.getActions().add(new SeedAvatar(metadata));
        try (JenkinsRule.WebClient client = r.createWebClient().login("reader")) {
            String url =
                    client.goTo("seed-avatar/", "text/plain").getWebResponse().getContentAsString();
            assertTrue(url.startsWith(r.getURL().toString()));
            return url.substring(r.getURL().toString().length());
        }
    }

    public static class SeedAvatar implements RootAction {
        private final GitLabAvatar metadata;

        SeedAvatar(GitLabAvatar metadata) {
            this.metadata = metadata;
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return "seed-avatar";
        }

        public HttpResponse doIndex() {
            return HttpResponses.text(metadata.getAvatarImageOf("48x48"));
        }
    }

    @Test
    void conditional_request_uses_whole_second_precision_and_private_headers() throws Exception {
        WebResponse initial = request("reader", null);
        assertEquals(200, initial.getStatusCode());
        WebResponse unchanged = request("reader", initial.getResponseHeaderValue("Last-Modified"));
        assertEquals(304, unchanged.getStatusCode());
        assertEquals("private, no-store", unchanged.getResponseHeaderValue("Cache-Control"));
        assertEquals(
                timestamp / 1000 * 1000,
                new CacheEntry(avatar, new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), timestamp).lastModified);
    }

    @Test
    void revoking_overall_read_blocks_previously_cached_images() throws Exception {
        assertEquals(200, request("reader", null).getStatusCode());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy());
        assertEquals(404, request("reader", null).getStatusCode());
    }

    @Test
    void persisted_metadata_and_cache_urls_are_independent_of_item_lifecycle() throws Exception {
        GitLabAvatar metadata = new GitLabAvatar(avatar, "selected");
        metadata = (GitLabAvatar) Items.XSTREAM2.fromXML(Items.XSTREAM2.toXML(metadata));
        String originalPath = path;
        Folder folder = r.jenkins.createProject(Folder.class, "folder");
        folder.createProject(Folder.class, "project");
        folder.renameTo("renamed");
        Folder destination = r.jenkins.createProject(Folder.class, "destination");
        folder = Items.move(folder, destination);
        folder.delete();
        r.jenkins.createProject(Folder.class, "folder").createProject(Folder.class, "project");
        assertEquals(originalPath, render(metadata));
        assertEquals(200, request("reader", null).getStatusCode());
    }

    @Test
    void cache_identity_includes_selected_server() {
        String key = GitLabAvatarCache.cacheKey(avatar, "selected");
        assertNotEquals(key, GitLabAvatarCache.cacheKey(avatar, null));
        assertNotEquals(key, GitLabAvatarCache.cacheKey(avatar, "other"));
    }

    @Test
    void avatar_metadata_passes_selected_server_and_keeps_legacy_metadata_anonymous() {
        try (var cache = mockStatic(GitLabAvatarCache.class)) {
            new GitLabAvatar(avatar, "selected").getAvatarImageOf("48x48");
            cache.verify(() -> GitLabAvatarCache.buildUrl(avatar, "48x48", "selected"));
            new GitLabAvatar(avatar).getAvatarImageOf("48x48");
            cache.verify(() -> GitLabAvatarCache.buildUrl(avatar, "48x48", null));
        }
        assertNotEquals(new GitLabAvatar(avatar, "selected"), new GitLabAvatar(avatar, "other"));
        assertEquals(new GitLabAvatar(avatar, "selected"), new GitLabAvatar(avatar, "selected"));
    }

    @Test
    void anonymous_url_cache_entries_remain_public() throws Exception {
        path = seed(null);
        WebResponse image = request(null, null);
        assertEquals(200, image.getStatusCode());
        assertEquals("max-age=3600, public", image.getResponseHeaderValue("Cache-Control"));
    }
}
