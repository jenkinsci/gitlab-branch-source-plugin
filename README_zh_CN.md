[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/jenkinsci/gitlab-branch-source-plugin)
[![构建状态](https://ci.jenkins.io/job/Plugins/job/gitlab-branch-source-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/gitlab-branch-source-plugin/job/master/)
[![GitHub 发布](https://img.shields.io/github/release/jenkinsci/gitlab-branch-source-plugin.svg?label=release)](https://github.com/jenkinsci/gitlab-branch-source-plugin/releases/latest)
[![Gitter](https://badges.gitter.im/jenkinsci/gitlab-branch-source-plugin.svg)](https://gitter.im/jenkinsci/gitlab-branch-source-plugin)
[![Jenkins 插件安装量](https://img.shields.io/jenkins/plugin/i/gitlab-branch-source.svg?color=blue)](https://plugins.jenkins.io/gitlab-branch-source)

# GitLab 分支源插件 (GitLab Branch Source Plugin)

为了能够全方位在 GitLab 仓库或项目上运行 Jenkins 持续集成，您需要以下插件：

* [GitLab API Plugin](https://github.com/jenkinsci/gitlab-api-plugin) - 封装了 GitLab Java API。

* GitLab Branch Source Plugin - 包含两个包：

     * `io.jenkins.plugins.gitlabserverconfig` - 管理服务器配置和 Web Hook 管理。理想情况下，这应该位于名为 `GitLab Plugin` 的另一个插件中。未来，该包将被移动到一个新插件中。

     * `io.jenkins.plugins.gitlabbranchsource` - 为多分支流水线作业（包括合并请求）和文件夹组织添加 GitLab 分支源。

## 入门指南

<details>
<summary>Jenkins 入门：</summary><br>

Jenkins 是一个开源的、自托管的自动化服务器，用于持续集成和持续交付。Jenkins 核心及其插件的源代码是用 Java 编写的。现代 Jenkins 服务器（参见：[Blueocean 插件](https://github.com/jenkinsci/blueocean-plugin)）已经有了新的进展，使用 React 和其他现代前端工具提供丰富的用户体验。

有关更多 Jenkins 相关信息，请参阅[文档](https://jenkins.io/doc/)。

### 扩展 Jenkins

Jenkins 拥有 1000 多个插件，因此已经实现了大量功能，可用于杠杆化新插件。Jenkins 具有可扩展性，允许插件通过简单的继承类来使用其他插件或核心功能。在 Jenkins 中定义或创建扩展时，我们使用 `@Extension` 注解。这个注解会被 Jenkins 识别，新的扩展将被添加到 `ExtensionList` 对象中，然后可以通过 `ExtensionFinder` 找到该扩展。

### 运行 Jenkins 服务器：

这里有几种设置您自己的 Jenkins 服务器的方法：

1. 使用 Jenkins Docker 镜像：

    i. 从[这里](https://hub.docker.com/r/jenkins/jenkins)下载 Docker 镜像。

    ii. 打开终端/命令提示符窗口并进入下载目录。

    iii. 运行以下命令：

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

    iv. 浏览器访问 `http://localhost:8080`。

    如果您需要有关 Docker 命令的更多信息，请参阅[此处](https://jenkins.io/doc/book/installing/#on-macos-and-linux)。

2. 使用 Jenkins Web 应用程序归档文件 (WAR)：

    i. 下载[最新的稳定版 Jenkins WAR 文件](https://get.jenkins.io/war-stable/latest/jenkins.war)。

    ii. 打开终端/命令提示符窗口并进入下载目录。

    iii. 运行命令：

    ```bash
    java -jar jenkins.war
    ```

    iv. 浏览器访问 `http://localhost:8080/jenkins`。

3. 使用 Azure Jenkins 解决方案：

    参考 Azure [文档](https://docs.microsoft.com/en-us/azure/architecture/example-scenario/apps/jenkins)。

4. 使用 Bitnami Jenkins Stack：

    参考 Bitnami [文档](https://docs.bitnami.com/general/apps/jenkins/)。

5. 使用 [Jenkins CLI](https://github.com/jenkins-zh/jenkins-cli) 进行开发运行：

    通过以下方式运行：`jcli plugin run`

### 配置 Jenkins 实例：

1. 解锁您的 Jenkins 实例：

    i. 从 Jenkins 控制台日志输出中，复制自动生成的字母数字密码。

    ii. 在解锁 Jenkins 页面上，将此密码粘贴到“管理员密码”字段中，然后单击“继续”。

2. 使用插件自定义 Jenkins。选择一个选项：

    i. `安装建议的插件` - 安装推荐的插件集，这些插件基于最常见的用例。

    ii. `选择要安装的插件` - 选择要初始安装的插件集。当您首次访问插件选择页面时，默认会选中建议的插件。

3. 创建管理员用户：

    i. 当出现“创建第一个管理员用户”页面时，在相应字段中指定管理员用户的详细信息，然后单击“保存并完成”。

    ii. 当出现“Jenkins 已就绪”页面时，单击“开始使用 Jenkins”。

    iii. 如果需要，使用刚刚创建的用户凭据登录 Jenkins，然后就可以开始使用 Jenkins 了！

</details>

## 最低要求

1. Jenkins - 2.176.2 LTS 或更高版本
2. GitLab - 11.0 或更高版本

## 安装插件

您可以使用以下任一方式：

1. 从 Jenkins 更新中心安装。转到 Jenkins > 配置 > 管理插件 > 可选插件，搜索 `gitlab branch source plugin`，然后选择“安装”。

2. 使用[插件管理工具](https://github.com/jenkinsci/plugin-installation-manager-tool)

    ```bash
    java -jar plugin-management-tool.jar
        -p gitlab-branch-source
        -d <path-to-default-jenkins-plugins-directory>
        -w <path-to-jenkins-war>
    ```

3. 从源码安装：

    i. 将源代码检出到本地计算机：

    ```
    git clone https://github.com/jenkinsci/gitlab-branch-source-plugin.git
    cd gitlab-branch-source-plugin
    ```

    ii. 安装插件：
    ```
    mvn clean install
        或
    mvn clean install -DskipTests # 跳过测试
    ```

    iii. 运行插件：

    ```
    mvn hpi:run # 在 localhost:8080 运行一个 Jenkins 实例
        或
    mvn hpi:run -Djetty.port={port} # 在您想要的端口号上运行
    ```

    iv. 现在可以将生成的 `*.hpi` 手动安装到您的 Jenkins 实例中：
    ```
    1. 选择 `管理 Jenkins` (Manage Jenkins)

    2. 选择 `插件管理` (Manage Plugins)

    3. 选择 `高级` (Advanced) 选项卡

    3. 在 `上传插件` (Upload Plugin) 部分，选择 `选择文件` (Choose file)

    4. 选择 `${root_dir}/target/gitlab-branch-source.hpi`

    5. 选择 `上传` (Upload)

    6. 选择 `安装而不重启` (Install without restart)
	```
4. 从[这里](https://updates.jenkins.io/latest/gitlab-branch-source.hpi)下载最新版本并手动安装。

## 初始设置

在您的 Jenkins 实例上安装插件后，您需要配置 GitLab 服务器设置。

### 在 Jenkins 上设置 GitLab 服务器配置

1. 在 Jenkins 上，选择 `管理 Jenkins`。

2. 选择 `系统配置` (Configure System)。

3. 向下滚动找到 `GitLab` 部分。

   ![gitlab-section](/docs/img/add-server.png)

4. 选择 `Add GitLab Server` | 选择 `GitLab Server`。

5. 现在您将看到 GitLab 服务器配置选项

   ![gitlab-server](/docs/img/server-config.png)

   有 4 个字段需要配置：

    i. `Name` - 插件会自动为您生成一个唯一的服务器名称。用户可以根据需要配置此字段，但应确保它是足够唯一的。我们建议保持原样。

    ii. `Server URL` - 包含您的 GitLab 服务器的 URL。默认设置为 "https://gitlab.com"。用户可以修改它以输入其 GitLab 服务器 URL，例如 https://gitlab.gnome.org/、http://gitlab.example.com:7990 等。

    iii. `Credentials` - 包含类型为 GitLab 个人访问令牌 (Personal Access Token) 或任何字符串凭据 (String Credentials) 的凭证条目列表。未添加任何凭据时显示 "-none-"。用户可以通过点击 "Add" 按钮添加凭据。

    iv. `Mange Web Hook` - 如果您希望插件在您的 GitLab 项目上设置 Web Hook 以获取推送/合并请求/标签/注释事件，请勾选此复选框。

    iv. `Mange System Hook` - 如果您希望插件在您的 GitLab 项目上设置系统钩子以检测项目是否被移除，请勾选此复选框。请记住，只有提供的访问令牌具有 `Admin` 权限，插件才能在您的服务器上设置系统钩子。

    v. `Secret Token` - 需要该密钥令牌来验证从 GitLab 服务器收到的 WebGL 负载。使用“高级”选项中的生成机密令牌功能或使用您自己的令牌。如果您是老插件用户，以前没有设置过令牌，并且希望将令牌应用到现有作业的钩子中，您可以添加令牌并重新扫描作业。届时将应用带有新令牌的现有钩子。

    vi. `Root URL for hooks` - 默认情况下，此插件创建的钩子的根 URL 是您的 Jenkins 实例 URL。您可以通过添加自定义根 URL 来修改根 URL。如果您希望 Jenkins URL 作为您的自定义钩子 URL，请留空。Web 钩子会自动添加路径 `/gitlab-webhook/post`，系统钩子会自动添加路径 `/gitlab-systemhook/post`。

6. 添加个人访问令牌凭据：

   这是一个手动设置过程。要自动生成个人访问令牌，请参阅[下一节](#creating-personal-access-token-within-jenkins)。

    i. 用户需要添加 `GitLab Personal Access Token` 或任何 `String Credential` 类型的凭据条目，以便在 Jenkins 内部安全地持久化令牌。

    ii. 在您的 GitLab 服务器上生成 `个人访问令牌 (Personal Access Token)`：

    ```
    a. 从右上角的配置文件下拉菜单中选择
    b. 选择 `设置` (Settings)
    c. 从左侧列中选择 `访问令牌` (Access Token)
    d. 输入名称 | 将范围设置为 `api` (如果是管理员，还需提供 `sudo` 权限，这是系统钩子和合并请求评论触发所必需的)
    e. 选择 `创建个人访问令牌` (Create Personal Access Token)
    f. 复制生成的令牌
    ```

    iii. 返回 Jenkins | 在凭据 (Credentials) 字段中选择 `Add` | 选择 `Jenkins`。

    iv. 将 `Kind` 设置为 GitLab Personal Access Token。

    v. 输入 `Token`。

    vi. 在 `ID` 中输入一个唯一的 ID。

    vii. 输入一个通俗易懂的描述。

      ![gitlab-credentials](/docs/img/gitlab-credentials.png)

    viii. 选择 `Add`。

7. 测试连接：

    i. 在 `Credentials` 下拉菜单中选择所需的令牌。

    ii. 选择 `Test Connection`。

    iii. 它应该返回类似 `Credentials verified for user {username}` 的信息。

8. 单击“应用”（底部）。

9. 至此，GitLab 服务器已在 Jenkins 上设置完毕。

### 在 Jenkins 内部创建个人访问令牌

或者，用户可以在 Jenkins 自身内部生成 GitLab 个人访问令牌，并自动将该令牌凭据添加到 Jenkins 服务器。

1. 在 `GitLab` 部分底部选择 `高级` (Advanced)。

2. 选择 `管理额外的 GitLab 操作` (Manage Additional GitLab Actions)。

3. 选择 `将登录名和密码转换为令牌` (Convert login and password to token)。

4. 设置 `GitLab Server URL`。

5. 生成令牌有两个选项：

    i. `From credentials` - 选择已存在的“用户名密码”凭据或添加新的。

    ii. `From login and password` - 如果这是一次性的，您可以直接在文本框中输入您的凭据，用户名/密码凭据不会持久化。

6. 设置用户名/密码凭据后，选择 `Create token credentials`。

7. 令牌创建器将在您的 GitLab 服务器中为给定用户创建具有所需范围的个人访问令牌，并在 Jenkins 服务器内部为该令牌创建凭据。您可以返回 GitLab 服务器配置以选择生成的新凭据（先选择 "-none-"，然后新凭据就会出现）。出于安全原因，此令牌不会以纯文本形式显示，而是返回一个 `id`。这是一个 128 位长的 UUID-4 字符串（36 个字符）。

    ![gitlab-token-creator](/docs/img/gitlab-token-creator.png)

### 在 GitLab 服务器上手动创建钩子

在 GitLab 服务器上设置 Web 钩子和系统钩子时，请使用以下端点。`Jenkins Url` 必须是完全限定域名 (FQDN)，因此不能是 `localhost`。

#### WebHook

```
<jenkins_url>/gitlab-webhook/post
```

带有 `push`、`tag`、`merge request` 和 `note` 事件。


#### SystemHook

```
<jenkins_url>/gitlab-systemhook/post
```
带有 `repository update` 事件。

### 配置即代码 (Configuration as Code)

有一种更简单的方法可以在 Jenkins 服务器上设置 GitLab 服务器配置。无需在 UI 中繁琐操作。`Jenkins Configuration as Code (JCasC)` 或简称为 `Configuration as Code` 插件允许您通过 `yaml` 文件配置 Jenkins。如果您是第一次使用，可以在[此处](https://github.com/jenkinsci/configuration-as-code-plugin)了解有关 JCasC 的更多信息。

#### 前提条件：

在您的 Jenkins 实例上安装 `Configuration as Code` 插件。

参考[在 Jenkins 中安装新插件](https://jenkins.io/doc/book/managing/plugins/#installing-a-plugin)。

#### 添加配置 YAML：

加载 JCasC yaml 文件以配置 Jenkins 有多种方式：

* JCasC 默认在 `$JENKINS_ROOT` 中搜索名为 `jenkins.yaml` 的文件。

* JCasC 查找环境变量 `CASC_JENKINS_CONFIG`，该变量包含配置 `yaml` 文件的路径。

    * 指向包含一组配置文件文件夹的路径，例如 `/var/jenkins_home/casc_configs`。

    * 指向单个文件的全路径，例如 `/var/jenkins_home/casc_configs/jenkins.yaml`。

    * 指向网络上文件的 URL，例如 `https://<your-domain>/jenkins.yaml`。

* 您也可以在 UI 中设置配置 yaml 路径。转到 `<your-jenkins-domain>/configuration-as-code`。输入 `jenkins.yaml` 的路径或 URL，然后选择 `Apply New Configuration`。

要在 Jenkins 中配置您的 GitLab 服务器，请将以下内容添加到 `jenkins.yaml` 中：

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
          - gitlabPersonalAccessToken:
              scope: SYSTEM
              id: "i<3GitLab"
              token: "glpat-XfsqZvVtAx5YCph5bq3r" # gitlab 个人访问令牌
          - gitlabGroupAccessToken:
              scope: SYSTEM
              id: "i<3GitLab"
              token: "glgat-XfsqZvVtAx5YCph5bq3r" # gitlab 组访问令牌

unclassified:
  gitLabServers:
    servers:
      - credentialsId: "i<3GitLab" # 与为 gitlab 个人访问令牌凭证指定的 id 相同
        manageWebHooks: true
        manageSystemHooks: true # 访问令牌应具有管理员权限以设置系统钩子
        name: "gitlab-3214"
        serverUrl: "https://gitlab.com"
        hooksRootUrl: ""
        secretToken: ""
```

为了获得更好的安全性，请参阅 JCasC 文档中的 [处理机密部分](https://github.com/jenkinsci/configuration-as-code-plugin#handling-secrets)。

## 任务设置 (Jobs Setup)

GitLab 分支源插件允许您创建 2 种作业：

* `多分支流水线作业` (Multibranch Pipeline Jobs) - 针对单个项目。
* `文件夹组织` (Folder Organization) - 针对所有者（用户/组/子组）内部的多个项目。

### 多分支流水线作业

多分支流水线作业类型使您能够为同一项目的不同分支实现不同的 Jenkinsfile。在多分支流水线作业中，Jenkins 会自动发现、管理并执行包含源代码管理中 `Jenkinsfile` 的分支/合并请求/标签的流水线。这消除了手动创建和管理流水线的需要。

要创建“多分支流水线作业”：

1. 在 Jenkins 主页上选择 `新建任务` (New Item)。

2. 输入作业名称，选择 `多分支流水线` (Multibranch Pipeline) | 点击 `确定`。

3. 在 `分支源` (Branch Sources) 部分，选择 `添加源` (Add source) | 选择 `GitLab Project`。

4. 现在您需要配置您的作业。

    ![branch-source](/docs/img/branch-source.png)

    i. 选择初始服务器设置中配置的 `Server`。

    ii. [可选] 如果将由插件构建任何私有项目，请添加 `Checkout Credentials` (SSHPrivateKey 或用户名/密码)。

    iii. 添加您要构建的项目所在所有者的路径。如果是用户，输入 `username`。如果是组，输入 `组名`。如果是子组，输入 `带有命名空间的子组路径`。

    iv. 根据提供的所有者，会在路径中发现所有项目并将其添加到 `Projects` 列表框中。您现在可以选择要构建的项目。

    v. `行为` (Behaviours)（又称 SCM Traits）为您的构建允许不同的配置选项。在 SCM Trait APIs 部分中有更多相关信息。

5. 接下来您可以保存作业。

更多信息请参见[此处](https://jenkins.io/doc/book/pipeline/multibranch/)。

保存后，如果在服务器配置中指定了 `GitLab 访问令牌`，则会在您的 GitLab 服务器中创建一个新的 Web 钩子。然后根据您选择的行为选项启动分支索引。随着索引的进行，将为在根目录中包含 `Jenkinsfile` 的每个分支启动并排队新的作业。

构建结果将作为每个构建分支的 HEAD 提交的流水线状态 (Pipeline Status) 通知给 GitLab 服务器。forked MR 的构建无法通知给 GitLab 服务器，因为出于安全考虑，GitLab 不为来自 fork 的合并请求提供流水线状态。参见[此处](https://docs.gitlab.com/ee/ci/merge_request_pipelines/#important-notes-about-merge-requests-from-forked-projects)。

我们对此有一个解决方法。如果 MR 作者是可信的所有者，即具有 `Developer`/`Maintainer`/`Owner` 访问级别，Jenkins 将构建来自 forked 项目的 MR。在 SCM Trait APIs 部分中有更多相关信息。

由于现在 GitLab 服务器已在您的 Jenkins CI 上设置了 Web 钩子，任何推送事件、合并请求事件或标签事件都会触发 Jenkins 中的相关构建。

### 文件夹组织 (Folder Organization)

文件夹组织使 Jenkins 能够监控整个 GitLab `用户`/`组`/`子组`，并为包含含有 `Jenkinsfile` 的分支/合并请求/标签的项目自动创建新的多分支流水线。在我们的插件中，这种作业类型被称为 `GitLab Group`。

要创建“GitLab 组作业”：

1. 在 Jenkins 主页上选择 `新建任务`。

2. 输入作业名称。

3. 选择 `Organization Folder` 作为作业类型，然后按 `确定` 按钮。

4. 在“配置”中的 `Projects` 下，选择 `GitLab Group` 作为 `Repository Sources`。

5. 现在您需要配置您的作业。

    i. 选择初始服务器设置中配置的 `Server`。

    ii. [可选] 仅当需要构建任何私有项目时，才添加 `Checkout Credentials` (SSHPrivateKey 或用户名/密码)。

    iii. 添加您要构建其项目的持有者的路径。如果是用户，输入 `username`。如果是组，输入 `组名`。如果是子组，输入 `带有命名空间的子组路径`。

    v. `行为` (Behaviours)（又称 SCM Traits）允许您的构建具有不同的配置选项。在 SCM Trait APIs 部分中有更多相关信息。

此组作业类型中的索引只需发现一个带有 `Jenkinsfile` 的分支，因此它只显示部分索引日志。您需要访问具体项目以查看其完整的索引信息。

## SCM Trait APIs

以下行为适用于 `多分支流水线作业` 和 `文件夹组织`（除非另有说明）。

### 默认特性 (Default Traits):

* `发现分支` (Discover branches) - 发现分支。

	* `仅发现不是 MR 的分支` - 如果您正在发现源合并请求，那么将同样的更改既作为合并请求又作为分支来发现可能没有意义。
	* `仅发现是 MR 的分支` - 此选项的存在是为了在从旧版本的插件升级时保留传统行为。注意：如果您对该选项有实际用例，请针对此文本提交合并请求。
	* `所有分支` - 忽略分支是否也被提交为合并请求，而是发现源项目上的所有分支。

* `发现源项目的合并请求` (Discover merge requests from origin) - 发现从源分支发起的合并请求。

	* `将合并请求与当前目标版本合并后的结果` - 发现每个合并请求一次，发现的版本对应于与目标分支当前版本合并后的结果。
	* `当前合并请求版本` - 发现每个合并请求一次，发现的版本对应于合并请求头部版本，且不进行合并。
	* `合并请求当前版本以及与当前目标版本合并后的结果` - 发现每个合并请求两次。第一个发现的版本对应于每次扫描中与目标分支当前版本合并后的结果。第二个并行发现的版本对应于合并请求头部版本，且不进行合并。

* `发现来自 fork 的合并请求` (Discover merge requests from forks) - 发现由 forked 项目分支发起的合并请求。

	* 策略 (Strategy):

		* `将合并请求与当前目标版本合并后的结果` - 发现每个合并请求一次，发现的版本对应于与目标分支当前版本合并后的结果。
		* `当前合并请求版本` - 发现每个合并请求一次，发现的版本对应于合并请求头部版本，且不进行合并。
		* `合并请求当前版本以及与当前目标版本合并后的结果` - 发现每个合并请求两次。第一个发现的版本对应于每次扫描中与目标分支当前版本合并后的结果。第二个并行发现的版本对应于合并请求头部版本，且不进行合并。

	* 信任 (Trust):

		* `成员` (Members) - 发现来自作者是源项目成员的 forked 项目的 MR。
		* `受信任成员` (Trusted Members) - [推荐] 发现来自作者在源项目中具有 Developer/Maintainer/Owner 访问级别的 forked 项目的 MR。
		* `所有人` (Everyone) - 发现任何人提交的来自 forked 项目的 MR。出于安全原因，您永远不应使用此选项。它可能被用来泄露您的流水线凭据环境变量。
		* `无人` (Nobody) - 完全不发现来自 forked 项目的 MR。相当于完全删除该特性。

	如果选择了 `成员` 或 `受信任成员`，则插件将为来自非信任/不受信任成员的 MR 构建目标分支。

### 额外特性 (Additional Traits):

这些特性可以通过在 `行为` (Behaviours) 部分中选择 `Add` 来选中。

* `标签发现` (Tag discovery) - 发现项目中的标签。要自动构建标签，请安装 `basic-branch-build-plugin`。

* `发现组/子组项目` (Discover group/subgroup projects) - 发现组/子组内部的子组项目。仅适用于持有者为 `组/子组` 而不是 `用户` 的 `GitLab Group` 作业类型。

* `发现共享项目` (Discover shared projects) - 发现从另一个组共享到配置持有者组的项目。在插件的 684 版本之前，这一直是默认行为，但出于潜在的安全考虑，现在已成为一个单独的特性，默认不添加。

* `在 GitLab 上将构建状态记录为评论` (Log build status as comment on GitLab) - 启用在 GitLab 上将构建状态记录为评论的功能。一旦构建完成，就会在提交或合并请求上记录一条评论。您可以决定是否记录成功的构建。您还可以使用 sudo 用户来评论构建状态，例如 `jenkinsadmin` 或类似的用户。

* `基于合并请求评论触发构建` (Trigger build on merge request comment) - 允许通过具有您期望的评论内容（默认：`jenkins rebuild`）的评论来触发合并请求的重新构建。作业只能由该项目的受信任成员触发，即具有 Developer/Maintainer/Owner 访问级别（也包括从祖先组继承的权限）的用户。默认情况下，只有项目的受信任成员才能触发 MR 构建。
您可能想要禁用此选项，因为受信任成员不包括从共享组继承的成员（截至 GitLab 13.0.0，目前没有办法从 GitLabApi 中获取此信息）。如果禁用，任何有权访问您的项目的用户都可以触发 MR 评论。

* `禁用 GitLab 项目头像` (Disable GitLab project avatar) - 禁用 GitLab 项目的头像。当 API 没有令牌认证或项目是私有的时，无法获取头像。因此，您可以将此选项作为一种变通方法。我们将在以后的版本中修复此问题。

* `项目命名策略` (Project Naming Strategy) - 选择是以 `项目名称`、`完整项目路径（带命名空间）`、`上下文项目路径（部分命名空间）` 还是 `简单项目路径（无命名空间）` 作为每个项目的作业名称。由于历史原因，我们将 `项目路径（带命名空间）` 作为默认命名方案。注意，如果已经创建了作业并更改了命名策略，将导致项目和构建日志被销毁。

* `按名称过滤 (Regex)` - 基于指定的正则表达式过滤您要在项目中发现的项目类型。例如，要仅发现 `master` 分支、`develop` 分支和所有合并请求，请添加 `(master|develop|MR-.*)`。

* `按名称过滤 (Wildcards)` - 基于指定的通配符过滤您要在项目中发现的项目类型。例如，要仅发现 `master` 分支、`develop` 分支和所有合并请求，请添加 `development master MR-*`。

* `跳过流水线状态通知` (Skip pipeline status notifications) - 禁用向 GitLab 服务器通知流水线状态的功能。

* `覆盖挂钩管理模式` (Override hook management modes) - 覆盖 Web 钩子和系统钩子的默认钩子管理模式。目前不支持 Web 钩子的 `ITEM` 凭证。

* `通过 SSH 检出` (Checkout over SSH) - [不推荐] 使用此模式通过 SSH 检出。请改用 `Checkout Credentials`。

* `Webhook 监听条件` (Webhook Listener Conditions) - 基于 Web 钩子内容设置触发构建的条件。

## 环境变量

默认情况下，多分支作业具有以下环境变量（由 Branch API Plugin 提供）：

分支 (Branch) - `BRANCH_NAME`

合并请求 (Merge Request) - `BRANCH_NAME`, `CHANGE_ID`, `CHANGE_TARGET`, `CHANGE_BRANCH`, `CHANGE_FORK`, `CHANGE_URL`, `CHANGE_AUTHOR`, `CHANGE_TITLE`. `CHANGE_AUTHOR_DISPLAY_NAME`

标签 (Tag) - `BRANCH_NAME`, `TAG_NAME`, `TAG_TIMESTAMP`, `TAG_DATE`, `TAG_UNIXTIME`

此插件向构建（仅限 `WorkflowRun` 类型，即作为 Web 钩子接收的负载）添加了更多环境变量。参见 https://docs.gitlab.com/ee/user/project/integrations/webhooks.html#events。

有几点需要注意：

> 如果 Web 钩子负载中没有任何字段记录到响应，它将返回一个空字符串。要添加更多变量，请参阅 `package io.jenkins.plugins.gitlabbranchsource.Cause`。
>
> `GITLAB_OBJECT_KIND` - 在访问环境变量之前，应使用此环境变量检查事件类型。可能的值为 `none`、`push`、`tag_push` 和 `merge_request`。
>
> 任何以 `#` 结尾的变量都表示负载列表的索引，从 1 开始。

环境变量可从推送事件 (Push Event)、标签推送事件 (Tag Push Event) 和合并请求事件 (Merge Request Event) 中获取。

### 推送事件 (Push Event):

参见 https://docs.gitlab.com/ee/user/project/integrations/webhooks.html#push-events

<details>
<summary>展开 :snowflake:	</summary><br>

```
GITLAB_OBJECT_KIND
GITLAB_AFTER
GITLAB_BEFORE
GITLAB_REF
GITLAB_CHECKOUT_SHA
GITLAB_USER_ID
GITLAB_USER_NAME
GITLAB_USER_USERNAME
GITLAB_USER_EMAIL
GITLAB_PROJECT_ID
GITLAB_PROJECT_ID_2
GITLAB_PROJECT_NAME
GITLAB_PROJECT_DESCRIPTION
GITLAB_PROJECT_WEB_URL
GITLAB_PROJECT_AVATAR_URL
GITLAB_PROJECT_GIT_SSH_URL
GITLAB_PROJECT_GIT_HTTP_URL
GITLAB_PROJECT_NAMESPACE
GITLAB_PROJECT_VISIBILITY_LEVEL
GITLAB_PROJECT_PATH_NAMESPACE
GITLAB_PROJECT_CI_CONFIG_PATH
GITLAB_PROJECT_DEFAULT_BRANCH
GITLAB_PROJECT_HOMEPAGE
GITLAB_PROJECT_URL
GITLAB_PROJECT_SSH_URL
GITLAB_PROJECT_HTTP_URL
GITLAB_REPO_NAME
GITLAB_REPO_URL
GITLAB_REPO_DESCRIPTION
GITLAB_REPO_HOMEPAGE
GITLAB_REPO_GIT_SSH_URL
GITLAB_REPO_GIT_HTTP_URL
GITLAB_REPO_VISIBILITY_LEVEL
GITLAB_COMMIT_COUNT
GITLAB_REQUEST_URL
GITLAB_REQUEST_STRING
GITLAB_REQUEST_TOKEN
GITLAB_REFS_HEAD
```

</details>

### 标签事件 (Tag Event):

注意：
> Jenkins 默认情况下不会在推送时自动构建标签 ([参见原因](https://issues.jenkins-ci.org/browse/JENKINS-47496?focusedCommentId=332369&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-332369))。您需要安装 Branch Build Strategy Plugin 来解决这个问题。
>
> 参见指南：https://github.com/jenkinsci/basic-branch-build-strategies-plugin/blob/master/docs/user.adoc
>
> 请记住，如果您将 Basic Branch Build 用于标签构建，您还需要为分支和拉取请求（变更请求）添加策略，否则它们将不会被自动构建（见下面的 GIF 图）。

![branch-build-strategy](/docs/img/branch-build-strategy.gif)

参见 https://docs.gitlab.com/ee/user/project/integrations/webhooks.html#tag-events

<details>
<summary>展开 :sunny: </summary><br>


```
GITLAB_OBJECT_KIND
GITLAB_AFTER
GITLAB_BEFORE
GITLAB_REF
GITLAB_CHECKOUT_SHA
GITLAB_USER_ID
GITLAB_USER_NAME
GITLAB_USER_USERNAME
GITLAB_USER_EMAIL
GITLAB_PROJECT_ID
GITLAB_PROJECT_ID_2
GITLAB_PROJECT_NAME
GITLAB_PROJECT_DESCRIPTION
GITLAB_PROJECT_WEB_URL
GITLAB_PROJECT_AVATAR_URL
GITLAB_PROJECT_GIT_SSH_URL
GITLAB_PROJECT_GIT_HTTP_URL
GITLAB_PROJECT_NAMESPACE
GITLAB_PROJECT_VISIBILITY_LEVEL
GITLAB_PROJECT_PATH_NAMESPACE
GITLAB_PROJECT_CI_CONFIG_PATH
GITLAB_PROJECT_DEFAULT_BRANCH
GITLAB_PROJECT_HOMEPAGE
GITLAB_PROJECT_URL
GITLAB_PROJECT_SSH_URL
GITLAB_PROJECT_HTTP_URL
GITLAB_REPO_NAME
GITLAB_REPO_URL
GITLAB_REPO_DESCRIPTION
GITLAB_REPO_HOMEPAGE
GITLAB_REPO_GIT_SSH_URL
GITLAB_REPO_GIT_HTTP_URL
GITLAB_REPO_VISIBILITY_LEVEL
GITLAB_COMMIT_COUNT
GITLAB_REQUEST_URL
GITLAB_REQUEST_STRING
GITLAB_REQUEST_TOKEN
GITLAB_REFS_HEAD
```

</details>

### 合并请求事件 (Merge Request Event):

参见 https://docs.gitlab.com/ee/user/project/integrations/webhooks.html#merge-request-events

<details>
<summary>展开 :zap: </summary><br>

```
GITLAB_OBJECT_KIND
GITLAB_USER_NAME
GITLAB_USER_USERNAME
GITLAB_USER_AVATAR_URL
GITLAB_PROJECT_ID
GITLAB_PROJECT_NAME
GITLAB_PROJECT_DESCRIPTION
GITLAB_PROJECT_WEB_URL
GITLAB_PROJECT_AVATAR_URL
GITLAB_PROJECT_GIT_SSH_URL
GITLAB_PROJECT_GIT_HTTP_URL
GITLAB_PROJECT_NAMESPACE
GITLAB_PROJECT_VISIBILITY_LEVEL
GITLAB_PROJECT_PATH_NAMESPACE
GITLAB_PROJECT_CI_CONFIG_PATH
GITLAB_PROJECT_DEFAULT_BRANCH
GITLAB_PROJECT_HOMEPAGE
GITLAB_PROJECT_URL
GITLAB_PROJECT_SSH_URL
GITLAB_PROJECT_HTTP_URL
GITLAB_REPO_NAME
GITLAB_REPO_URL
GITLAB_REPO_DESCRIPTION
GITLAB_REPO_HOMEPAGE
GITLAB_REPO_GIT_SSH_URL
GITLAB_REPO_GIT_HTTP_URL
GITLAB_REPO_VISIBILITY_LEVEL
GITLAB_OA_ID
GITLAB_OA_TARGET_BRANCH
GITLAB_OA_SOURCE_BRANCH
GITLAB_OA_SOURCE_PROJECT_ID
GITLAB_OA_AUTHOR_ID
GITLAB_OA_ASSIGNEE_ID
GITLAB_OA_TITLE
GITLAB_OA_CREATED_AT
GITLAB_OA_UPDATED_AT
GITLAB_OA_MILESTONE_ID
GITLAB_OA_STATE
GITLAB_OA_MERGE_STATUS
GITLAB_OA_TARGET_PROJECT_ID
GITLAB_OA_IID
GITLAB_OA_DESCRIPTION
GITLAB_OA_SOURCE_NAME
GITLAB_OA_SOURCE_DESCRIPTION
GITLAB_OA_SOURCE_WEB_URL
GITLAB_OA_SOURCE_AVATAR_URL
GITLAB_OA_SOURCE_GIT_SSH_URL
GITLAB_OA_SOURCE_GIT_HTTP_URL
GITLAB_OA_SOURCE_NAMESPACE
GITLAB_OA_SOURCE_VISIBILITY_LEVEL
GITLAB_OA_SOURCE_PATH_WITH_NAMESPACE
GITLAB_OA_SOURCE_DEFAULT_BRANCH
GITLAB_OA_SOURCE_HOMEPAGE
GITLAB_OA_SOURCE_URL
GITLAB_OA_SOURCE_SSH_URL
GITLAB_OA_SOURCE_HTTP_URL
GITLAB_OA_TARGET_NAME
GITLAB_OA_TARGET_DESCRIPTION
GITLAB_OA_TARGET_WEB_URL
GITLAB_OA_TARGET_AVATAR_URL
GITLAB_OA_TARGET_GIT_SSH_URL
GITLAB_OA_TARGET_GIT_HTTP_URL
GITLAB_OA_TARGET_NAMESPACE
GITLAB_OA_TARGET_VISIBILITY_LEVEL
GITLAB_OA_TARGET_PATH_WITH_NAMESPACE
GITLAB_OA_TARGET_DEFAULT_BRANCH
GITLAB_OA_TARGET_HOMEPAGE
GITLAB_OA_TARGE_URL
GITLAB_OA_TARGET_SSH_URL
GITLAB_OA_TARGET_HTTP_URL
GITLAB_OA_LAST_COMMIT_ID
GITLAB_OA_LAST_COMMIT_MESSAGE
GITLAB_OA_LAST_COMMIT_TIMESTAMP
GITLAB_OA_LAST_COMMIT_URL
GITLAB_OA_LAST_COMMIT_AUTHOR_NAME
GITLAB_OA_LAST_COMMIT_AUTHOR_EMAIL
GITLAB_OA_WIP
GITLAB_OA_URL
GITLAB_OA_ACTION
GITLAB_OA_OLDREV
GITLAB_OA_ASSIGNEE_NAME
GITLAB_OA_ASSIGNEE_USERNAME
GITLAB_OA_ASSIGNEE_AVATAR_URL
GITLAB_LABELS_COUNT
GITLAB_LABEL_ID_#
GITLAB_LABEL_TITLE_#
GITLAB_LABEL_COLOR_#
GITLAB_LABEL_PROJECT_ID_#
GITLAB_LABEL_CREATED_AT_#
GITLAB_LABEL_UPDATED_AT_#
GITLAB_LABEL_TEMPLATE_#
GITLAB_LABEL_DESCRIPTION_#
GITLAB_LABEL_TYPE_#
GITLAB_LABEL_GROUP_ID_#
GITLAB_CHANGES_UPDATED_BY_ID_PREV
GITLAB_CHANGES_UPDATED_BY_ID_CURR
GITLAB_CHANGES_UPDATED_AT_PREV
GITLAB_CHANGES_UPDATED_AT_CURR
```

</details>


## Job DSL seed 任务配置

要创建 Job DSL seed 任务，请参阅此[教程](https://github.com/jenkinsci/job-dsl-plugin/wiki/Tutorial---Using-the-Jenkins-Job-DSL)。

下面是针对文件夹组织作业的示例 seed 任务脚本：

```groovy
organizationFolder('GitLab Organization Folder') {
    description("使用 Job DSL 创建的 GitLab org 文件夹")
    displayName('My Project')
    // "Projects"
    organizations {
        gitLabSCMNavigator {
            projectOwner("baymac")
            credentialsId("i<3GitLab")
            serverName("gitlab-3214")
            // 与 GUI 中的 "Behaviours" 对应的 "Traits"；它们是 "declarative-compatible" 的
            traits {
                subGroupProjectDiscoveryTrait() // 发现子组内部的项目
                gitLabBranchDiscovery {
                    strategyId(3) // 发现所有分支
                }
                originMergeRequestDiscoveryTrait {
                    strategyId(1) // 发现合并请求并将其与目标分支合并
                }
                gitLabTagDiscovery() // 发现标签
            }
        }
    }
    // “特性”（GUI 中的“行为”）不是“声明式兼容”的
    // 对于某些特性，我们需要手动配置，直到 JobDSL 处理它为止
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
    // "Scan Organization Folder Triggers" : 1 天
    // 我们需要手动配置这些内容，因为 JobDSL 目前只允许 'periodic(int min)'
    triggers {
        periodicFolderTrigger {
            interval('1d')
        }
    }
 organización}
```

要查看 Job DSL 支持的所有 API，您可以访问以下链接：

```
http://localhost:8080/jenkins/plugin/job-dsl/api-viewer/index.html#path/organizationFolder-organizations-gitLabSCMNavigator-traits
```

## 使用 JCasC 配置创建作业 (JCasC configuration to create job)

您也可以使用 JCasC 直接从 Job DSL seed 任务创建作业。下面是一个 yaml 配置示例：

```groovy
jobs:
  - script: >
      organizationFolder('GitLab Organization Folder') {
        description("使用 JCasC 配置的 GitLab org 文件夹")
        displayName('My Project')
        // "Projects"
        organizations {
          gitLabSCMNavigator {
            projectOwner("baymac")
            credentialsId("i<3GitLab")
            serverName("gitlab-3214")
            // 与 GUI 中的 "Behaviours" 对应的 "Traits"；它们是 "declarative-compatible" 的
            traits {
              subGroupProjectDiscoveryTrait() // 发现子组内部的项目
              gitLabBranchDiscovery {
                strategyId(3) // 发现所有分支
              }
              originMergeRequestDiscoveryTrait {
                strategyId(1) // 发现合并请求并将其与目标分支合并
              }
              gitLabTagDiscovery() // 发现标签
            }
          }
        }
        // “特性”（GUI 中的“行为”）不是“声明式兼容”的
        // 对于某些特性，我们需要手动配置，直到 JobDSL 处理它为止
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
        // "Scan Organization Folder Triggers" : 1 天
        // 我们需要手动配置这些内容，因为 JobDSL 目前只允许 'periodic(int min)'
        configure { node ->
          node / triggers / 'com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger' {
            spec('H H * * *')
            interval(86400000)
          }
        }
      }
```

您也可以使用文件或 URL 来加载脚本，参见[此处](https://github.com/jenkinsci/job-dsl-plugin/wiki/JCasC)。

## 已知问题

* 系统钩子 (System Hook) 功能仍处于测试阶段。该插件仅在创建新项目时进行检测。它无法检测到项目何时被销毁或更新。要让更改反映出来，需要手动执行分支索引，或者等待分支索引的自动触发（在作业中配置）。

## 如何与我们联系？

* 本项目使用 [Jenkins JIRA](https://issues.jenkins-ci.org/) 来跟踪问题。您可以在 [`gitlab-branch-source-plugin`](https://issues.jenkins-ci.org/issues/?jql=project+%3D+JENKINS+AND+component+%3D+gitlab-branch-source-plugin) 组件下提交问题。

* 在[开发者邮件列表](https://groups.google.com/forum/#!forum/jenkinsci-dev)中发送您的邮件。

* 加入我们的 [Gitter 频道](https://gitter.im/jenkinsci/gitlab-branch-source-plugin)。

## 致谢

本插件由 Google Summer of Code (GSoC) 团队为 [GitLab 的多分支流水线支持](https://jenkins.io/projects/gsoc/2019/gitlab-support-for-multibranch-pipeline/)创建。

当前维护者：

* [Michael Fitoussi](https://github.com/mifitous)
* [Joseph](https://github.com/jetersen)
