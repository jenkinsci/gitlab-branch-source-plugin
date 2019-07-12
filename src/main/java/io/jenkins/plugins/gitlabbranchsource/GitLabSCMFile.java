package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import jenkins.scm.api.SCMFile;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.RepositoryFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitLabSCMFile extends SCMFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabSCMFile.class);
    private final GitLabApi gitLabApi;
    private final Project project;
    private final String ref;
    private Boolean isFile;

    public GitLabSCMFile(GitLabApi gitLabApi, Project project, String ref) {
        super();
        this.gitLabApi = gitLabApi;
        type(Type.DIRECTORY);
        this.project = project;
        this.ref = ref;
    }

    private GitLabSCMFile(@NonNull GitLabSCMFile parent, String name, Boolean isFile) {
        super(parent, name);
        this.gitLabApi = parent.gitLabApi;
        this.project = parent.project;
        this.ref = parent.ref;
        this.isFile = isFile;
    }

    @NonNull
    @Override
    protected SCMFile newChild(String name, boolean assumeIsDirectory) {
        return new GitLabSCMFile(this, name, assumeIsDirectory ? Boolean.FALSE : null);
    }

    @NonNull
    @Override
    public Iterable<SCMFile> children() throws IOException {
        // TODO Fix this
        return Collections.emptyList();
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        // TODO Fix this
        return 0L;
    }

    @NonNull
    @Override
    protected Type type() throws IOException, InterruptedException {
        // TODO needs review
        if (isFile == null) {
            try {
                isFile = checkFile();
            } catch (GitLabApiException e) {
                e.printStackTrace();
                isFile = false;
                return Type.NONEXISTENT;
            }
        }
        return isFile ? Type.REGULAR_FILE : Type.NONEXISTENT;
    }

    private Boolean checkFile() throws GitLabApiException {
        RepositoryFile file = gitLabApi.getRepositoryFileApi().getFileInfo(project, getPath(), ref);
        return file != null;
    }

    @NonNull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        // TODO needs review
        if (isFile != null && !isFile) {
            throw new FileNotFoundException(getPath());
        }
        InputStream content = fetchFile();
        if(content == null) {
            throw new FileNotFoundException(getPath());
        }
        isFile = true;
        return content;
    }

    private InputStream fetchFile()  {
        try {
            return gitLabApi.getRepositoryFileApi().getRawFile(project, ref, getPath());
        } catch (GitLabApiException e) {
            LOGGER.info("Jenkinfile Not found: "+ref);
        }
        return null;
    }

}
