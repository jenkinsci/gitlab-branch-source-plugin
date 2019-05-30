package io.jenkins.plugins.gitlabbranchsource.servers.GitLabServer

import io.jenkins.plugins.gitlabbranchsource.servers.GitLabServer
import lib.FormTagLib
import lib.CredentialsTagLib

def f = namespace(FormTagLib)
def c = namespace(CredentialsTagLib)

f.entry(title: _("Name"), field: "name", "description": "A name for the connection") {
    f.textbox()
}

f.entry(title: _("Server URL"), field: "serverUrl", "description": "The url to the GitLab server") {
    f.textbox(default: GitLabServer.GITLAB_SERVER_URL)
}

f.entry(title: _("Credentials"), field: "credentialsId", "description": "The Personal Access Token for GitLab APIs access") {
    c.select(context: app)
}

f.advanced() {

    f.entry("title": "Advanced configurations") {
        f.textbox("default": "Will be added in later release")
    }
}


f.validateButton(
        title: _("Test connection"),
        progress: _("Testing.."),
        method: "testConnection",
        with: "serverUrl,credentialsId"
)


f.entry() {
    f.checkbox(title: _("Manage hooks"), field: "manageHooks")
}
