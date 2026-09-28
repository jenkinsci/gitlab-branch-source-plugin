package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damnhandy.uri.template.UriTemplate;
import hudson.Util;
import hudson.model.Items;
import hudson.model.TaskListener;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import java.io.IOException;
import java.util.List;
import jenkins.branch.BranchSource;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.SCMSourceOwner;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Project;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;

@RunWith(Parameterized.class)
public class GitLabSCMSourceRetrievalTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] retrievals() {
        return new Object[][] {
            {"heads", (Retrieval) source -> source.retrieve(null, SCMHeadObserver.collect(), null, TaskListener.NULL), 1
            },
            {"revision", (Retrieval) source -> source.retrieve(new BranchSCMHead("master"), TaskListener.NULL), 0},
            {"source actions", (Retrieval) source -> source.retrieveActions((SCMSourceEvent) null, TaskListener.NULL), 0
            },
            {
                "head actions",
                (Retrieval) source -> source.retrieveActions(new BranchSCMHead("master"), null, TaskListener.NULL),
                0
            }
        };
    }

    private final Retrieval retrieval;
    private final int successfulSaves;

    public GitLabSCMSourceRetrievalTest(String name, Retrieval retrieval, int successfulSaves) {
        this.retrieval = retrieval;
        this.successfulSaves = successfulSaves;
    }

    @Test
    public void successfulRetrievalDoesNotChangeSourceDigest() throws Exception {
        GitLabApi api = mockApi();
        GitLabSCMSource source = newSource();
        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        source.setOwner(owner);
        try (MockedStatic<GitLabHelper> helper = mockHelper(source, api)) {
            // Settle project metadata before comparing, just as an initial scan would.
            source.getGitlabProject(api);
            String digest = sourceDigest(source);

            retrieval.run(source);

            assertEquals(digest, sourceDigest(source));
            verify(owner, times(successfulSaves)).save();
        }
    }

    @Test
    public void failedProjectLookupStillSavesTimestampForJobDslRetry() throws Exception {
        GitLabApi api = mockApi();
        GitLabSCMSource source = newSource();
        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        source.setOwner(owner);
        Project project = api.getProjectApi().getProject("group/project");
        when(api.getProjectApi().getProject("group/project"))
                .thenThrow(new GitLabApiException("GitLab unreachable"))
                .thenReturn(project);
        try (MockedStatic<GitLabHelper> helper = mockHelper(source, api)) {
            assertThrows(IOException.class, () -> retrieval.run(source));

            assertNotNull(source.getLastRetrieveTimestamp());
            verify(owner).save();

            // Recover from the failed lookup, then check that successful retrievals settle.
            retrieval.run(source);
            String digest = sourceDigest(source);
            clearInvocations(owner);
            retrieval.run(source);
            assertEquals(digest, sourceDigest(source));
            verify(owner, times(successfulSaves)).save();
        }
    }

    @Test
    public void failedClientInitializationStillSavesTimestampForJobDslRetry() throws Exception {
        GitLabSCMSource source = newSource();
        SCMSourceOwner owner = mock(SCMSourceOwner.class);
        source.setOwner(owner);

        assertThrows(IllegalStateException.class, () -> retrieval.run(source));

        assertNotNull(source.getLastRetrieveTimestamp());
        verify(owner).save();
    }

    @Test
    public void successfulRetrievalPreservesLegacyTimestamp() throws Exception {
        GitLabApi api = mockApi();
        GitLabSCMSource source = newSource();
        source.setLastRetrieveTimestamp(1L);
        try (MockedStatic<GitLabHelper> helper = mockHelper(source, api)) {
            source.getGitlabProject(api);
            String digest = sourceDigest(source);

            retrieval.run(source);

            assertEquals(digest, sourceDigest(source));
        }
    }

    private static GitLabSCMSource newSource() {
        return new GitLabSCMSourceBuilder("id", "server", "creds", "group", "group/project", "project").build();
    }

    private static GitLabApi mockApi() throws GitLabApiException {
        GitLabApi api = mock(GitLabApi.class);
        ProjectApi projects = mock(ProjectApi.class);
        RepositoryApi repository = mock(RepositoryApi.class);
        Project project = new Project().withId(42L).withWebUrl("https://gitlab.example/group/project");
        project.setSshUrlToRepo("git@gitlab.example:group/project.git");
        project.setHttpUrlToRepo("https://gitlab.example/group/project.git");
        Commit commit = new Commit().withId("0123456789012345678901234567890123456789");
        Branch branch = new Branch().withName("master").withCommit(commit);
        when(api.getProjectApi()).thenReturn(projects);
        when(api.getRepositoryApi()).thenReturn(repository);
        when(projects.getProject(any())).thenReturn(project);
        when(repository.getBranch(project, "master")).thenReturn(branch);
        return api;
    }

    private static MockedStatic<GitLabHelper> mockHelper(GitLabSCMSource source, GitLabApi api) {
        MockedStatic<GitLabHelper> helper = mockStatic(GitLabHelper.class);
        helper.when(() -> GitLabHelper.apiBuilder(source.getOwner(), "server", "creds"))
                .thenReturn(api);
        helper.when(() -> GitLabHelper.branchUriTemplate(anyString()))
                .thenAnswer(invocation -> UriTemplate.buildFromTemplate(invocation.<String>getArgument(0))
                        .template("/-/tree/{branch}")
                        .build());
        return helper;
    }

    private static String sourceDigest(GitLabSCMSource source) {
        // Branch API uses this serializer to decide whether sources need reindexing.
        return Util.getDigestOf(Items.XSTREAM2.toXML(List.of(new BranchSource(source))));
    }

    @FunctionalInterface
    public interface Retrieval {
        void run(GitLabSCMSource source) throws Exception;
    }
}
