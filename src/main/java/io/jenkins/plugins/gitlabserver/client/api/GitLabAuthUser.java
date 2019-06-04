package io.jenkins.plugins.gitlabserver.client.api;

/**
 * Represents username/password authentication to the GitLab API
 */
public class GitLabAuthUser implements GitLabAuth {
    /**
     * The username.
     */
    private final String username;
    /**
     * The password.
     */
    private final String password;

    /**
     * Constructor.
     *
     * @param username the username.
     * @param password the password.
     */
    public GitLabAuthUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the username.
     *
     * @return the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password.
     *
     * @return the password.
     */
    public String getPassword() {
        return password;
    }
}
