package io.jenkins.plugins.gitlabbranchsource.servers.GitLabServers

import lib.FormTagLib

def f = namespace(FormTagLib)

f.section(title: descriptor.displayName) {

    f.entry(title: _("GitLab Servers"),
            help:descriptor.getHelpFile()) {

        f.repeatableHeteroProperty(
                field: "servers",
                hasHeader: "true",
                addCaption: _("Add GitLab Server")
        )
    }
    f.advanced() {
        // add advanced configurations for users common to GitLab Servers
//        f.entry() {
//            "Advanced configurations will be added in later release"
//        }
    }
}