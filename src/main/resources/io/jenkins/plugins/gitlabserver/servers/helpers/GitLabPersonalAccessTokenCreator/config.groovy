package io.jenkins.plugins.gitlabserver.servers.helpers.GitLabPersonalAccessTokenCreator

import io.jenkins.plugins.gitlabserver.servers.GitLabServer
import lib.CredentialsTagLib
import lib.FormTagLib


def f = namespace(FormTagLib);
def c = namespace(CredentialsTagLib)

f.entry(title: _("GitLab Server URL"), field: "serverUrl",
        help: app.getDescriptor(GitLabServer.class)?.getHelpFile("serverUrl")) {
    f.textbox(default: GitLabServer.GITLAB_SERVER_URL)
}

f.radioBlock(checked: true, name: "credentials", value: "plugin", title: "From credentials") {
    f.entry(title: _("Credentials"), field: "credentialsId") {
        c.select(context: app, includeUser: true, expressionAllowed: false)
    }

    f.block() {
        f.validateButton(
                title: _("Create token credentials"),
                progress: _("Creating..."),
                method: "createTokenByCredentials",
                with: "serverUrl,credentialsId"
        )
    }
}

f.radioBlock(checked: false, name: "credentials", value: "manually", title: "From login and password") {

    f.entry(title: _("Username"), field: "login") {
        f.textbox()
    }

    f.entry(title: _("Password"), field: "password") {
        f.password()
    }

    f.block() {
        f.validateButton(
                title: _("Create token credentials"),
                progress: _("Creating..."),
                method: "createTokenByPassword",
                with: "serverUrl,login,password"
        )
    }
}
