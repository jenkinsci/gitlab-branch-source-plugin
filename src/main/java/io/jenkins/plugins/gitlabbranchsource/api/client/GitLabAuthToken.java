package io.jenkins.plugins.gitlabbranchsource.api.client;

/**
 * Represents token based authentication to the Gitea API.
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
        if(token != null) {
            return token;
        }
        return "unknown";
    }
}
