package io.jenkins.plugins.gitlabbranchsource.servers.GitLabServer

import io.jenkins.plugins.gitlabbranchsource.servers.GitLabServer
import lib.FormTagLib
import lib.CredentialsTagLib

def f = namespace(FormTagLib)
def c = namespace(CredentialsTagLib)

f.entry(title: _("Name"), field: "name") {
    f.textbox()
}

f.entry(title: _("Server URL"), field: "serverUrl") {
    f.textbox(default: GitLabServer.GITLAB_SERVER_URL)
}

f.entry(title: _("Credentials"), field: "credentialsId") {
    c.select(context: app, includeUser:false, expressionAllowed:false)
}

// TODO implement verifyCredentials method in GitLabServer
f.block() {
    f.validateButton(
            title: _("Test connection"),
            progress: _("Testing.."),
            method: "verifyCredentials",
            with: "serverUrl,credentialsId"
    )
}

f.entry() {
    f.checkbox(title: _("Manage hooks"), field: "manageHooks")
}

f.advanced() {
    // Add advanced configurations for users specific to the server
    f.description(title: descriptor.getAdvanceConfigMessage)
}