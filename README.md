# GitLab Branch Source Plugin

This plugin is being developed as a part of GSoC 2019 project [Multi branch Pipeline Support for GitLab](https://jenkins.io/projects/gsoc/2019/gitlab-support-for-multibranch-pipeline/).

To fully run a Jenkins Continuous Integration on a GitLab repository or project, you require three plugins:

1. [GitLab API Plugin](https://github.com/jenkinsci/gitlab-api-plugin) - Wraps GitHub Java API

2. [GitLab Plugin](https://github.com/jenkinsci/gitlab-plugin/) - Server configuration and web hooks Management 

3. GitLab Branch Source Plugin - To support Multi branch Pipeline Jobs (including Merge Requests) and Folder organisation

GitLab API Plugin has been released and GitLab Plugin is being heavily modified.

### Issues with Jenkins and GitLab Integration:

i. The GitLab Java APIs are written within the plugin itself when it should be a different plugin.

ii. Multi branch Pipeline support is missing as Merge Requests cannot be discovered.

iii. GitLab Folder Organisation for GitLab Projects is not available.

iv. Convention for 3 separate plugin is not followed.

### Goals of this project

1. Implement a lightweight GitLab Plugin that depends on GitLab API Plugin.

2. Follow convention of 3 separate plugins (as listed above).

3. Implement GitLab Branch Source Plugin 

4. Support new Jenkins features such as [Jenkins Code as Configuration](https://github.com/jenkinsci/configuration-as-code-plugin) (JCasc)

5. Provide rich user experience

6. Support all SCM Trait APIs 

7. Fully support Java 8 

### Installation

No binaries are available for this plugin as the plugin is in the very early alpha stage, and not ready for the general public quite yet.  If you want to jump in early, you can try building it yourself from source.

To run the plugin, run the following:

```bash

git clone https://github.com/baymac/gitlab-branch-source-plugin.git\

cd gitlab-branch-source-plugin

mvn clean install # use -DskipTests to skip tests

mvn hpi:run # use -Djetty.port=<port> to define a port for plugin to run

```

If you want to test it with your Jenkins server, after `mvn clean install` follow these steps in your Jenkins instance:

1. Select `Manage Jenkins`

2. Select `Manage Plugins`

3. Select `Advanced` tab

3. In `Upload Plugin` section, select `Choose file`

4. Select ${root_dir}/target/gitlab-branch-source.hpi

5. Select `Upload` 

6. Select `Install without restart`






