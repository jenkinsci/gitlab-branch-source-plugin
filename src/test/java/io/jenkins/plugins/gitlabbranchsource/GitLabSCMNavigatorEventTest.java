package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import java.util.ArrayList;
import java.util.List;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupProjectsFilter;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.SharedGroup;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.PushEvent;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;

public class GitLabSCMNavigatorEventTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    private static final String KNOWN = "grp/sub/proj";

    private GitLabApi api;
    private GroupApi groupApi;
    private ProjectApi projectApi;
    private GitLabSCMNavigator navigator;

    @Before
    public void setUp() throws Exception {
        api = mock(GitLabApi.class);
        groupApi = mock(GroupApi.class);
        projectApi = mock(ProjectApi.class);
        when(api.getGroupApi()).thenReturn(groupApi);
        when(api.getProjectApi()).thenReturn(projectApi);

        Group group = new Group();
        group.setId(1L);
        group.setName("grp");
        group.setFullName("grp");
        when(groupApi.getGroup("grp")).thenReturn(group);
        when(groupApi.getProjects(anyString(), any(GroupProjectsFilter.class)))
                .thenReturn(List.of(project(KNOWN), project("grp/other")));
        when(projectApi.getProject(KNOWN)).thenReturn(project(KNOWN));

        navigator = new GitLabSCMNavigator("grp");
        navigator.setServerName("server");
        navigator.setTraits(List.of(new SubGroupProjectDiscoveryTrait()));
        navigator.getNavigatorProjects().add(KNOWN);
    }

    @Test
    public void eventForKnownProjectFetchesOnlyThatProject() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        try (MockedStatic<GitLabHelper> helper = mockStatic(GitLabHelper.class, CALLS_REAL_METHODS)) {
            helper.when(() -> GitLabHelper.apiBuilder(any(), any(), nullable(String.class)))
                    .thenReturn(api);
            navigator.visitSources(observer, pushEvent(KNOWN));
        }
        verify(projectApi).getProject(KNOWN);
        verify(groupApi, never()).getProjects(anyString(), any(GroupProjectsFilter.class));
        assertEquals(List.of(KNOWN), observer.observed);
    }

    @Test
    public void eventForUnknownProjectFallsBackToListing() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        try (MockedStatic<GitLabHelper> helper = mockStatic(GitLabHelper.class, CALLS_REAL_METHODS)) {
            helper.when(() -> GitLabHelper.apiBuilder(any(), any(), nullable(String.class)))
                    .thenReturn(api);
            navigator.visitSources(observer, pushEvent("grp/other"));
        }
        verify(groupApi).getProjects(anyString(), any(GroupProjectsFilter.class));
        verify(projectApi, never()).getProject(anyString());
        assertEquals(List.of("grp/other"), observer.observed);
    }

    @Test
    public void fullScanStillListsAllProjects() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        try (MockedStatic<GitLabHelper> helper = mockStatic(GitLabHelper.class, CALLS_REAL_METHODS)) {
            helper.when(() -> GitLabHelper.apiBuilder(any(), any(), nullable(String.class)))
                    .thenReturn(api);
            navigator.visitSources(observer);
        }
        verify(groupApi).getProjects(anyString(), any(GroupProjectsFilter.class));
        verify(projectApi, never()).getProject(anyString());
        assertEquals(List.of(KNOWN, "grp/other"), observer.observed);
    }

    @Test
    public void eventForKnownProjectNoLongerInScopeFallsBackToListing() throws Exception {
        // previously discovered through subgroup discovery, which has since been disabled
        navigator.setTraits(List.of());
        // without subgroup discovery, GitLab only lists the group's direct projects
        when(groupApi.getProjects(anyString(), any(GroupProjectsFilter.class)))
                .thenReturn(List.of(project("grp/other")));
        assertFallsBackToListing(KNOWN);
    }

    @Test
    public void eventForKnownProjectNoLongerSharedFallsBackToListing() throws Exception {
        navigator.setTraits(List.of(new SharedProjectsDiscoveryTrait()));
        navigator.getNavigatorProjects().add("elsewhere/proj");
        when(projectApi.getProject("elsewhere/proj")).thenReturn(project("elsewhere/proj"));
        assertFallsBackToListing("elsewhere/proj");
    }

    @Test
    public void eventForKnownSharedProjectFetchesOnlyThatProject() throws Exception {
        navigator.setTraits(List.of(new SharedProjectsDiscoveryTrait()));
        navigator.getNavigatorProjects().add("elsewhere/proj");
        Project shared = project("elsewhere/proj");
        SharedGroup group = new SharedGroup();
        group.setGroupFullPath("grp");
        shared.setSharedWithGroups(List.of(group));
        when(projectApi.getProject("elsewhere/proj")).thenReturn(shared);
        RecordingObserver observer = new RecordingObserver();
        try (MockedStatic<GitLabHelper> helper = mockStatic(GitLabHelper.class, CALLS_REAL_METHODS)) {
            helper.when(() -> GitLabHelper.apiBuilder(any(), any(), nullable(String.class)))
                    .thenReturn(api);
            navigator.visitSources(observer, pushEvent("elsewhere/proj"));
        }
        verify(groupApi, never()).getProjects(anyString(), any(GroupProjectsFilter.class));
        assertEquals(List.of("elsewhere/proj"), observer.observed);
    }

    @Test
    public void eventForKnownProjectNoLongerAccessibleFallsBackToListing() throws Exception {
        navigator.getNavigatorProjects().add("grp/gone");
        doThrow(new GitLabApiException("404 Project Not Found", 404))
                .when(projectApi)
                .getProject("grp/gone");
        assertFallsBackToListing("grp/gone");
    }

    private void assertFallsBackToListing(String path) throws Exception {
        RecordingObserver observer = new RecordingObserver();
        try (MockedStatic<GitLabHelper> helper = mockStatic(GitLabHelper.class, CALLS_REAL_METHODS)) {
            helper.when(() -> GitLabHelper.apiBuilder(any(), any(), nullable(String.class)))
                    .thenReturn(api);
            navigator.visitSources(observer, pushEvent(path));
        }
        verify(projectApi).getProject(path);
        verify(groupApi).getProjects(anyString(), any(GroupProjectsFilter.class));
        // the listing does not contain the project, so nothing is proposed
        assertEquals(List.of(), observer.observed);
    }

    private static Project project(String path) {
        Project p = new Project();
        p.setPathWithNamespace(path);
        Namespace namespace = new Namespace();
        namespace.setFullPath(path.substring(0, path.lastIndexOf('/')));
        p.setNamespace(namespace);
        p.setName(path.substring(path.lastIndexOf('/') + 1));
        p.setDefaultBranch("main");
        p.setArchived(false);
        p.setWebUrl("https://gitlab.example/" + path);
        return p;
    }

    private static GitLabPushSCMEvent pushEvent(String path) {
        EventProject project = new EventProject();
        project.setPathWithNamespace(path);
        PushEvent push = new PushEvent();
        push.setProject(project);
        push.setBefore("1111111111111111111111111111111111111111");
        push.setAfter("2222222222222222222222222222222222222222");
        push.setRef("refs/heads/main");
        return new GitLabPushSCMEvent(push, "test");
    }

    /** Records the names of the projects the navigator proposes. */
    private static class RecordingObserver extends SCMSourceObserver {
        final List<String> observed = new ArrayList<>();
        private final SCMSourceOwner owner = mock(SCMSourceOwner.class);

        @NonNull
        @Override
        public SCMSourceOwner getContext() {
            return owner;
        }

        @NonNull
        @Override
        public TaskListener getListener() {
            return TaskListener.NULL;
        }

        @NonNull
        @Override
        public ProjectObserver observe(@NonNull String projectName) {
            observed.add(projectName);
            return new ProjectObserver() {
                @Override
                public void addSource(@NonNull SCMSource source) {}

                @Override
                public void addAttribute(@NonNull String key, Object value) {}

                @Override
                public void complete() {}
            };
        }

        @Override
        public void addAttribute(@NonNull String key, Object value) {}
    }
}
