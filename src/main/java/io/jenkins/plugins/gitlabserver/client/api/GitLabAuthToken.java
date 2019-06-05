package io.jenkins.plugins.gitlabserver.client.api;

/**
 * Represents token based authentication to the GitLab API.
 */
public class GitLabAuthToken implements GitLabAuth {
    /**
     * The token.
     */
    private final String token;

    /**
     * Constructor.
     *
     * @param token the token.
     */
    public GitLabAuthToken(String token) {
        this.token = token;
    }

    /**
     * Gets the token.
     *
     * @return the token.
     */
    public String getToken() {
        return token;
    }
}
