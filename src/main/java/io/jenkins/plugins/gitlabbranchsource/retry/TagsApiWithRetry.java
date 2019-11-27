package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.List;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.TagsApi;
import org.gitlab4j.api.models.Tag;

import static io.jenkins.plugins.gitlabbranchsource.retry.RetryHelper.withRetry;

public class TagsApiWithRetry {
    private final TagsApi _tagsApi;
    private final Integer _retryCount;

    public TagsApiWithRetry(TagsApi tagsApi, Integer retryCount){
        _tagsApi = tagsApi;
        _retryCount = retryCount;
    }

    public List<Tag> getTags(Object projectIdOrPath) throws GitLabApiException {
        Retryable<List<Tag>> call = () -> _tagsApi.getTags(projectIdOrPath);

        return withRetry(call, _retryCount);
    }

    public Tag getTag(Object projectIdOrPath, String tagName) throws GitLabApiException {
        Retryable<Tag> call = () -> _tagsApi.getTag(projectIdOrPath, tagName);

        return withRetry(call, _retryCount);
    }
}
