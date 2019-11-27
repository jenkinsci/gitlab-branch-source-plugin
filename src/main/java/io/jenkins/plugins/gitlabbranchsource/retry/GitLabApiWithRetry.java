package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.logging.Logger;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;

public class GitLabApiWithRetry {
    public static final Logger LOGGER = Logger.getLogger(GitLabApiWithRetry.class.getName());

    private final GitLabApi _wrappedApi;
    private final Integer _retryCount;

    public GitLabApiWithRetry(GitLabApi wrappedApi, Integer retryCount) {
        _wrappedApi = wrappedApi;
        _retryCount = retryCount;
    }

    private RepositoryApiWithRetry _repositoryApi;
    private ProjectApiWithRetry _projectApi;
    private MergeRequestApiWithRetry _mergeRequestApi;
    private TagsApiWithRetry _tagsApi;
    private CommitsApiWithRetry _commitsApi;
    private GroupApiWithRetry _groupApi;
    private UserApiWithRetry _userApi;
    private NotesApiWithRetry _notesApi;
    private RepositoryFileApiWithRetry _repositoryFileApi;

    public RepositoryApiWithRetry getRepositoryApi() {
         if (_repositoryApi == null) {
            synchronized (this) {
                if (_repositoryApi == null) {
                    _repositoryApi = new RepositoryApiWithRetry(_wrappedApi.getRepositoryApi(), _retryCount);
                }
            }
        }

        return _repositoryApi;
    }

    public ProjectApiWithRetry getProjectApi() {
        if (_projectApi == null) {
            synchronized (this) {
                if (_projectApi == null) {
                    _projectApi = new ProjectApiWithRetry(_wrappedApi.getProjectApi(), _retryCount);
                }
            }
        }

        return _projectApi;
    }

    public MergeRequestApiWithRetry getMergeRequestApi() {
        if (_mergeRequestApi == null) {
            synchronized (this) {
                if (_mergeRequestApi == null) {
                    _mergeRequestApi = new MergeRequestApiWithRetry(_wrappedApi.getMergeRequestApi(), _retryCount);
                }
            }
        }

        return _mergeRequestApi;
    }

    public TagsApiWithRetry getTagsApi() {
        if (_tagsApi == null) {
            synchronized (this) {
                if (_tagsApi == null) {
                    _tagsApi = new TagsApiWithRetry(_wrappedApi.getTagsApi(), _retryCount);
                }
            }
        }

        return _tagsApi;
    }

    public CommitsApiWithRetry getCommitsApi() {
        if (_commitsApi == null) {
            synchronized (this) {
                if (_commitsApi == null) {
                    _commitsApi = new CommitsApiWithRetry(_wrappedApi.getCommitsApi(), _retryCount);
                }
            }
        }

        return _commitsApi;
    }

    public GroupApiWithRetry getGroupApi() {
        if (_groupApi == null) {
            synchronized (this) {
                if (_groupApi == null) {
                    _groupApi = new GroupApiWithRetry(_wrappedApi.getGroupApi(), _retryCount);
                }
            }
        }

        return _groupApi;
    }

    public UserApiWithRetry getUserApi() {
        if (_userApi == null) {
            synchronized (this) {
                if (_userApi == null) {
                    _userApi = new UserApiWithRetry(_wrappedApi.getUserApi(), _retryCount);
                }
            }
        }

        return _userApi;
    }

    public NotesApiWithRetry getNotesApi() {
        if (_notesApi == null) {
            synchronized (this) {
                if (_notesApi == null) {
                    _notesApi = new NotesApiWithRetry(_wrappedApi.getNotesApi(), _retryCount);
                }
            }
        }

        return _notesApi;
    }

    public RepositoryFileApiWithRetry getRepositoryFileApi() {
        if (_repositoryFileApi == null) {
            synchronized (this) {
                if (_repositoryFileApi == null) {
                    _repositoryFileApi = new RepositoryFileApiWithRetry(_wrappedApi.getRepositoryFileApi(), _retryCount);
                }
            }
        }

        return _repositoryFileApi;
    }

    public void sudo(String userName) throws GitLabApiException {
        _wrappedApi.sudo(userName);
    }
}
