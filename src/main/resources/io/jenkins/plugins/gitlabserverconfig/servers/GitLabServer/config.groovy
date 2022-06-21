package io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer

import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer
import lib.CredentialsTagLib
import lib.FormTagLib
import org.apache.commons.lang.RandomStringUtils;

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

f.entry(title: _("Web Hook"), field: "manageWebHooks", "description": "Do you want to automatically manage GitLab Web Hooks on Jenkins Server?") {
    f.checkbox(title: _("Manage Web Hooks"))
}

f.entry(title: _("System Hook"), field: "manageSystemHooks", "description": "Do you want to automatically manage GitLab System Hooks on Jenkins Server?") {
    f.checkbox(title: _("Manage System Hooks"))
}

f.entry(title: _("Secret Token"), field: "secretToken", "description": "The secret token used while setting up hook url in the GitLab server") {
    f.password()
}

f.entry(title: _("Root URL for hooks"), field: "hooksRootUrl", "description": "Jenkins root URL to use in hooks URL (if different from the public Jenkins root URL)") {
    f.textbox()
}

f.advanced() {
    f.entry(title: _("Immediate Web Hook trigger"), field: "immediateHookTrigger", "description": "Trigger a build immediately on a GitLab Web Hook trigger") {
        f.checkbox(title: _("Immediate Web Hook trigger"))
    }
    f.entry(title: _("Web Hook trigger delay"), field: "hookTriggerDelay", "description": "Delay in seconds to be used for GitLab Web Hook build triggers (defaults to GitLab cache timeout)") {
        f.textbox()
    }
}

f.validateButton(
    title: _("Test connection"),
    progress: _("Testing.."),
    method: "testConnection",
    with: "serverUrl,credentialsId"
)



