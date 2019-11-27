package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.function.Supplier;
import java.util.logging.Logger;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;

public class GitLabApiRetryWrapper {
    public static final Logger LOGGER = Logger.getLogger(GitLabApiRetryWrapper.class.getName());

    private final GitLabApi _wrappedApi;
    private final Integer _retryCount;

    public GitLabApiRetryWrapper(GitLabApi wrappedApi, Integer retryCount) {
        _wrappedApi = wrappedApi;
        _retryCount = retryCount;

        _tagsApi = () -> new TagsApiRetryWrapper(_wrappedApi.getTagsApi(), _retryCount);
        _userApi = () -> new UserApiRetryWrapper(_wrappedApi.getUserApi(), _retryCount);
        _notesApi = () -> new NotesApiRetryWrapper(_wrappedApi.getNotesApi(), _retryCount);
        _groupApi = () -> new GroupApiRetryWrapper(_wrappedApi.getGroupApi(), _retryCount);
        _projectApi = () -> new ProjectApiRetryWrapper(_wrappedApi.getProjectApi(), _retryCount);
        _commitsApi = () -> new CommitsApiRetryWrapper(_wrappedApi.getCommitsApi(), _retryCount);
        _repositoryApi = () -> new RepositoryApiRetryWrapper(_wrappedApi.getRepositoryApi(), _retryCount);
        _mergeRequestApi = () -> new MergeRequestApiRetryWrapper(_wrappedApi.getMergeRequestApi(), _retryCount);
        _repositoryFileApi = () -> new RepositoryFileApiRetryWrapper(_wrappedApi.getRepositoryFileApi(), _retryCount);
    }

    private Supplier<TagsApiRetryWrapper> _tagsApi;
    private Supplier<UserApiRetryWrapper> _userApi;
    private Supplier<NotesApiRetryWrapper> _notesApi;
    private Supplier<GroupApiRetryWrapper> _groupApi;
    private Supplier<ProjectApiRetryWrapper> _projectApi;
    private Supplier<CommitsApiRetryWrapper> _commitsApi;
    private Supplier<RepositoryApiRetryWrapper> _repositoryApi;
    private Supplier<MergeRequestApiRetryWrapper> _mergeRequestApi;
    private Supplier<RepositoryFileApiRetryWrapper> _repositoryFileApi;

    public RepositoryApiRetryWrapper getRepositoryApi() {
        return _repositoryApi.get();
    }

    public ProjectApiRetryWrapper getProjectApi() {
         return _projectApi.get();
    }

    public MergeRequestApiRetryWrapper getMergeRequestApi() {
        return _mergeRequestApi.get();
    }

    public TagsApiRetryWrapper getTagsApi() {
        return _tagsApi.get();
    }

    public CommitsApiRetryWrapper getCommitsApi() {
        return _commitsApi.get();
    }

    public GroupApiRetryWrapper getGroupApi() {
        return _groupApi.get();
    }

    public UserApiRetryWrapper getUserApi() {
        return _userApi.get();
    }

    public NotesApiRetryWrapper getNotesApi() {
        return _notesApi.get();
    }

    public RepositoryFileApiRetryWrapper getRepositoryFileApi() {
        return _repositoryFileApi.get();
    }

    public void sudo(String userName) throws GitLabApiException {
        _wrappedApi.sudo(userName);
    }
}
