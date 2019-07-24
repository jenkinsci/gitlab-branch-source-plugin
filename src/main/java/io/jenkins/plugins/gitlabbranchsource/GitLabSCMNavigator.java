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
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabLink;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabOwner;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabUser;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import jenkins.model.Jenkins;
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
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.models.Visibility;
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
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabIcons.ICON_GITLAB;
import static io.jenkins.plugins.gitlabbranchsource.helpers.GitLabIcons.iconFilePathPattern;

public class GitLabSCMNavigator extends SCMNavigator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabSCMNavigator.class);
    /**
     * The GitLab server name configured in Jenkins.
     */
    private String serverName;
    /**
     * The owner of the projects to navigate.
     */
    private final String projectOwner;

    /**
     * The default credentials to use for checking out).
     */
    private String credentialsId;

    /**
     * The behavioural traits to apply.
     */
    private List<SCMTrait<? extends SCMTrait<?>>> traits;

    private transient GitLabOwner gitlabOwner; // TODO check if a better data structure can be used

    @DataBoundConstructor
    public GitLabSCMNavigator(String projectOwner) {
        this.projectOwner = projectOwner;
        this.traits = new ArrayList<>();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getServerName() {
        return serverName;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    /**
     * Sets the behavioural traits that are applied to this navigator and any {@link GitLabSCMSource} instances it
     * discovers. The new traits will take affect on the next navigation through any of the
     * {@link #visitSources(SCMSourceObserver)} overloads or {@link #visitSource(String, SCMSourceObserver)}.
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
     * Sets the behavioural traits that are applied to this navigator and any {@link GitLabSCMSource} instances it
     * discovers. The new traits will take affect on the next navigation through any of the
     * {@link #visitSources(SCMSourceObserver)} overloads or {@link #visitSource(String, SCMSourceObserver)}.
     *
     * @param traits the new behavioural traits.
     */
    @Override
    public void setTraits(@CheckForNull List<SCMTrait<? extends SCMTrait<?>>> traits) {
        this.traits = traits != null ? new ArrayList<>(traits) : new ArrayList<SCMTrait<? extends SCMTrait<?>>>();

    }

    @DataBoundSetter
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    /**
     * Gets the behavioural traits that are applied to this navigator and any {@link GitLabSCMSource} instances it
     * discovers.
     *
     * @return the behavioural traits.
     */
    @NonNull
    public List<SCMTrait<? extends SCMTrait<?>>> getTraits() {
        return Collections.unmodifiableList(traits);
    }

    @NonNull
    @Override
    protected String id() {
        return GitLabHelper.getServerUrlFromName(serverName) + "::" + projectOwner;
    }

    @Override
    public void visitSources(@NonNull final SCMSourceObserver observer) throws IOException, InterruptedException {
        LOGGER.info("visiting sources..");
        try (GitLabSCMNavigatorRequest request = new GitLabSCMNavigatorContext()
                .withTraits(traits)
                .newRequest(this, observer)) {
            GitLabApi gitLabApi = GitLabHelper.apiBuilder(serverName);
            gitlabOwner = GitLabOwner.fetchOwner(gitLabApi, projectOwner);
            List<Project> projects;
            if(gitlabOwner instanceof GitLabUser) {
                // Even returns the group projects owned by the user
                if(gitLabApi.getAuthToken().equals("")) {
                    projects = gitLabApi.getProjectApi().getUserProjects(projectOwner, new ProjectFilter().withVisibility(Visibility.PUBLIC));
                } else {
                    projects = gitLabApi.getProjectApi().getOwnedProjects();
                }
            } else {
                // If projectOwner is a subgroup, it will only return projects in the subgroup
                projects = gitLabApi.getGroupApi().getProjects(projectOwner);
            }
            int count = 0;
            observer.getListener().getLogger().format("%nChecking projects...%n");
            for(Project p : projects) {
                if(gitlabOwner instanceof GitLabUser && p.getNamespace().getKind().equals("group")) {
                    // skip the user repos which includes all groups that they are a member of
                    continue;
                }
                // If repository is empty it throws an exception
                count++;
                try {
                    gitLabApi.getRepositoryApi().getTree(p);
                } catch (GitLabApiException e) {
                    observer.getListener().getLogger().format("%nIgnoring project with empty repository %s%n",
                            HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                    continue;
                }
                observer.getListener().getLogger().format("%nChecking project %s%n",
                        HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                if (request.process(p.getPathWithNamespace(),
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
                        observer.getListener().getLogger().format("Proposing %s%n", projectPath);
                    } else {
                        observer.getListener().getLogger().format("Ignoring %s%n", projectPath);
                    }
                })) {
                    observer.getListener().getLogger().format("%n%d projects were processed (query complete)%n",
                            count);
                    return;
                }
            }
            observer.getListener().getLogger().format("%n%d projects were processed%n", count);
        } catch (GitLabApiException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMNavigatorOwner owner, SCMNavigatorEvent event,
                                           @NonNull TaskListener listener) throws IOException, InterruptedException {
        LOGGER.info("retrieving actions..");
        try {
            GitLabApi gitLabApi = GitLabHelper.apiBuilder(serverName);
            if (gitlabOwner == null) {
                gitlabOwner = GitLabOwner.fetchOwner(gitLabApi, projectOwner);
            }
            String fullName = gitlabOwner.getFullName();
            String webUrl = gitlabOwner.getWebUrl();
            String avatarUrl = gitlabOwner.getAvatarUrl();
            List<Action> result = new ArrayList<>();
            result.add(new ObjectMetadataAction(
                    Util.fixEmpty(fullName),
                    null,
                    webUrl)
            );
            if (StringUtils.isNotBlank(avatarUrl)) {
                result.add(new GitLabAvatar(avatarUrl));
            }
            result.add(new GitLabLink("gitlab-logo", webUrl));
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
        GitLabWebhookRegistration mode = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits(new GitLabSCMNavigatorContext().withTraits(traits).traits())
                .webhookRegistration();
        LOGGER.info(mode.toString());
        GitLabWebhookCreator.register(owner, this, mode);
    }

    public PersonalAccessToken credentials(SCMSourceOwner owner) {
        return CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        PersonalAccessToken.class,
                        owner,
                        Jenkins.getAuthentication(),
                        fromUri(GitLabHelper.getServerUrlFromName(serverName)).build()),
                credentials -> credentials instanceof PersonalAccessToken
        );
    }

    @Extension
    public static class DescriptorImpl extends SCMNavigatorDescriptor implements IconSpec {

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

        public static FormValidation doCheckProjectOwner(@QueryParameter String projectOwner, @QueryParameter String serverName) {
            if(projectOwner.equals("")) {
                return FormValidation.ok();
            }
            GitLabApi gitLabApi = null;
            try {
                gitLabApi = GitLabHelper.apiBuilder(serverName);
                GitLabOwner gitLabOwner = GitLabOwner.fetchOwner(gitLabApi, projectOwner);
                return FormValidation.ok(projectOwner + " is a valid " + gitLabOwner.getWord());
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                return FormValidation.error(e, e.getMessage());
            }
            return FormValidation.ok("");
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
                    fromUri(GitLabHelper.getServerUrlFromName(serverName)).build(),
                    GitClient.CREDENTIALS_MATCHER
            );
            return result;
        }

        @SuppressWarnings("unused") // jelly
        public List<NamedArrayList<? extends SCMTraitDescriptor<?>>> getTraitsDescriptorLists() {
            GitLabSCMSource.DescriptorImpl sourceDescriptor =
                    Jenkins.getActiveInstance().getDescriptorByType(GitLabSCMSource.DescriptorImpl.class);
            List<SCMTraitDescriptor<?>> all = new ArrayList<>();
            all.addAll(SCMNavigatorTrait._for(this, GitLabSCMNavigatorContext.class, GitLabSCMSourceBuilder.class));
            all.addAll(SCMSourceTrait._for(sourceDescriptor, GitLabSCMSourceContext.class, null));
            all.addAll(SCMSourceTrait._for(sourceDescriptor, null, GitLabSCMBuilder.class));
            List<NamedArrayList<? extends SCMTraitDescriptor<?>>> result = new ArrayList<>();
            NamedArrayList.select(all, "Projects", new NamedArrayList.Predicate<SCMTraitDescriptor<?>>() {
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

        @Inject
        private GitLabSCMSource.DescriptorImpl delegate;

        @Override
        @NonNull
        @SuppressWarnings("unused") // jelly
        public List<SCMTrait<? extends SCMTrait<?>>> getTraitsDefaults() {
            List<SCMTrait<? extends SCMTrait<?>>> result = new ArrayList<>();
            result.addAll(delegate.getTraitsDefaults());
            return result;
        }
    }

}
