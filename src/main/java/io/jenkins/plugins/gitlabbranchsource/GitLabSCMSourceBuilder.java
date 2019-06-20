package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.trait.SCMSourceBuilder;

public class GitLabSCMSourceBuilder extends SCMSourceBuilder<GitLabSCMSourceBuilder, GitLabSCMSource> {

    @CheckForNull
    private final String id;
    @CheckForNull
    private final String serverUrl;
    @CheckForNull
    private final String credentialsId;
    @NonNull
    private final String projectOwner;

    public GitLabSCMSourceBuilder(@CheckForNull String id, @CheckForNull String serverUrl,
                                  @CheckForNull String credentialsId, @NonNull String projectOwner,
                                  @NonNull String projectName) {
        super(GitLabSCMSource.class, projectName);
        this.id = id;
        this.serverUrl = serverUrl;
        this.projectOwner = projectOwner;
        this.credentialsId = credentialsId;
    }

    @CheckForNull
    public String getId() {
        return id;
    }

    @CheckForNull
    public String getServerUrl() {
        return serverUrl;
    }

    @CheckForNull
    public String getCredentialsId() {
        return credentialsId;
    }

    @NonNull
    public String getProjectOwner() {
        return projectOwner;
    }

    @NonNull
    @Override
    public GitLabSCMSource build() {
        // projectName() should have been getProjectName()
        GitLabSCMSource result = new GitLabSCMSource(serverUrl, projectOwner, projectName());
        result.setId(getId());
        result.setCredentialsId(getCredentialsId());
        result.setTraits(traits());
        return result;
    }

}
