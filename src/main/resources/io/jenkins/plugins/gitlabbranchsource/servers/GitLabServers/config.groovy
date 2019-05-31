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
        f.entry() {
            f.entry(title: _("Additional actions"), help: descriptor.getHelpFile('additional')) {
                f.hetero_list(items: [],
                        addCaption: _("Manage additional GitHub actions"),
                        name: "actions",
                        oneEach: "true", hasHeader: "true", descriptors: instance.actions())
            }
        }
    }
}
