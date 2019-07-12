package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.damnhandy.uri.template.UriTemplate;
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
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatar;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabHelper;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabLink;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabOwner;
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
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.trait.SCMNavigatorRequest;
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
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.models.Visibility;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;

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
            if(gitlabOwner == GitLabOwner.USER) {
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
                if(gitlabOwner == GitLabOwner.USER && p.getNamespace().getKind().equals("group")) {
                    // skip the user repos which includes all groups that they are a member of
                    continue;
                }
                count++;
                // TODO needs review
                // If repository is empty it throws an exception
                try {
                    gitLabApi.getRepositoryApi().getTree(p);
                } catch (GitLabApiException e) {
                    observer.getListener().getLogger().format("%nIgnoring project with empty repository %s%n",
                            HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                    continue;
                }
                observer.getListener().getLogger().format("%nChecking project %s%n",
                        HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                if (request.process(p.getPathWithNamespace(), new SCMNavigatorRequest.SourceLambda() {
                    @NonNull
                    @Override
                    public SCMSource create(@NonNull String projectName) throws IOException, InterruptedException {
                        return new GitLabSCMSourceBuilder(
                                getId() + "::" + projectName,
                                serverName,
                                credentialsId,
                                projectOwner,
                                projectName
                        )
                                .withTraits(traits)
                                .build();
                    }
                }, null, new SCMNavigatorRequest.Witness() {
                    @Override
                    public void record(@NonNull String projectName, boolean isMatch) {
                        if (isMatch) {
                            observer.getListener().getLogger().format("Proposing %s%n", projectName);
                        } else {
                            observer.getListener().getLogger().format("Ignoring %s%n", projectName);
                        }
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
            if (this.gitlabOwner == null) {
                gitlabOwner = GitLabOwner.fetchOwner(gitLabApi, projectOwner);
            }
            String fullName = "";
            if (gitlabOwner == GitLabOwner.USER) {
                try {
                    fullName = gitLabApi.getUserApi().getUser(projectOwner).getName();
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    fullName = gitLabApi.getGroupApi().getGroup(projectOwner).getFullName();
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                }
            }
            List<Action> result = new ArrayList<>();
            String objectUrl = UriTemplate.buildFromTemplate(GitLabHelper.getServerUrlFromName(serverName))
                    .path("owner")
                    .build()
                    .set("owner", projectOwner)
                    .expand();
            result.add(new ObjectMetadataAction(
                    Util.fixEmpty(fullName),
                    null,
                    objectUrl)
            );
            String avatarUrl = "";
            if (gitlabOwner == GitLabOwner.USER) {
                try {
                    avatarUrl = gitLabApi.getUserApi().getUser(projectOwner).getAvatarUrl();
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    avatarUrl = gitLabApi.getGroupApi().getGroup(projectOwner).getAvatarUrl();
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                }
            }
            if (StringUtils.isNotBlank(avatarUrl)) {
                result.add(new GitLabAvatar(avatarUrl));
            }
            result.add(new GitLabLink("icon-gitlab", objectUrl));
            if (gitlabOwner == GitLabOwner.USER) {
                String website = null;
                try {
                    // This is a hack since getting a user via username finds user from a list of users
                    // and list of users contain limited info about users which doesn't include website url
                    User user = gitLabApi.getUserApi().getUser(projectOwner);
                    website = gitLabApi.getUserApi().getUser(user.getId()).getWebsiteUrl();
                } catch (GitLabApiException e) {
                    e.printStackTrace();
                }
                if (StringUtils.isBlank(website)) {
                    listener.getLogger().println("User Website URL: unspecified");
                    listener.getLogger().printf("User Website URL: %s%n",
                            HyperlinkNote.encodeTo(website, StringUtils.defaultIfBlank(fullName, website)));
                }
            }
            return result;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new IOException("Server Not found");
        }
    }

    @Override
    public void afterSave(@NonNull SCMNavigatorOwner owner) {
        WebhookRegistration mode = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits(new GitLabSCMNavigatorContext().withTraits(traits).traits())
                .webhookRegistration();
        GitLabWebhookListener.register(owner, this, mode, credentialsId);
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
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "GitLab Group";
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

        @NonNull
        @Override
        public String getDescription() {
            return "Scans a GitLab Group (or GitLab User) for all projects matching some defined markers.";
        }

        @Override
        public String getIconClassName() {
            return "icon-gitlab";
        }

        @Override
        public String getPronoun() {
            return "GitLab Group";
        }

        @Override
        public SCMNavigator newInstance(String name) {
            LOGGER.info("Instantiating GitLabSCMNavigator..");
            GitLabSCMNavigator navigator =
                    new GitLabSCMNavigator("");
            navigator.setTraits(getTraitsDefaults());
            return navigator;
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
