[![Build Status](https://ci.jenkins.io/job/Plugins/job/gitlab-branch-source-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/gitlab-branch-source-plugin/job/master/)
[![Travis](https://img.shields.io/travis/jenkinsci/gitlab-branch-source-plugin.svg?logo=travis&label=build&logoColor=white)](https://travis-ci.org/jenkinsci/gitlab-branch-source-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/gitlab-branch-source-plugin.svg?label=release)](https://github.com/jenkinsci/gitlab-branch-source-plugin/releases/latest)
[![Gitter](https://badges.gitter.im/jenkinsci/gitlab-branch-source-plugin.svg)](https://gitter.im/jenkinsci/gitlab-branch-source-plugin)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/gitlab-branch-source.svg?color=blue)](https://plugins.jenkins.io/gitlab-branch-source)

# GitLab Branch Source Plugin

To be fully able to run a Jenkins Continuous Integration on a GitLab repository or project,
you require the following plugins:

* [GitLab API Plugin](https://github.com/jenkinsci/gitlab-api-plugin) - Wraps GitLab Java API.

* GitLab Branch Source Plugin - Contains two packages:

     * `io.jenkins.plugins.gitlabserverconfig` - Manages server configuration and web hooks management. Ideally should reside inside another plugin with name `GitLab Plugin`. In future, this package will be moved into a new plugin.
     
     * `io.jenkins.plugins.gitlabbranchsource` - Adds GitLab Branch Source for Multi-branch Pipeline Jobs (including
     Merge Requests) and Folder organisation.

## Getting Started

<details>
<summary>Getting Started with Jenkins:</summary><br>

Jenkins is an open source, self hosting automation server for continuous integration and continuous delivery. The source code of the core Jenkins and its plugins are written in Java. There have been developments on a modern Jenkins Server (see: [Blueocean Plugin](https://github.com/jenkinsci/blueocean-plugin)) using React and other modern front end tools to provide rich user experience.

For more Jenkins related information, see [documentation](https://jenkins.io/doc/).

### Extending Jenkins

Jenkins has more than a 1000 plugins so a vast set of functionality has already been implemented and this can be used to leverage new plugins. Jenkins has an extensibility feature that allows plugin to use other plugins or core features simply by extending their classes. To define or create an extension in Jenkins,we use the `@Extension` annotation type. This annotation is picked up by Jenkins, and the new extension will be added to an `ExtensionList` object, where the extension can then be found via `ExtensionFinder`.

### Running Jenkins server:

Here are a few ways to setup your own Jenkins server:

1. Using a Jenkins docker:

    i. Download docker image from [here](https://hub.docker.com/r/jenkins/jenkins).

    ii. Open up a terminal/command prompt window to the download directory.

    iii. Run command:

    ```bash
    docker run \
      -u root \
      --rm \
      -d \
      -p 8080:8080 \
      -p 50000:50000 \
      -v jenkins-data:/var/jenkins_home \
      -v /var/run/docker.sock:/var/run/docker.sock \
	  --name jenkins \
      jenkinsci/blueocean
    ```

    iv. Browse to `http://localhost:8080`.

    If you need more information about docker commands, see [here](https://jenkins.io/doc/book/installing/#on-macos-and-linux).

2. Using a Jenkins Web application Archive (WAR):

    i. Download [latest stable Jenkins WAR file](http://mirrors.jenkins.io/war-stable/latest/jenkins.war).

    ii. Open up a terminal/command prompt window to the download directory.

    iii. Run command:

    ```bash
    java -jar jenkins.war
    ```

    iv. Browse to `http://localhost:8080/jenkins`.

3. Using a Azure Jenkins solution:

    Refer to Azure [docs](https://docs.microsoft.com/en-us/azure/architecture/example-scenario/apps/jenkins).

4. Using a Bitnami Jenkins Stack:

    Refer to Bitnami [docs](https://docs.bitnami.com/general/apps/jenkins/).

### Configuring Jenkins instance:

1. Unlock your Jenkins instance:

    i. From the Jenkins console log output, copy the automatically-generated alphanumeric password.

    ii. On the Unlock Jenkins page, paste this password into the Administrator password field and click `Continue`.

2. Customizing Jenkins with plugins. Choose one option:

    i. `Install suggested plugins` - to install the recommended set of plugins, which are based on most common use cases.

    ii. `Select plugins to install` - to choose which set of plugins to initially install. When you first access the
    plugin selection page, the suggested plugins are selected by default.

3. Create an admin user:

    i. When the Create First Admin User page appears, specify the details for your administrator user in the respective fields and click Save and Finish.

    ii. When the Jenkins is ready page appears, click Start using Jenkins.

    iii. If required, log in to Jenkins with the credentials of the user you just created and you are ready to start using Jenkins!

</details>

## Minimum Requirements

1. Jenkins - 2.176.2 LTS or above
2. GitLab - 11.0 or above

## Installing plugin

1. Using [Plugin Management Tool](https://github.com/jenkinsci/plugin-installation-manager-tool)

    ```bash
    $ java -jar plugin-management-tool.jar
        -p gitlab-branch-source
        -d <path-to-default-jenkins-plugins-directory>
        -w <path-to-jenkins-war>
    ```
    
2. Install from Jenkins Update Center. Go to Jenkins > Configure > Manage Plugins > Available and search for `gitlab branch source plugin` then select Install.

3. From Source:

    i. Checkout out source code to your local machine:
    
         git clone https://github.com/baymac/gitlab-branch-source-plugin.git
    
         cd gitlab-branch-source-plugin
    
    ii. Install the plugin:
    
        mvn clean install
        
        mvn clean install -DskipTests # to skip tests
    
    iii. Run the Plugin:
    
        mvn hpi:run # runs a Jenkins instance at localhost:8080
    
        mvn hpi:run -Djetty.port={port} # to run on your desired port number
    
    iv. Now the `*.hpi` generated can be manually installed on your Jenkins instance:

        1. Select `Manage Jenkins`
    
        2. Select `Manage Plugins`
    
        3. Select `Advanced` tab
    
        3. In `Upload Plugin` section, select `Choose file`
    
        4. Select `${root_dir}/target/gitlab-branch-source.hpi`
    
        5. Select `Upload`
    
        6. Select `Install without restart`
	
4. Download latest release from [here](https://updates.jenkins.io/latest/gitlab-branch-source.hpi) and manually install.

## Initial Setup

After installing the plugin on your Jenkins instance, you need configure your GitLab Server settings.

### Setting up GitLab Server Configuration on Jenkins

1. On jenkins, select `Manage Jenkins`.

2. Select `Configure System`.

3. Scroll down to find the `GitLab` section.

   ![gitlab-section](/docs/img/add-server.png)

4. Select `Add GitLab Server` | Select `GitLab Server`.

5. Now you will now see the GitLab Server Configuration options

   ![gitlab-server](/docs/img/server-config.png)

   There are 4 fields that needs to be configured:

    i. `Name` - Plugin automatically generates an unique server name for you. User may want to configure this field to suit their needs but should make sure it is sufficiently unique. We recommend to keep it as it is.

    ii. `Server URL` - Contains the URL to your GitLab Server. By default it is set to "https://gitlab.com". User can modify it to enter their GitLab Server URL e.g. https://gitlab.gnome.org/, http://gitlab.example.com:7990. etc.

    iii. `Credentials` - Contains a list of credentials entries that are of type GitLab Personal Access Token. When no credential has been added it shows "-none-". User can add a credential by clicking "Add" button.

    iv. `Mange Web Hook` - If you want the plugin to setup web hook on your GitLab project(s) to get push/mr/tag/note events then check this box. 
    
    iv. `Mange System Hook` - If you want the plugin to setup system hook on your GitLab project(s) to detect if a project is removed then check this box. Remember plugin can only setup system hook on your server if supplied access token has `Admin` access. 

6. Adding a Personal Access Token Credentials:

   This is a manual setup. To automatically generate Personal Access Token see [next section](#creating-personal-access-token-within-jenkins).

    i. User is required to add a `GitLab Personal Access Token` type credentials entry to securely persist the token
    inside Jenkins.

    ii. Generate a `Personal Access Token` on your GitLab Server

        a. Select profile dropdown menu from top-right corner

        b. Select `Settings`

        c. Select `Access Token` from left column

        d. Enter a name | Set Scope to `api` (If admin also give `sudo` which required for systemhooks and mr comment trigger)

        e. Select `Create Personal Access Token`

        f. Copy the token generated

    iii. Return to Jenkins | Select `Add` in Credentials field | Select `Jenkins`.

    iv. Set `Kind` to GitLab Personal Access Token.

    v. Enter `Token`.

    vi. Enter a unique id in `ID`.

    vii. Enter a human readable description.

      ![gitlab-credentials](/docs/img/gitlab-credentials.png)

    viii. Select `Add`.

7. Testing connection:

    i. Select your desired token in the `Credentials` dropdown.

    ii. Select `Test Connection`.

    iii. It should return something like `Credentials verified for user {username}`.

8. Select `Apply` (at the bottom).

9. GitLab Server is now setup on Jenkins.

### Creating Personal Access Token within Jenkins

Alternatively, users can generate a GitLab Personal Access Token within Jenkins itself and automatically add the
GitLab Personal Access Token credentials to Jenkins server credentials.

1. Select `Advanced` at the bottom of `GitLab` Section.

2. Select `Manage Additional GitLab Actions`.

3. Select `Convert login and password to token`.

4. Set the `GitLab Server URL`.

5. There are 2 options to generate token:

    i. `From credentials` - To select an already persisting Username Password Credentials or add an Username Password
    credential to persist it.

    ii. `From login and password` - If this is a one time thing then you can directly enter you credentials to the text boxes and the username/password credential is not persisted.

6. After setting your username/password credential, select `Create token credentials`.

7. The token creator will create a Personal Access Token in your GitLab Server for the given user with the required scope and also create a credentials for the same inside Jenkins server. You can go back to the GitLab Server Configuration to select the new credentials generated (select "-none-" first then new credentials will appear). For security reasons this token is not revealed as plain text rather returns an `id`. It is a 128-bit long UUID-4 string (36 characters).

    ![gitlab-token-creator](/docs/img/gitlab-token-creator.png)
    
### Manually create hooks on GitLab Server

Use the following end points for web hooks and system hooks setup on your GitLab Server. The `Jenkins Url` needs to be a fully qualified domain name (FQDN).

#### WebHook

```
<jenkins_url>/gitlab-webhook/post
```

with `push`, `tag`, `merge request` and `note` events.


#### SystemHook

```
<jenkins_url>/gitlab-systemhook/post
```
with `repository update` event.

### Configuration as Code

There is an easier way to setup GitLab Server configuration on your Jenkins server. No need for messing around in the UI.
`Jenkins Configuration as Code (JCasC)` or simply `Configuration as Code` Plugin allows you to configure Jenkins
via a `yaml` file. If you are a first time user, you can learn more about JCasC
[here](https://github.com/jenkinsci/configuration-as-code-plugin).

#### Prerequisite:

Install `Configuration as Code` Plugin on your Jenkins instance.

Refer to [Installing a new plugin in Jenkins](https://jenkins.io/doc/book/managing/plugins/#installing-a-plugin).

#### Add configuration YAML:

There are multiple ways to load JCasC yaml file to configure Jenkins:

* JCasC by default searches for a file with the name `jenkins.yaml` in `$JENKINS_ROOT`.

* The JCasC looks for an environment variable `CASC_JENKINS_CONFIG` which contains the path for the configuration `yaml` file.

    * A path to a folder containing a set of config files e.g. `/var/jenkins_home/casc_configs`.

    * A full path to a single file e.g. `/var/jenkins_home/casc_configs/jenkins.yaml`.

    * A URL pointing to a file served on the web e.g. `https://<your-domain>/jenkins.yaml`.

* You can also set the configuration yaml path in the UI. Go to `<your-jenkins-domain>/configuration-as-code`. Enter path or URL to `jenkins.yaml` and select `Apply New Configuration`.

To configure your GitLab Server in Jenkins add the following to `jenkins.yaml`:

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
          - gitlabPersonalAccessToken:
              scope: SYSTEM
              id: "i<3GitLab"
              token: "XfsqZvVtAx5YCph5bq3r" # gitlab personal access token

unclassified:
  gitLabServers:
    servers:
      - credentialsId: "i<3GitLab" # same as id specified for gitlab personal access token credentials
        manageWebHooks: true
        manageSystemHooks: true # access token should have admin access to set system hooks
        name: "gitlab-3214"
        serverUrl: "https://gitlab.com"
```

See handling secrets [section](https://github.com/jenkinsci/configuration-as-code-plugin#handling-secrets) in JCasC documentation for better security.

## Jobs Setup

GitLab Branch Source Plugin allows you to create 2 type of jobs:

* `Multibranch Pipeline Jobs` - For single project.
* `Folder Organisation` - For multiple projects inside a owner (user/group/subgroup).

### Multibranch Pipeline Jobs

The Multibranch Pipeline job type enables you to implement different Jenkinsfiles for different branches of the same project. In a Multibranch Pipeline job, Jenkins automatically discovers, manages and executes Pipelines for Branches/Merge Requests/Tags which contain a `Jenkinsfile` in source control. This eliminates the need for manual Pipeline creation and management.

To create a `Multibranch Pipeline Job`:

1. Select `New Item` on Jenkins home page.

2. Enter a name for your job, select `Multibranch Pipeline` | select `Ok`.

3. In `Branch Sources` sections, select `Add source` | select `GitLab Project`.

4. Now you need to configure your jobs.
 
    ![branch-source](/docs/img/branch-source.png)
    
    i. Select `Server` configured in the initial server setup.
    
    ii. [Optional] Add `Checkout Credentials` (SSHPrivateKey or Username/Password) if there is any private projects that will be built by the plugin.
    
    iii. Add path to the owner where the project you want to build exists. If user, enter `username`. If group, enter `group name`. If subgroup, enter `subgroup path with namespace`.
    
    iv. Based on the owner provided. All the projects are discovered in the path and added to the `Projects` listbox. You can now choose the project you want to build.
    
    v. `Behaviours` (a.k.a. SCM Traits) allow different configurations option to your build. More about it in the SCM Trait APIs section.

5. Now you can go ahead and save the job.

For more info see [this](https://jenkins.io/doc/book/pipeline/multibranch/).

After saving, a new web hook is created in your GitLab Server if a `GitLab Access Token` is specified in the server configuration. Then the branch indexing starts based on what options behaviours you selected. As the indexing proceeds new jobs are started and queued for each branches with a `Jenkinsfile` in their root directory.

The Job results are notified to the GitLab Server as Pipeline Status for the HEAD commit of each branches built. The build for forked MR cannot be notified to GitLab Server as GitLab doesn't provide Pipeline status for Merge Requests from forks for security concerns. See [this](https://docs.gitlab.com/ee/ci/merge_request_pipelines/#important-notes-about-merge-requests-from-forked-projects).

We have a workaround for this. Jenkins will build the MRs from forked projects if the MR author is a trusted owner i.e. has `Developer`/`Maintainer`/`Owner` access level. More about it in the SCM Trait APIs section.

As the web hook is now setup on your Jenkins CI by the GitLab server. Any push-events or merge-request events or tag events trigger the concerned build in Jenkins. 

### Folder Organization

Folders Organization enable Jenkins to monitor an entire GitLab `User`/`Group`/`Subgroup` and automatically create new Multibranch Pipelines for projects which contain branches/merge requests/tags containing a `Jenkinsfile`. In our plugin this type of job is called `GitLab Group`.

To create a `GitLab Group Job`:

1. Select `New Item` on Jenkins home page.

2. Enter a name for your job, select `GitLab Group` | select `Ok`.

3. Now you need to configure your jobs.
    
    i. Select `Server` configured in the initial server setup.

    ii. [Optional] Add `Checkout Credentials` (SSHPrivateKey or Username/Password) only if there are any private projects required to be built.

    iii. Add path to the owner whose projects you want to build. If user, enter `username`. If group, enter `group name`. If subgroup, enter `subgroup path with namespace`.

    v. `Behaviours` (a.k.a. SCM Traits) are allow different configuration option to your build. More about it in the SCM Trait APIs section.

The indexing in this group job type only needs to discover one branch with`Jenkinsfile` and thus it only shows the partial indexing log. You need to visit individual projects to see their full indexing.

### SCM Trait APIs

The following behaviours apply to both `Multibranch Pipeline Jobs` and `Folder Organization` (unless otherwise stated).

#### Default Traits:

* `Discover branches` - To discover branches.

	* `Only Branches that are not also filed as MRs` - If you are discovering origin merge requests, it may not make sense to discover the same changes both as a merge request and as a branch.
	* `Only Branches that are filed as MRs` - This option exists to preserve legacy behaviour when upgrading from older versions of the plugin. NOTE: If you have an actual use case for this option please file a merge request against this text.
	* `All Branches` - Ignores whether the branch is also filed as a merge request and instead discovers all branches on the origin project.

* `Discover merge requests from origin` - To discover merge requests made from origin branches.
	
	* `Merging the merge request merged with current target revision` - Discover each merge request once with the discovered revision corresponding to the result of merging with the current revision of the target branch.
	* `The current merge request revision` - Discover each merge request once with the discovered revision corresponding to the merge request head revision without merging.
	* `Both current mr revision and the mr merged with current target revision` - Discover each merge request twice. The first discovered revision corresponds to the result of merging with the current revision of the target branch in each scan. The second parallel discovered revision corresponds to the merge request head revision without merging.

* `Discover merge requests from forks` - To discover merge requests made from forked project branches.

	* Strategy:
	
		* `Merging the merge request merged with current target revision` - Discover each merge request once with the discovered revision corresponding to the result of merging with the current revision of the target branch.
		* `The current merge request revision` - Discover each merge request once with the discovered revision corresponding to the merge request head revision without merging.
		* `Both current mr revision and the mr merged with current target revision` - Discover each merge request twice. The first discovered revision corresponds to the result of merging with the current revision of the target branch in each scan. The second parallel discovered revision corresponds to the merge request head revision without merging.
		
	* Trust
	
		* `Members` - Discover MRs from Forked Projects whose author is a member of the origin project.
		* `Trusted Members` - [Recommended] Discover MRs from Forked Projects whose author is has Developer/Maintainer/Owner accesslevel in the origin project.
		* `Everyone` - Discover MRs from Forked Projects filed by anybody. For security reasons you should never use this option. It may be used to reveal your Pipeline secrets environment variables.
		* `Nobody` - Discover no MRs from Forked Projects at all. Equivalent to removing the trait altogether.
		
	If `Members` or `Trusted Members` is selected, then plugin will build the target branch of MRs from non/untrusted members.

#### Additional Traits:

These traits can be selected by selecting `Add` in the `Behaviours` section.

* `Tag discovery` - Discover tags in the project. To automatically build tags install `basic-branch-build-plugin`.

* `Discover group/subgroup projects` - Discover subgroup projects inside a group/subgroup. Only applicable to `GitLab Group` Job type whose owner is a `Group`/`Subgroup` but not `User`.

* `Log build status as comment on GitLab` - Enable logging build status as comment on GitLab. A comment is logged on the commit or merge request once the build is completed. You can decide if you want to log success builds or not. You can also use sudo user to comment the build status as commment e.g. `jenkinsadmin` or something similar. 

* `Trigger build on merge request comment` - Enable trigger a rebuild of a merge request by comment with your desired comment body (default: `jenkins rebuild`). The job can only be triggered by trusted members of the project i.e. users with Developer/Maintainer/Owner accesslevel.

* `Disable GitLab project avatar` - Disable avatars of GitLab project(s). It is not possible to fetch avatars when API has no token authentication or project is private. So you may use this option as a workaround. We will fix this issue in a later release.

* `Project Naming Strategy` - Choose whether you want `project name` or the `project path (with namespace)` as job names of each project. Users generally prefer the first option but due to legacy reasons we have `project path (with namespace)` as default naming scheme. Note if a job is already created and the naming strategy is changed it will cause projects and build logs to be destroyed.

* `Filter by name (with regex)` - Filter the type of items you want to discover in your project based on the regular expression specified. For example, to discover only `master` branch, `develop` branch and all Merge Requests add `(master|develop|MR-.*)`.

* `Filter by name (with wildcards)` - Filter the type of items you want to discover in your project based on the wildcards specified. For example, to discover only `master` branch, `develop` branch and all Merge Requests add `development master MR-*`.

* `Skip pipeline status notifications` - Disable notifying GitLab server about the pipeline status.

* `Override hook management modes` - Override default hook management mode of web hook and system hook. `ITEM` credentials for webhook is currently not supported.

* `Checkout over SSH` - [Not Recommended] Use this mode to checkout over SSH. Use `Checkout Credentials` instead.

## Job DSL seed job configuration

To create a Job DSL seed job see this [tutorial](https://github.com/jenkinsci/job-dsl-plugin/wiki/Tutorial---Using-the-Jenkins-Job-DSL).

Here is a sample seed job script for folder organisation job:

```groovy
organizationFolder('GitLab Organization Folder') {
    description("GitLab org folder created with Job DSL")
    displayName('My Project')
    // "Projects"
    organizations {
        gitLabSCMNavigator {
            projectOwner("baymac")
            credentialsId("i<3GitLab")
            serverName("gitlab-3214")
            // "Traits" ("Behaviours" in the GUI) that are "declarative-compatible"
            traits {
                subGroupProjectDiscoveryTrait() // discover projects inside subgroups
                gitLabBranchDiscovery {
                    strategyId(3) // discover all branches
                }
                originMergeRequestDiscoveryTrait {
                    strategyId(1) // discover MRs and merge them with target branch
                }
                gitLabTagDiscovery() // discover tags
            }
        }
    }
    // "Traits" ("Behaviours" in the GUI) that are NOT "declarative-compatible"
    // For some 'traits, we need to configure this stuff by hand until JobDSL handles it
    // https://issues.jenkins.io/browse/JENKINS-45504
    configure { 
        def traits = it / navigators / 'io.jenkins.plugins.gitlabbranchsource.GitLabSCMNavigator' / traits
        traits << 'io.jenkins.plugins.gitlabbranchsource.ForkMergeRequestDiscoveryTrait' {
            strategyId(2)
            trust(class: 'io.jenkins.plugins.gitlabbranchsource.ForkMergeRequestDiscoveryTrait$TrustPermission')
        }
    }
    // "Project Recognizers"
    projectFactories {
        workflowMultiBranchProjectFactory {
            scriptPath 'Jenkinsfile'
        }
    }
    // "Orphaned Item Strategy"
    orphanedItemStrategy {
        discardOldItems {
            daysToKeep(10)
            numToKeep(5)
        }
    }
    // "Scan Organization Folder Triggers" : 1 day
    // We need to configure this stuff by hand because JobDSL only allow 'periodic(int min)' for now
    triggers {
        periodicFolderTrigger {
            interval('1d')
        }
    }
}
```

To see all the APIs supported by Job DSL you can visit the following link:

```
http://localhost:8080/jenkins/plugin/job-dsl/api-viewer/index.html#path/organizationFolder-organizations-gitLabSCMNavigator-traits
```

## JCasC configuration to create job

You can also use JCasC to directly create job from a Job DSL seed job. Here's an example of the yaml config:

```groovy
jobs:
  - script: >
      organizationFolder('GitLab Organization Folder') {
        description("GitLab org folder configured with JCasC")
        displayName('My Project')
        // "Projects"
        organizations {
          gitLabSCMNavigator {
            projectOwner("baymac")
            credentialsId("i<3GitLab")
            serverName("gitlab-3214")
            // "Traits" ("Behaviours" in the GUI) that are "declarative-compatible"
            traits {
              subGroupProjectDiscoveryTrait() // discover projects inside subgroups
              gitLabBranchDiscovery {
                strategyId(3) // discover all branches
              }
              originMergeRequestDiscoveryTrait {
                strategyId(1) // discover MRs and merge them with target branch
              }
              gitLabTagDiscovery() // discover tags
            }
          }
        }
        // "Traits" ("Behaviours" in the GUI) that are NOT "declarative-compatible"
        // For some 'traits, we need to configure this stuff by hand until JobDSL handles it
        // https://issues.jenkins.io/browse/JENKINS-45504
        configure { node ->
            def traits = node / navigators / 'io.jenkins.plugins.gitlabbranchsource.GitLabSCMNavigator' / traits
            traits << 'io.jenkins.plugins.gitlabbranchsource.ForkMergeRequestDiscoveryTrait' {
                strategyId('2')
                trust(class: 'io.jenkins.plugins.gitlabbranchsource.ForkMergeRequestDiscoveryTrait$TrustPermission')
            }
        }
        // "Project Recognizers"
        projectFactories {
            workflowMultiBranchProjectFactory {
                scriptPath 'Jenkinsfile'
            }
        }
        // "Orphaned Item Strategy"
        orphanedItemStrategy {
          discardOldItems {
            daysToKeep(-1)
            numToKeep(-1)
          }
        }
        // "Scan Organization Folder Triggers" : 1 day
        // We need to configure this stuff by hand because JobDSL only allow 'periodic(int min)' for now
        configure { node ->
          node / triggers / 'com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger' {
            spec('H H * * *')
            interval(86400000)
          }
        }
      }
```

You can also use file or url to load the script, see [this](https://github.com/jenkinsci/job-dsl-plugin/wiki/JCasC).

## Known Issues

* System Hook feature is still in beta. The plugin only detects when a new project is created. It is not able to detect when a project is destroyed or updated. For the changes to reflect branch indexing needs to be performed manually or wait for the automatic trigger of branch indexing (configured in the job).

## How to talk to us?

* This project uses [Jenkins JIRA](https://issues.jenkins-ci.org/) to track issues. You can file issues under [`gitlab-branch-source-plugin`](https://issues.jenkins-ci.org/issues/?jql=project+%3D+JENKINS+AND+component+%3D+gitlab-branch-source-plugin) component.

* Send your mail in the [Developer Mailing list](https://groups.google.com/forum/#!forum/jenkinsci-dev).

* Join our [Gitter channel](https://gitter.im/jenkinsci/gitlab-branch-source-plugin).

## Acknowledgements

This plugin is built and maintained by the Google Summer of Code (GSoC) Team for [Multibranch Pipeline Support for GitLab](https://jenkins.io/projects/gsoc/2019/gitlab-support-for-multibranch-pipeline/).

Maintainers:

* [Parichay](https://github.com/baymac)
* [Marky](https://github.com/markyjackson-taulia) 
* [Joseph](https://github.com/casz) 
* [Justin](https://github.com/justinharringa) 

External Support:

* [Oleg](https://github.com/oleg-nenashev) 
* [Greg](https://github.com/gmessner) (The maintainer of GitLab4J APIs)
* [Stephen](https://github.com/stephenc) (The maintainer of SCM related Jenkins Plugins)

Also thanks to entire Jenkins community for contributing with technical expertise and inspiration.
