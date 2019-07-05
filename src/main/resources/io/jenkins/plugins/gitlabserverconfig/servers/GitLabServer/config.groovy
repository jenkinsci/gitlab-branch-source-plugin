package io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer
import lib.CredentialsTagLib
import lib.FormTagLib
import org.apache.commons.lang.RandomStringUtils

def f = namespace(FormTagLib)
def c = namespace(CredentialsTagLib)

f.entry(title: _("Display Name"), field: "name", "description": "A unique name for the server") {
    f.textbox(default: String.format("gitlab-%s", RandomStringUtils.randomNumeric(GitLabServer.SHORT_NAME_LENGTH)))
}

f.entry(title: _("Server URL"), field: "serverUrl", "description": "The url to the GitLab server") {
    f.textbox(default: GitLabServer.GITLAB_SERVER_URL)
}

f.entry(title: _("Credentials"), field: "credentialsId", "description": "The Personal Access Token for GitLab APIs access") {
    c.select(context: app)
}

f.entry(title: _("Web Hook"), "description": "Do you want to automatically manage GitLab Web Hooks on Jenkins Server?",
        help:descriptor.getHelpFile("manageHooks")) {
    f.checkbox(title: _("Manage Web Hooks"), field: "manageHooks")
}

// Add advance fields if user needs some meta data to send to the api
//f.advanced() {
//    f.entry("title": "Advanced configurations") {
//        f.textbox("default": "Will be added in later release")
//    }
//}

f.validateButton(
        title: _("Test connection"),
        progress: _("Testing.."),
        method: "testConnection",
        with: "serverUrl,credentialsId"
)



