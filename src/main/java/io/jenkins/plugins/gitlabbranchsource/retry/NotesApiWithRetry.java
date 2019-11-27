package io.jenkins.plugins.gitlabbranchsource.retry;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.NotesApi;
import org.gitlab4j.api.models.Note;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class NotesApiWithRetry {
    private final NotesApi _notesApi;
    private final Integer _retryCount;

    public NotesApiWithRetry(NotesApi notesApi, Integer retryCount) {
        _notesApi = notesApi;
        _retryCount = retryCount;
    }

    public Note createMergeRequestNote(Object projectIdOrPath, Integer mergeRequestIid, String body) throws GitLabApiException {
        Retryable<Note> call = () -> _notesApi.createMergeRequestNote(projectIdOrPath, mergeRequestIid, body);

        return withRetry(call, _retryCount);
    }
}
