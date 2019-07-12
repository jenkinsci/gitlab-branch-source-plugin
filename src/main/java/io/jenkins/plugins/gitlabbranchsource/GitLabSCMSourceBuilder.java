package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.trait.SCMSourceBuilder;

public class GitLabSCMSourceBuilder extends SCMSourceBuilder<GitLabSCMSourceBuilder, GitLabSCMSource> {

    @CheckForNull
    private final String id;
    @CheckForNull
    private final String serverName;
    @CheckForNull
    private final String credentialsId;
    @NonNull
    private final String projectOwner;

    public GitLabSCMSourceBuilder(@CheckForNull String id, @CheckForNull String serverName,
                                  @CheckForNull String credentialsId, @NonNull String projectOwner,
                                  @NonNull String projectPath) {
        super(GitLabSCMSource.class, projectPath);
        this.id = id;
        this.serverName = serverName;
        this.projectOwner = projectOwner;
        this.credentialsId = credentialsId;
    }

    @CheckForNull
    public String getId() {
        return id;
    }

    @CheckForNull
    public String getServerName() {
        return serverName;
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
        GitLabSCMSource result = new GitLabSCMSource(serverName, projectOwner, projectName());
        result.setId(getId());
        result.setCredentialsId(getCredentialsId());
        result.setTraits(traits());
        return result;
    }
}
