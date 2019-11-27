package io.jenkins.plugins.gitlabbranchsource.retry;

import java.util.logging.Logger;
import org.gitlab4j.api.GitLabApiException;

public class RetryHelper {
    public static final Logger LOGGER = Logger.getLogger(GitLabApiRetryWrapper.class.getName());

    public static <T> T withRetry(Retryable<T> func, Integer retryCount) throws GitLabApiException {
        int retries = 0;

        while (true) {
            try {
                return func.execute();
            }
            catch (GitLabApiException e) {
                if (shouldRetry(e) && retries < retryCount) {

                    LOGGER.warning(String.format(
                        "The call to GitLab has failed with status code %s. Will retry %d more times.",
                        e.getHttpStatus(),
                        retryCount - retries));

                    sleepSafe(1000);

                } else {
                    throw e;
                }

                retries++;
            }
        }
    }

    public static void withRetry(RetryableAction action, Integer retryCount) throws GitLabApiException {
        int retries = 0;

        while (true) {
            try {
                action.execute();
                return;
            } catch (GitLabApiException e) {
                if (shouldRetry(e) && retries < retryCount) {

                    LOGGER.warning(String.format(
                        "The call to GitLab has failed with status code %s. Will retry %d more times.",
                        e.getHttpStatus(),
                        retryCount - retries));

                    sleepSafe(1000);
                } else {
                    throw e;
                }

                retries++;
            }
        }
    }

    private static boolean shouldRetry(GitLabApiException exception) {
        int statusCode = exception.getHttpStatus();

        if(statusCode >= 500 || statusCode == 429) {
            return true;
        }

        return false;
    }

    private static void sleepSafe(long duration) {
        try {
            Thread.sleep(duration);
        }
        catch (InterruptedException e) { }
    }
}
