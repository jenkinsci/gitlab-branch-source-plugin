package io.jenkins.plugins.gitlabbranchsource.helpers;

import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.commitUriTemplate;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.getUriTemplateFromServer;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.splitPath;

import com.damnhandy.uri.template.UriTemplateBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.browser.GitRepositoryBrowser;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import java.io.IOException;
import java.net.URL;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

public class GitLabBrowser extends GitRepositoryBrowser {

    @DataBoundConstructor
    public GitLabBrowser(String projectUrl) {
        super(projectUrl);
    }

    public String getProjectUrl() {
        return super.getRepoUrl();
    }

    @Override
    public URL getChangeSetLink(GitChangeSet changeSet) throws IOException {
        return new URL(commitUriTemplate(getProjectUrl())
                .set("hash", changeSet.getId())
                .expand());
    }

    @Override
    public URL getDiffLink(GitChangeSet.Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT
                || path.getSrc() == null
                || path.getDst() == null
                || path.getChangeSet().getParentCommit() == null) {
            return null;
        }
        return diffLink(path);
    }

    @Override
    public URL getFileLink(GitChangeSet.Path path) throws IOException {
        if (path.getEditType().equals(EditType.DELETE)) {
            return diffLink(path);
        } else {
            return new URL(getUriTemplateFromServer(getProjectUrl())
                    .literal("/blob")
                    .path(UriTemplateBuilder.var("changeSet"))
                    .path(UriTemplateBuilder.var("path", true))
                    .build()
                    .set("changeSet", path.getChangeSet().getId())
                    .set("path", splitPath(path.getPath()))
                    .expand());
        }
    }

    private URL diffLink(GitChangeSet.Path path) throws IOException {
        return new URL(getUriTemplateFromServer(getProjectUrl())
                .literal("/commit")
                .path(UriTemplateBuilder.var("changeSet"))
                .fragment(UriTemplateBuilder.var("diff"))
                .build()
                .set("changeSet", path.getChangeSet().getId())
                .set("diff", "#diff-" + getIndexOfPath(path))
                .expand());
    }

    // [JENKINS-72104] notes that the symbol 'gitLabBrowser' is used
    // instead of the preferred 'gitLab' symbol in order to not break
    // compatibility for existing git plugin users.  The git plugin
    // already defines a repository browser with the symbol "gitLab".
    @Symbol("gitLabBrowser")
    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {

        @NonNull
        public String getDisplayName() {
            return "GitLab";
        }

        @Override
        public GitLabBrowser newInstance(StaplerRequest2 req, @NonNull JSONObject jsonObject) throws FormException {
            return req.bindJSON(GitLabBrowser.class, jsonObject);
        }
    }
}
