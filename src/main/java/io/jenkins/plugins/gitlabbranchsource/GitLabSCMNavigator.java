package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatar;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabGroup;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabLink;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabOwner;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabUser;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import jenkins.model.Jenkins;
import jenkins.plugins.git.traits.GitBrowserSCMSourceTrait;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMNavigatorEvent;
import jenkins.scm.api.SCMNavigatorOwner;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.trait.SCMNavigatorRequest.Witness;
import jenkins.scm.api.trait.SCMNavigatorTrait;
import jenkins.scm.api.trait.SCMNavigatorTraitDescriptor;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMTrait;
import jenkins.scm.api.trait.SCMTraitDescriptor;
import jenkins.scm.impl.form.NamedArrayList;
import jenkins.scm.impl.trait.Discovery;
import jenkins.scm.impl.trait.Selection;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.GroupProjectsFilter;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.jenkins.ui.icon.IconSpec;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.apiBuilder;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper.getServerUrlFromName;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabIcons.ICON_GITLAB;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabIcons.iconFilePathPattern;

public class GitLabSCMNavigator extends SCMNavigator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabSCMNavigator.class);
    /**
     * The owner of the projects to navigate.
     */
    private final String projectOwner;
    /**
     * The GitLab server name configured in Jenkins.
     */
    private String serverName;
    /**
     * The default credentials to use for checking out).
     */
    private String credentialsId;

    /**
     * The behavioural traits to apply.
     */
    private List<SCMTrait<? extends SCMTrait<?>>> traits;

    /**
     * The path with namespace of Navigator projects.
     */
    private HashSet<String> navigatorProjects = new HashSet<>();

    /**
     * To store if project owner is group
     */
    private boolean isGroup;

    /**
     * To store if navigator should include subgroup projects
     */
    private boolean wantSubGroupProjects;
    private transient GitLabOwner gitlabOwner; // TODO check if a better data structure can be used

    @DataBoundConstructor
    public GitLabSCMNavigator(String projectOwner) {
        this.projectOwner = projectOwner;
        this.traits = new ArrayList<>();
    }

    public static String getProjectOwnerFromNamespace(String projectPathWithNamespace) {
        int namespaceLength = projectPathWithNamespace.lastIndexOf("/");
        return projectPathWithNamespace.substring(0, namespaceLength);
    }

    public HashSet<String> getNavigatorProjects() {
        return navigatorProjects;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public boolean isWantSubGroupProjects() {
        return wantSubGroupProjects;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getServerName() {
        return serverName;
    }

    @DataBoundSetter
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    /**
     * Gets the behavioural traits that are applied to this navigator and any {@link
     * GitLabSCMSource} instances it discovers.
     *
     * @return the behavioural traits.
     */
    @NonNull
    public List<SCMTrait<? extends SCMTrait<?>>> getTraits() {
        return Collections.unmodifiableList(traits);
    }

    /**
     * Sets the behavioural traits that are applied to this navigator and any {@link
     * GitLabSCMSource} instances it discovers. The new traits will take affect on the next
     * navigation through any of the {@link #visitSources(SCMSourceObserver)} overloads or {@link
     * #visitSource(String, SCMSourceObserver)}.
     *
     * @param traits the new behavioural traits.
     */
    @DataBoundSetter
    public void setTraits(@CheckForNull SCMTrait[] traits) {
        this.traits = new ArrayList<>();
        if (traits != null) {
            for (SCMTrait trait : traits) {
                this.traits.add(trait);
            }
        }
    }

    /**
     * Sets the behavioural traits that are applied to this navigator and any {@link
     * GitLabSCMSource} instances it discovers. The new traits will take affect on the next
     * navigation through any of the {@link #visitSources(SCMSourceObserver)} overloads or {@link
     * #visitSource(String, SCMSourceObserver)}.
     *
     * @param traits the new behavioural traits.
     */
    @Override
    public void setTraits(@CheckForNull List<SCMTrait<? extends SCMTrait<?>>> traits) {
        this.traits = traits != null ? new ArrayList<>(traits) : new ArrayList<>();

    }

    @NonNull
    @Override
    protected String id() {
        return getServerUrlFromName(serverName) + "::" + projectOwner;
    }

    @Override
    public void visitSources(@NonNull final SCMSourceObserver observer)
        throws IOException, InterruptedException {
        LOGGER.info("visiting sources..");
        try (GitLabSCMNavigatorRequest request = new GitLabSCMNavigatorContext()
            .withTraits(traits)
            .newRequest(this, observer)) {
            GitLabApi gitLabApi = apiBuilder(serverName);
            if (gitlabOwner == null) {
                gitlabOwner = GitLabOwner.fetchOwner(gitLabApi, projectOwner);
            }
            List<Project> projects;
            if (gitlabOwner instanceof GitLabUser) {
                // Even returns the group projects owned by the user
                projects = gitLabApi.getProjectApi()
                    .getUserProjects(projectOwner, new ProjectFilter().withOwned(true));
            } else {
                isGroup = true;
                GroupProjectsFilter groupProjectsFilter = new GroupProjectsFilter();
                wantSubGroupProjects = request.wantSubgroupProjects();
                groupProjectsFilter.withIncludeSubGroups(wantSubGroupProjects);
                // If projectOwner is a subgroup, it will only return projects in the subgroup
                projects = gitLabApi.getGroupApi().getProjects(projectOwner, groupProjectsFilter);
            }
            int count = 0;
            observer.getListener().getLogger().format("%nChecking projects...%n");
            PersonalAccessToken webHookCredentials = getWebHookCredentials(observer.getContext());
            GitLabApi webhookGitLabApi = null;
            if (webHookCredentials != null) {
                webhookGitLabApi = new GitLabApi(getServerUrlFromName(serverName),
                    webHookCredentials.getToken().getPlainText());
            }
            String webHookUrl = GitLabHookCreator.getHookUrl(true);
            for (Project p : projects) {
                count++;
                String projectPathWithNamespace = p.getPathWithNamespace();
                getNavigatorProjects().add(projectPathWithNamespace);
                if (StringUtils.isEmpty(p.getDefaultBranch())) {
                    observer.getListener().getLogger()
                        .format("%nIgnoring project with empty repository %s%n",
                            HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                    continue;
                }
                observer.getListener().getLogger().format("%nChecking project %s%n",
                    HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                try {
                    if (webhookGitLabApi != null) {
                        observer.getListener().getLogger().format("Web hook %s%n", GitLabHookCreator
                            .createWebHookWhenMissing(webhookGitLabApi, projectPathWithNamespace,
                                webHookUrl));
                    }
                } catch (GitLabApiException e) {
                    observer.getListener().getLogger()
                        .format("Cannot set web hook: %s%n", e.getReason());
                }
                String projectOwner = getProjectOwnerFromNamespace(projectPathWithNamespace);
                if (request.process(projectPathWithNamespace,
                    projectPath -> new GitLabSCMSourceBuilder(
                        getId() + "::" + projectPath,
                        serverName,
                        credentialsId,
                        projectOwner,
                        projectPath
                    ).withTraits(traits).build(),
                    null,
                    (Witness) (projectPath, isMatch) -> {
                        if (isMatch) {
                            observer.getListener().getLogger()
                                .format("Proposing %s%n", projectPath);
                        } else {
                            observer.getListener().getLogger().format("Ignoring %s%n", projectPath);
                        }
                    })) {
                    observer.getListener().getLogger()
                        .format("%n%d projects were processed (query complete)%n",
                            count);
                    return;
                }
            }
            observer.getListener().getLogger().format("%n%d projects were processed%n", count);
        } catch (GitLabApiException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private PersonalAccessToken getWebHookCredentials(SCMSourceOwner owner) {
        PersonalAccessToken credentials = null;
        GitLabServer server = GitLabServers.get().findServer(getServerName());
        if (server == null) {
            return null;
        }
        GitLabSCMNavigatorContext navigatorContext = new GitLabSCMNavigatorContext()
            .withTraits(traits);
        GitLabSCMSourceContext ctx = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
            .withTraits(navigatorContext.traits());
        GitLabHookRegistration webhookMode = ctx.webhookRegistration();
        switch (webhookMode) {
            case DISABLE:
                break;
            case SYSTEM:
                if (!server.isManageWebHooks()) {
                    break;
                }
                credentials = server.getCredentials();
                if (credentials == null) {
                    LOGGER.info("No System credentials added, cannot create web hook");
                }
                break;
            case ITEM:
                credentials = credentials(owner);
                if (credentials == null) {
                    LOGGER.info("No Item credentials added, cannot create web hook");
                }
                break;
            default:
                return null;
        }
        return credentials;
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMNavigatorOwner owner,
        SCMNavigatorEvent event,
        @NonNull TaskListener listener) throws IOException, InterruptedException {
        LOGGER.info("retrieving actions..");
        try {
            GitLabApi gitLabApi = apiBuilder(serverName);
            if (gitlabOwner == null) {
                gitlabOwner = GitLabOwner.fetchOwner(gitLabApi, projectOwner);
            }
            String fullName = gitlabOwner.getFullName();
            String webUrl = gitlabOwner.getWebUrl();
            String avatarUrl = gitlabOwner.getAvatarUrl();
            String description = null;
            if (gitlabOwner instanceof GitLabGroup) {
                description = ((GitLabGroup) gitlabOwner).getDescription();
            }
            List<Action> result = new ArrayList<>();
            result.add(new ObjectMetadataAction(
                Util.fixEmpty(fullName),
                description,
                webUrl)
            );
            if (StringUtils.isNotBlank(avatarUrl)) {
                result.add(new GitLabAvatar(avatarUrl));
            }
            result.add(GitLabLink.toGroup(webUrl));
            if (StringUtils.isBlank(webUrl)) {
                listener.getLogger().println("Web URL unspecified");
            } else {
                listener.getLogger().printf("%s URL: %s%n", gitlabOwner.getWord(),
                    HyperlinkNote
                        .encodeTo(webUrl, StringUtils.defaultIfBlank(fullName, webUrl)));
            }
            return result;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new IOException("Server Not found");
        }
    }

    @Override
    public void afterSave(@NonNull SCMNavigatorOwner owner) {
        GitLabSCMNavigatorContext navigatorContext = new GitLabSCMNavigatorContext()
            .withTraits(traits);
        GitLabSCMSourceContext ctx = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
            .withTraits(navigatorContext.traits());
        GitLabHookRegistration systemhookMode = ctx.systemhookRegistration();
        LOGGER.info("Mode of system hook: " + systemhookMode.toString());
        GitLabHookCreator.register(owner, this, systemhookMode);
    }

    public PersonalAccessToken credentials(SCMSourceOwner owner) {
        return CredentialsMatchers.firstOrNull(
            lookupCredentials(
                PersonalAccessToken.class,
                owner,
                Jenkins.getAuthentication(),
                fromUri(getServerUrlFromName(serverName)).build()
            ), credentials -> credentials instanceof PersonalAccessToken
        );
    }

    @Extension
    public static class DescriptorImpl extends SCMNavigatorDescriptor implements IconSpec {

        @Inject
        private GitLabSCMSource.DescriptorImpl delegate;

        public static FormValidation doCheckProjectOwner(@QueryParameter String projectOwner,
            @QueryParameter String serverName) {
            if (projectOwner.equals("")) {
                return FormValidation.ok();
            }
            GitLabApi gitLabApi = null;
            try {
                gitLabApi = apiBuilder(serverName);
                GitLabOwner gitLabOwner = GitLabOwner.fetchOwner(gitLabApi, projectOwner);
                return FormValidation.ok(projectOwner + " is a valid " + gitLabOwner.getWord());
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                return FormValidation.error(e, e.getMessage());
            }
            return FormValidation.ok("");
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "GitLab Group";
        }

        @Override
        public String getPronoun() {
            return "GitLab Group";
        }

        @NonNull
        @Override
        public String getDescription() {
            return "Scans a GitLab Group (or GitLab User) for all projects matching some defined markers.";
        }

        @Override
        public String getIconClassName() {
            return ICON_GITLAB;
        }

        @Override
        public String getIconFilePathPattern() {
            return iconFilePathPattern(getIconClassName());
        }

        @Override
        public SCMNavigator newInstance(String name) {
            LOGGER.info("Instantiating GitLabSCMNavigator..");
            GitLabSCMNavigator navigator =
                new GitLabSCMNavigator("");
            navigator.setTraits(getTraitsDefaults());
            return navigator;
        }

        public ListBoxModel doFillServerNameItems(@AncestorInPath SCMSourceOwner context,
            @QueryParameter String serverName) {
            if (context == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    // must have admin if you want the list without a context
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverName);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)) {
                    // must be able to read the configuration the list
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverName);
                    return result;
                }
            }
            return GitLabServers.get().getServerItems();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context,
            @QueryParameter String serverName,
            @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (context == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    // must have admin if you want the list without a context
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)
                    && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    // must be able to read the configuration or use the item credentials if you want the list
                    result.includeCurrentValue(credentialsId);
                    return result;
                }
            }
            result.includeEmptyValue();
            result.includeMatchingAs(
                context instanceof Queue.Task ?
                    Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                    : ACL.SYSTEM,
                context,
                StandardUsernameCredentials.class,
                fromUri(getServerUrlFromName(serverName)).build(),
                GitClient.CREDENTIALS_MATCHER
            );
            return result;
        }

        @SuppressWarnings("unused") // jelly
        public List<NamedArrayList<? extends SCMTraitDescriptor<?>>> getTraitsDescriptorLists() {
            GitLabSCMSource.DescriptorImpl sourceDescriptor =
                Jenkins.getActiveInstance()
                    .getDescriptorByType(GitLabSCMSource.DescriptorImpl.class);
            List<SCMTraitDescriptor<?>> all = new ArrayList<>();
            all.addAll(SCMNavigatorTrait
                ._for(this, GitLabSCMNavigatorContext.class, GitLabSCMSourceBuilder.class));
            all.addAll(SCMSourceTrait._for(sourceDescriptor, GitLabSCMSourceContext.class, null));
            all.addAll(SCMSourceTrait._for(sourceDescriptor, null, GitLabSCMBuilder.class));
            Set<SCMTraitDescriptor<?>> dedup = new HashSet<>();
            for (Iterator<SCMTraitDescriptor<?>> iterator = all.iterator(); iterator.hasNext(); ) {
                SCMTraitDescriptor<?> d = iterator.next();
                if (dedup.contains(d)
                    || d instanceof GitBrowserSCMSourceTrait.DescriptorImpl) {
                    // remove any we have seen already and ban the browser configuration as it will always be github
                    iterator.remove();
                } else {
                    dedup.add(d);
                }
            }
            List<NamedArrayList<? extends SCMTraitDescriptor<?>>> result = new ArrayList<>();
            NamedArrayList
                .select(all, "Projects", new NamedArrayList.Predicate<SCMTraitDescriptor<?>>() {
                        @Override
                        public boolean test(SCMTraitDescriptor<?> scmTraitDescriptor) {
                            return scmTraitDescriptor instanceof SCMNavigatorTraitDescriptor;
                        }
                    },
                    true, result);
            NamedArrayList.select(all, "Within project", NamedArrayList
                    .anyOf(NamedArrayList.withAnnotation(Discovery.class),
                        NamedArrayList.withAnnotation(Selection.class)),
                true, result);
            NamedArrayList.select(all, "Additional", null, true, result);
            return result;
        }

        @Override
        @NonNull
        @SuppressWarnings("unused") // jelly
        public List<SCMTrait<? extends SCMTrait<?>>> getTraitsDefaults() {
            return new ArrayList<>(delegate.getTraitsDefaults());
        }
    }

}
