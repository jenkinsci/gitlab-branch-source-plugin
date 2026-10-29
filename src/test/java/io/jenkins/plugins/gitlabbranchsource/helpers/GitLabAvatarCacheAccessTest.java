package io.jenkins.plugins.gitlabbranchsource.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.RootAction;
import hudson.util.HttpResponses;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatarCache.AvatarContext;
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
    private static final String AVATAR = "https://gitlab.example/uploads/-/system/project/avatar/42/logo.png";
    private JenkinsRule r;
    private Folder folder;
    private FreeStyleProject owner;
    private String path;
    private long timestamp;

    @BeforeEach
    void setUp(JenkinsRule r) throws Exception {
        this.r = r;
        folder = r.jenkins.createProject(Folder.class, "private-folder");
        owner = folder.createProject(FreeStyleProject.class, "project");
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(permissions());
        AvatarContext context = new AvatarContext("selected", owner.getFullName());
        String key = GitLabAvatarCache.cacheKey(AVATAR, context);
        // Seed a completed authenticated download. HTTP fetch/cleanup is covered by the fetch tests.
        timestamp = (System.currentTimeMillis() / 1000) * 1000 + 987;
        CacheEntry downloaded =
                new CacheEntry(AVATAR, new BufferedImage(48, 48, BufferedImage.TYPE_INT_RGB), timestamp);
        CacheEntry entry = new CacheEntry(AVATAR, CompletableFuture.completedFuture(downloaded), context);
        assertFalse(entry.pending());
        GitLabAvatarCache cache =
                r.jenkins.getExtensionList(GitLabAvatarCache.class).get(0);
        Field field = GitLabAvatarCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, CacheEntry> entries = (Map<String, CacheEntry>) field.get(cache);
        entries.put(key, entry);
        path = "gitlab-avatar/" + key + "?size=48x48";
    }

    private MockAuthorizationStrategy permissions() {
        return new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("reader", "outsider", "child-only")
                .grant(Item.READ)
                .onItems(folder, owner)
                .to("reader")
                .grant(Item.READ)
                .onItems(owner)
                .to("child-only");
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
    void owning_item_and_parent_read_permissions_are_required_even_for_conditional_requests() throws Exception {
        WebResponse authorized = request("reader", null);
        assertEquals(200, authorized.getStatusCode());
        assertEquals("image/png", authorized.getContentType());
        assertEquals("private, no-store", authorized.getResponseHeaderValue("Cache-Control"));
        String modified = authorized.getResponseHeaderValue("Last-Modified");
        assertNotNull(modified);
        for (String user : new String[] {null, "outsider", "child-only"}) {
            WebResponse denied = request(user, null);
            assertEquals(404, denied.getStatusCode());
            assertEquals("private, no-store", denied.getResponseHeaderValue("Cache-Control"));
            assertEquals(404, request(user, modified).getStatusCode());
        }
    }

    @Test
    void fresh_metadata_seeds_a_private_cache_entry() throws Exception {
        r.jenkins.getActions().add(new SeedAvatar(owner.getFullName()));
        try (JenkinsRule.WebClient client = r.createWebClient().login("reader")) {
            String avatarUrl =
                    client.goTo("seed-avatar/", "text/plain").getWebResponse().getContentAsString();
            String key =
                    GitLabAvatarCache.cacheKey("generated-avatar", new AvatarContext("selected", owner.getFullName()));
            assertEquals(new URL(r.getURL(), "gitlab-avatar/" + key + "?size=48x48").toString(), avatarUrl);
            path = "gitlab-avatar/" + key + "?size=48x48";
            assertEquals(404, request(null, null).getStatusCode());
            WebResponse image = request("reader", null);
            assertEquals(200, image.getStatusCode());
            assertEquals("private, no-store", image.getResponseHeaderValue("Cache-Control"));
        }
    }

    public static class SeedAvatar implements RootAction {
        private final String itemName;

        SeedAvatar(String itemName) {
            this.itemName = itemName;
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
            Item item = Jenkins.get().getItemByFullName(itemName);
            return HttpResponses.text(new GitLabAvatar("generated-avatar", "selected", item).getAvatarImageOf("48x48"));
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
                new CacheEntry(AVATAR, new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), timestamp).lastModified);
    }

    @Test
    void cached_images_do_not_outlive_permission_revocation_or_item_deletion() throws Exception {
        assertEquals(200, request("reader", null).getStatusCode());
        r.jenkins.setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(Jenkins.READ).everywhere().to("reader"));
        assertEquals(404, request("reader", null).getStatusCode());
        r.jenkins.setAuthorizationStrategy(permissions());
        owner.delete();
        assertEquals(404, request("reader", null).getStatusCode());
        FreeStyleProject replacement = folder.createProject(FreeStyleProject.class, "project");
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("reader")
                .grant(Item.READ)
                .onItems(folder, replacement)
                .to("reader"));
        assertEquals(404, request("reader", null).getStatusCode());
    }

    @Test
    void cache_identity_includes_selected_server_and_owning_item() {
        AvatarContext context = new AvatarContext("selected", owner.getFullName());
        String key = GitLabAvatarCache.cacheKey(AVATAR, context);
        assertNotEquals(key, GitLabAvatarCache.cacheKey(AVATAR, null));
        assertNotEquals(key, GitLabAvatarCache.cacheKey(AVATAR, new AvatarContext("other", owner.getFullName())));
        assertNotEquals(key, GitLabAvatarCache.cacheKey(AVATAR, new AvatarContext("selected", folder.getFullName())));
    }

    @Test
    void avatar_metadata_passes_server_and_owner_to_the_cache_and_keeps_legacy_metadata_anonymous() {
        try (var cache = mockStatic(GitLabAvatarCache.class)) {
            new GitLabAvatar(AVATAR, "selected", owner).getAvatarImageOf("48x48");
            cache.verify(() -> GitLabAvatarCache.buildUrl(AVATAR, "48x48", "selected", owner.getFullName()));
            new GitLabAvatar(AVATAR).getAvatarImageOf("48x48");
            cache.verify(() -> GitLabAvatarCache.buildUrl(AVATAR, "48x48", null, null));
        }
        assertNotEquals(new GitLabAvatar(AVATAR, "selected", owner), new GitLabAvatar(AVATAR, "other", owner));
        assertTrue(new GitLabAvatar(AVATAR, "selected", owner).equals(new GitLabAvatar(AVATAR, "selected", owner)));
    }
}
