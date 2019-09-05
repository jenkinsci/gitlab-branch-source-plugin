package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.trait.SCMSourceBuilder;

public class GitLabSCMSourceBuilder extends
    SCMSourceBuilder<GitLabSCMSourceBuilder, GitLabSCMSource> {

    @CheckForNull
    private final String id;
    @CheckForNull
    private final String serverName;
    @CheckForNull
    private final String credentialsId;
    @NonNull
    private final String projectOwner;
    @NonNull
    private final String projectPath;
    @NonNull
    private final String projectName;

    public GitLabSCMSourceBuilder(@CheckForNull String id, @CheckForNull String serverName,
        @CheckForNull String credentialsId, @NonNull String projectOwner,
        @NonNull String projectPath, @NonNull String projectName) {
        super(GitLabSCMSource.class, projectName);
        this.projectName = projectName;
        this.projectPath = projectPath;
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
    public String getCredentialsId() {
        return credentialsId;
    }

    @NonNull
    @Override
    public GitLabSCMSource build() {
        // projectName() should have been getProjectName()
        GitLabSCMSource result = new GitLabSCMSource(serverName, projectOwner, projectPath);
        result.setId(id);
        result.setCredentialsId(credentialsId);
        result.setTraits(traits());
        result.setProjectName(projectName);
        return result;
    }
}
