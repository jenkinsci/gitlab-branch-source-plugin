[![Build Status](https://ci.jenkins.io/job/Plugins/job/gitlab-branch-source-plugin/job/develop/badge/icon)](https://ci.jenkins.io/job/Plugins/job/gitlab-branch-source-plugin/job/develop/)
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

## Installing plugin

This plugin is still in Alpha stage. `gitlab-branch-source-0.0.5-alpha-2` release has been made to Jenkins Experimental Update Center. You can try it out by following ways:

1. Using [Plugin Management Tool](https://github.com/jenkinsci/plugin-installation-manager-tool)

    ```bash
    $ java -jar plugin-management-tool.jar
        -p gitlab-branch-source:experimental
        -d <path-to-default-jenkins-plugins-directory>
        -w <path-to-jenkins-war>
    ```

2. Changing update center URL on Jenkins Instance

    You can install plugins from Experimental Update Center by changing the JSON URL used to fetch plugins data. Go to `Plugin Manager`, then to the `Advanced` tab, and configure the update center URL:
     ```
     https://updates.jenkins.io/experimental/update-center.json
     ``` 
     then `submit`, and then select `Check Now`. Experimental plugin updates will be marked as such on the `Available` and `Updates` tabs of the Plugin Manager.

3. Download *.hpi from [here](http://updates.jenkins-ci.org/download/plugins/gitlab-branch-source/0.0.5-alpha-2/gitlab-branch-source.hpi) and manually install.

4. From Source:

    i. Checkout out source code to your local machine:
    
         git clone https://github.com/baymac/gitlab-branch-source-plugin.git\
    
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

## Initial Setup

After installing the plugin on your Jenkins instance, you need configure your GitLab Server settings.

### Setting up GitLab Server Configuration on Jenkins

1. On jenkins, select `Manage Jenkins`.

2. Select `Configure System`.

3. Scroll down to find the `GitLab` section.

   ![gitlab-section](https://user-images.githubusercontent.com/23079344/61185124-1d07a180-a673-11e9-898c-cd4e8c3e279f.png)

4. Select `Add GitLab Server` | Select `GitLab Server`.

5. Now you will now see the GitLab Server Configuration options

   ![gitlab-server](https://user-images.githubusercontent.com/23079344/61185125-1e38ce80-a673-11e9-9aad-24b56b43745f.png)

   There are 4 fields that needs to be configured:

    i. `Name` - Plugin automatically generates an unique server name for you. User may want to configure this field to suit their needs but should make sure it is sufficiently unique. We recommend to keep it as it is.

    ii. `Server URL` - Contains the URL to your GitLab Server. By default it is set to "https://gitlab.com". User canmodify it to enter their GitLab Server URL e.g. https://gitlab.gnome.org/, http://gitlab.example.com:7990. etc.

    iii. `Credentials` - Contains a list of credentials entries that are of type GitLab Personal Access Token. When no credential has been added it shows "-none-". User can add a credential by clicking "Add" button.

    iv. `Web Hook` - This field is a checkbox. If you want the plugin to setup a webhook on your GitLab project(s) related jobs, check this box. The plugin listens to a URL for the concerned GitLab project(s) and when an event occurs in the GitLab Server, the server sends an event trigger to the URL where the web hook is setup. If you want continuous integration (or continuous delivery) on your GitLab project then you may want to automatically set it up.

6. Adding a Personal Access Token Credentials:

   This is a manual setup. To automatically generate Personal Access Token see [next section](#creating-personal-access-token-within-jenkins).

    i. User is required to add a `GitLab Personal Access Token` type credentials entry to securely persist the token
    inside Jenkins.

    ii. Generate a `Personal Access Token` on your GitLab Server

        a. Select profile dropdown menu from top-right corner

        b. Select `Settings`

        c. Select `Access Token` from left column

        d. Enter a name | Set Scope to `api`,`read_user`, `read_repository`

        e. Select `Create Personal Access Token`

        f. Copy the token generated

    iii. Return to Jenkins | Select `Add` in Credentials field | Select `Jenkins`.

    iv. Set `Kind` to GitLab Personal Access Token.

    v. Enter `Token`.

    vi. Enter a unique id in `ID`.

    vii. Enter a human readable description.

      ![gitlab-credentials](https://user-images.githubusercontent.com/23079344/61185123-1bd67480-a673-11e9-97dc-83b0f4c4bcf9.png)

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

    ii. `From login and password` - If this is a one time thing then you can directly enter you credentials to the text boxes
    and the username/password credential is not persisted.

6. After setting your username/password credential, select `Create token credentials`.

7. The token creator will create a Personal Access Token in your GitLab Server for the given user with the required scope and also create a credentials for the same inside Jenkins server. You can go back to the GitLab Server Configuration to select the new credentials generated (select "-none-" first then new credentials will appear). For security reasons this token is not revealed as plain text rather returns an `id`. It is a 128-bit long UUID-4 string (36 characters).

    ![gitlab-token-creator](https://user-images.githubusercontent.com/23079344/61185126-1f69fb80-a673-11e9-9c82-c24c6c132347.png)

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
        manageHooks: true
        name: "gitlab-1024"
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
    
    v. `Behaviours` (a.k.a SCM Traits) are allow different configuration option to your build. More about it in the SCM Trait APIs section.

5. Now you can go ahead and save the job.

For more info see [this](https://jenkins.io/doc/book/pipeline/multibranch/).

After saving, a new webhook is created in your GitLab Server if a `GitLab Access Token` is specified in the server configuration. Then the branch indexing starts based on what options behaviours you selected. As the indexing proceeds new jobs are started and queued for each branches with a `Jenkinsfile` in their root directory.

The Job results are notified to the GitLab Server as Pipeline Status for the HEAD commit of each branches built. The build for forked MR cannot be notified to GitLab Server as GitLab doesn't provide Pipeline status for Merge Requests from forks for security concerns. See [this](https://docs.gitlab.com/ee/ci/merge_request_pipelines/#important-notes-about-merge-requests-from-forked-projects).

We have a workaround for this. Jenkins will build the MRs from forked projects if the MR author is a trusted owner i.e. has `Developer`/`Maintainer`/`Owner` access level. More about it in the SCM Trait APIs section.

As the webhook is now setup on your Jenkins CI by the GitLab server. Any push-events or merge-request events or tag events trigger the required build in Jenkins. Currently this feature is a work in progress and will be landing very soon. 🚀 Will be ready once [JENKINS-58593](https://issues.jenkins-ci.org/browse/JENKINS-58593) is fixed.

### Folder Organization

Folders Organization enable Jenkins to monitor an entire GitLab `User`/`Group`/`Subgroup` and automatically create new Multibranch Pipelines for projects which contain branches/merge requests/tags containing a `Jenkinsfile`. In our plugin this type of job is called `GitLab Group`.

To create a `GitLab Group Job`:

1. Select `New Item` on Jenkins home page.

2. Enter a name for your job, select `GitLab Group` | select `Ok`.

3. Now you need to configure your jobs.
    
    i. Select `Server` configured in the initial server setup.

    ii. [Optional] Add `Checkout Credentials` (SSHPrivateKey or Username/Password) if there is any private projects that will be built by the plugin.

    iii. Add path to the owner whose projects you want to build. If user, enter `username`. If group, enter `group name`. If subgroup, enter `subgroup path with namespace`.

    v. `Behaviours` (a.k.a SCM Traits) are allow different configuration option to your build. More about it in the SCM Trait APIs section.

Currently there is a bug which doesn't show proper projects indexing log in Jenkins. But behind the scenes indexing is done properly. Will be fixed in [JENKINS-58446](https://issues.jenkins-ci.org/browse/JENKINS-58446).

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
		
	If `Members` or `Trusted Members` is selected, plugin will build the MRs from Forked Projects with untrusted owners based on their target branch.

#### Additional Traits:

These traits can be selected by clicking `Add` button in the `Behaviours` section.

* `Skip notification` - Do not notify GitLab about pipeline status.

* `WebHook mode` - Override default webhook management mode.

* `Checkout over SSH` - Use this mode to checkout over ssh. It is recommended to use `Checkout Credentials` instead.

* `Tag discovery` - Discover tags in the project.

* `Discover group/subgroup projects` - Discovers group/subgroup projects inside the owner. For example, discovers subgroups' projects. Only applicable to `GitLab Group` Job type.

## Issues

This project uses Jenkins [JIRA](https://issues.jenkins-ci.org/) to track issues. You can file issues under `gitlab-branch-source-plugin` component.

## Acknowledgements

This plugin is built and maintained by the Google Summer of Code (GSoC) Team for [Multibranch Pipeline Support for GitLab](https://jenkins.io/projects/gsoc/2019/gitlab-support-for-multibranch-pipeline/).

Maintainers:

* [Parichay](https://github.com/baymac)
* [Marky](https://github.com/markyjackson-taulia) 
* [Joseph](https://github.com/casz) 
* [Justin](https://github.com/justinharringa) 

External Support:

* [Oleg](https://github.com/oleg-nenashev) (Helped with technical issues and reviews)
* [Greg](https://github.com/gmessner) (The maintainer of GitLab4J APIs)
* [Stephen](https://github.com/stephenc) (The maintainer of SCM related Jenkins Plugins)

Also thanks to entire Jenkins community for contributing with technical expertise and inspiration.
