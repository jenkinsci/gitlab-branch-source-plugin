# GitLab Branch Source Plugin

**This repository contains source code for:**

* GitLab Server Plugin
* GitLab Branch Source Plugin

**This plugin is being developed as a part of GSoC 2019 project [Multi branch Pipeline Support for GitLab](https://jenkins.io/projects/gsoc/2019/gitlab-support-for-multibranch-pipeline/).**

To fully be able to run a Jenkins Continuous Integration on a GitLab repository or project, you require three plugins:

1. [GitLab API Plugin](https://github.com/jenkinsci/gitlab-api-plugin) - Wraps GitHub Java API

2. [GitLab Plugin](https://github.com/jenkinsci/gitlab-plugin/) - Server configuration and web hooks Management 

3. GitLab Branch Source Plugin - To support Multi branch Pipeline Jobs (including Merge Requests) and Folder organisation

## Jenkins and GitLab Integration:

This section contains information related to how GitLab and Jen

### Present State

1. FreeStyle Job and Pipeline(Single Branch) Job are fully supported.

2. Multi branch Pipeline Job is partially supported (no MRs detection).

3. GitLab Folder Organisation is not supported.

### Issues

1. The GitLab Java APIs are written within the plugin itself when it should be a different plugin.

2. Multi branch Pipeline support is missing.

3. GitLab Folder Organisation for GitLab Projects is missing.

4. Convention for 3 separate plugin is not followed (e.g. - github-api-plugin, github-plugin, github-branch-source-plugin).

### Goals of this project

1. Implement a lightweight GitLab Plugin that depends on GitLab API Plugin

2. Follow convention of 3 separate plugins (as listed above)

3. Implement GitLab Branch Source Plugin

4. Support new Jenkins features such as [Jenkins Code as Configuration](https://github.com/jenkinsci/configuration-as-code-plugin) (JCasc)

5. Provide rich user experience

6. Support all SCM Trait APIs

7. Fully support Java 8

## Building the plugin

No binaries are available for this plugin as the plugin is in the very early alpha stage, and not ready for the general public quite yet.  If you want to jump in early, you can try building it yourself from source.

### Installation:

1) Checkout out source code to your local machine:

     ```bash
     git clone https://github.com/baymac/gitlab-branch-source-plugin.git\
    
     cd gitlab-branch-source-plugin
     ```

2) Install the plugin:
    
    ```bash
    mvn clean install 
    ```
    or
    ```
    mvn clean install -DskipTests # to skip tests
    ```

3) Run the Plugin:

    ```bash
    mvn hpi:run # runs a Jenkins instance at localhost:8080
    ```
    
    or
    
    ```bash
    mvn hpi:run -Djetty.port={port} # to run on your desired port number 
    ```

    If you want to test it with your Jenkins server, after `mvn clean install` follow these steps in your Jenkins instance:
    
    1. Select `Manage Jenkins`
    
    2. Select `Manage Plugins`
    
    3. Select `Advanced` tab
    
    3. In `Upload Plugin` section, select `Choose file`
    
    4. Select `${root_dir}/target/gitlab-branch-source.hpi`
    
    5. Select `Upload` 
    
    6. Select `Install without restart`
    
## Getting Started

Jenkins is an open source, self hosting automation server for continuous integration and continuous delivery. The source
code of the core Jenkins and its plugins are written in Java. There are developments
on a modern Jenkins Server (see: [Blueocean Plugin](https://github.com/jenkinsci/blueocean-plugin)) using React and other modern front end tools to provide rich user experience.

For more Jenkins related information, see [documentation](https://jenkins.io/doc/).

### Extending Jenkins 

Jenkins has more than a 1000 plugins so a vast set of functionality has already been implemented and this can be used to leverage
new plugins. Jenkins has an extensibility feature that allows plugin to use other plugins or
core features simply by extending their classes. To define or create an extension in Jenkins, 
we use the `@Extension` annotation type. This annotation is picked up by Jenkins, and the new extension will be added to an
`ExtensionList` object, where the extension can then be found via `ExtensionFinder`.

### Running your Jenkins server:

There are multiple ways to do it:

1. Using a Jenkins docker:
    
    i. Download docker image from [here](https://hub.docker.com/r/jenkins/jenkins)
    
    ii. Open up a terminal/command prompt window to the download directory
    
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
    
    iv. Browse to `http://localhost:8080`
    
    If you need more information about docker commands, see [here](https://jenkins.io/doc/book/installing/).
    
2. Using a Jenkins Web application Archive (WAR) :

    i. Download [latest stable Jenkins WAR file](http://mirrors.jenkins.io/war-stable/latest/jenkins.war)
    
    ii. Open up a terminal/command prompt window to the download directory
    
    iii. Run command
    
    ```bash
    java -jar jenkins.war
    ```
    
    iv. Browse to `http://localhost:8080/jenkins`
    
3. Using a Jenkins Azure solution from marketplace:

    Refer to Azure [docs](https://docs.microsoft.com/en-us/azure/architecture/example-scenario/apps/jenkins).
   
4. Using a Bitnami Jenkins Stack:

    Refer to Bitnami [docs](https://docs.bitnami.com/general/apps/jenkins/)
    
### Post installation:

1. Unlock your Jenkins instance:

    i. From the Jenkins console log output, copy the automatically-generated alphanumeric password
   
    ii. On the Unlock Jenkins page, paste this password into the Administrator password field and click Continue
    
2. Customizing Jenkins with plugins

   Click one of the two options shown:
       
    i. `Install suggested plugins` - to install the recommended set of plugins, which are based on most common use cases.
       
    ii. `Select plugins to install` - to choose which set of plugins to initially install. When you first access the plugin selection page, the suggested plugins are selected by default.
    
3. Create an admin user

    i. When the Create First Admin User page appears, specify the details for your administrator user in the respective fields and click Save and Finish.
    
    ii. When the Jenkins is ready page appears, click Start using Jenkins.
    
    iii. If required, log in to Jenkins with the credentials of the user you just created and you are ready to start using Jenkins!

## Usage

Assuming plugin installation has been done already.

### Setting up GitLab Server Configuration on Jenkins

1. On jenkins, select `Manage Jenkins`

2. Select `Configure System`

3. Scroll to find the `GitLab` section

![gitlab-section](docs/img/gitlab-section.png)








