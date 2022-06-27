package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import jenkins.scm.api.SCMFile;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.TreeItem;

public class GitLabSCMFile extends SCMFile {

    private final GitLabApi gitLabApi;
    private final String projectPath;
    private final String ref;
    private boolean isDir;

    public GitLabSCMFile(GitLabApi gitLabApi, String projectPath, String ref) {
        super();
        this.gitLabApi = gitLabApi;
        this.isDir = true;
        type(Type.DIRECTORY);
        this.projectPath = projectPath;
        this.ref = ref;
    }

    private GitLabSCMFile(@NonNull GitLabSCMFile parent, String name, boolean isDir) {
        super(parent, name);
        this.gitLabApi = parent.gitLabApi;
        this.projectPath = parent.projectPath;
        this.ref = parent.ref;
        this.isDir = isDir;
    }

    private GitLabSCMFile(GitLabSCMFile parent, String name, Type type) {
        super(parent, name);
        this.gitLabApi = parent.gitLabApi;
        this.projectPath = parent.projectPath;
        this.ref = parent.ref;
        isDir = type == Type.DIRECTORY;
        type(type);
    }

    @NonNull
    @Override
    protected SCMFile newChild(@NonNull String name, boolean assumeIsDirectory) {
        return new GitLabSCMFile(this, name, assumeIsDirectory);
    }

    @NonNull
    @Override
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        if (!this.isDirectory()) {
            throw new IOException("Cannot get children from a regular file");
        }
        List<TreeItem> treeItems = fetchTree();
        List<SCMFile> result = new ArrayList<>(treeItems.size());
        for (TreeItem c : treeItems) {
            Type t;
            if (c.getType() == TreeItem.Type.TREE) {
                t = Type.DIRECTORY;
            } else if (c.getType() == TreeItem.Type.BLOB) {
                if ("120000".equals(c.getMode())) {
                    // File Mode 120000 is a symlink
                    t = Type.LINK;
                } else {
                    t = Type.REGULAR_FILE;
                }
            } else {
                t = Type.OTHER;
            }
            result.add(new GitLabSCMFile(this, c.getName(), t));
        }
        return result;
    }

    @Override
    public long lastModified() throws IOException, InterruptedException {
        // TODO Fix this
        return 0L;
    }

    @NonNull
    @Override
    protected Type type() throws IOException, InterruptedException {
        if (isDir) {
            return Type.DIRECTORY;
        }
        try {
            gitLabApi.getRepositoryFileApi()
                .getFile(projectPath, getPath(), ref);
            return Type.REGULAR_FILE;
        } catch (GitLabApiException e) {
            if (e.getHttpStatus() != 404) {
                throw new IOException(e);
            }
            try {
                List<TreeItem> files = gitLabApi.getRepositoryApi().getTree(projectPath, getPath(), ref);
                if (files.size() == 0) {
                    return Type.NONEXISTENT;
                }
                return Type.DIRECTORY;
            } catch (GitLabApiException ex) {
                if (e.getHttpStatus() != 404) {
                    throw new IOException(e);
                }
            }
        }
        return Type.NONEXISTENT;
    }

    @NonNull
    @Override
    public InputStream content() throws IOException, InterruptedException {
        if (this.isDirectory()) {
            throw new IOException("Cannot get raw content from a directory");
        } else {
            return fetchFile();
        }
    }

    private InputStream fetchFile() throws IOException {
        try {
            return gitLabApi.getRepositoryFileApi().getRawFile(projectPath, ref, getPath());
        } catch (GitLabApiException e) {
            throw new IOException(String.format("%s not found at %s", getPath(), ref));
        }
    }

    private List<TreeItem> fetchTree() throws IOException {
        try {
            return gitLabApi.getRepositoryApi().getTree(projectPath, getPath(), ref);
        } catch (GitLabApiException e) {
            throw new IOException(String.format("%s not found at %s", getPath(), ref));
        }
    }

}
