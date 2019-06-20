package io.jenkins.plugins.gitlabbranchsource;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.damnhandy.uri.template.UriTemplate;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.plugins.git.browser.GitLab;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabAvatar;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabLink;
import io.jenkins.plugins.gitlabbranchsource.helpers.GitLabOwner;
import io.jenkins.plugins.gitlabserverconfig.credentials.PersonalAccessToken;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServer;
import io.jenkins.plugins.gitlabserverconfig.servers.GitLabServers;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import jenkins.model.Jenkins;
import jenkins.plugins.git.traits.GitBrowserSCMSourceTrait;
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
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.models.Visibility;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;

public class GitLabSCMNavigator extends SCMNavigator {
    private final String serverUrl;
    private final String projectOwner;
    private String credentialsId;
    private List<SCMTrait<?>> traits = new ArrayList<>();
    private GitLabOwner gitlabOwner; // TODO check if a better data structure can be used

    @DataBoundConstructor
    public GitLabSCMNavigator(String serverUrl, String projectOwner) {
        this.serverUrl = serverUrl;
        this.projectOwner = projectOwner;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setTraits(List<SCMTrait<?>> traits) {
        this.traits = new ArrayList<>(Util.fixNull(traits));
    }

    @NonNull
    @Override
    protected String id() {
        return serverUrl + "::" + projectOwner;
    }

    @Override
    public void visitSources(@NonNull final SCMSourceObserver observer) throws IOException, InterruptedException {
        try (GitLabSCMNavigatorRequest request = new GitLabSCMNavigatorContext()
                .withTraits(traits)
                .newRequest(this, observer)) {
            GitLabApi gitLabApi = apiBuilder(observer.getContext());
            gitlabOwner = fetchOwner(gitLabApi);
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
            observer.getListener().getLogger().format("%n  Checking repositories...%n");
            for(Project p : projects) {
                if(gitlabOwner == GitLabOwner.USER && p.getNamespace().getKind().equals("group")) {
                    // skip the user repos which includes all organizations that they are a member of
                    continue;
                }
                count++;
                // TODO needs review
                // If repository is empty it throws an exception
                try {
                    gitLabApi.getRepositoryApi().getTree(p);
                } catch (GitLabApiException e) {
                    observer.getListener().getLogger().format("%n    Ignoring empty repository %s%n",
                            HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                    continue;
                }
                observer.getListener().getLogger().format("%n    Checking repository %s%n",
                        HyperlinkNote.encodeTo(p.getWebUrl(), p.getName()));
                if (request.process(p.getName(), new SCMNavigatorRequest.SourceLambda() {
                    @NonNull
                    @Override
                    public SCMSource create(@NonNull String projectName) throws IOException, InterruptedException {
                        return new GitLabSCMSourceBuilder(
                                getId() + "::" + projectName,
                                serverUrl,
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
                            observer.getListener().getLogger().format("      Proposing %s%n", projectName);
                        } else {
                            observer.getListener().getLogger().format("      Ignoring %s%n", projectName);
                        }
                    }
                })) {
                    observer.getListener().getLogger().format("%n  %d repositories were processed (query complete)%n",
                            count);
                    return;
                }

            }
            observer.getListener().getLogger().format("%n  %d repositories were processed%n", count);
        } catch (GitLabApiException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    protected List<Action> retrieveActions(@NonNull SCMNavigatorOwner owner, SCMNavigatorEvent event,
                                           @NonNull TaskListener listener) throws IOException, InterruptedException {
        GitLabApi gitLabApi = apiBuilder(owner);
        if(this.gitlabOwner == null) {
            gitlabOwner = fetchOwner(gitLabApi);
        }
        String fullName = "";
        if(gitlabOwner == GitLabOwner.USER) {
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
        String objectUrl = UriTemplate.buildFromTemplate(serverUrl)
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
        if(gitlabOwner == GitLabOwner.USER) {
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
                website = gitLabApi.getUserApi().getUser(projectOwner).getWebsiteUrl();
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
    }

    @Override
    public void afterSave(@NonNull SCMNavigatorOwner owner) {
        WebhookRegistration mode = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits(new GitLabSCMNavigatorContext().withTraits(traits).traits())
                .webhookRegistration();
        GitLabWebhookListener.register(owner, this, mode, credentialsId);
    }

    private GitLabApi apiBuilder(SCMSourceOwner owner) throws AbortException {
        GitLabServer server = GitLabServers.get().findServer(serverUrl);
        if (server == null) {
            throw new AbortException("Unknown server: " + serverUrl);
        }
        PersonalAccessToken credentials = credentials(owner);
        CredentialsProvider.track(owner, credentials);
        if(credentials == null) {
            return new GitLabApi(serverUrl, "");
        }
        return new GitLabApi(serverUrl, credentials.getToken().getPlainText());
    }

    public PersonalAccessToken credentials(SCMSourceOwner owner) {
        return CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        PersonalAccessToken.class,
                        owner,
                        Jenkins.getAuthentication(),
                        fromUri(serverUrl).build()),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    private GitLabOwner fetchOwner(GitLabApi gitLabApi) {
        try {
            Group group = gitLabApi.getGroupApi().getGroup(projectOwner);
            return GitLabOwner.GROUP;
        } catch (GitLabApiException e) {
            try {
                User user = gitLabApi.getUserApi().getUser(projectOwner);
                return GitLabOwner.USER;
            } catch (GitLabApiException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends SCMNavigatorDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "GitLab Group";
        }

        public ListBoxModel doFillServerUrlItems(@AncestorInPath SCMSourceOwner context,
                                                 @QueryParameter String serverUrl) {
            if (context == null) {
                if (!Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER)) {
                    // must have admin if you want the list without a context
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverUrl);
                    return result;
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ)) {
                    // must be able to read the configuration the list
                    ListBoxModel result = new ListBoxModel();
                    result.add(serverUrl);
                    return result;
                }
            }
            return GitLabServers.get().getServerItems();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context,
                                                     @QueryParameter String serverUrl,
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
                    PersonalAccessToken.class,
                    URIRequirementBuilder.fromUri(serverUrl).build(),
                    credentials -> credentials instanceof PersonalAccessToken
            );
            return result;
        }

        @NonNull
        @Override
        public String getDescription() {
            return "Scans a GitLab Group (or user account) for all repositories matching some defined markers.";
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
            List<GitLabServer> servers = GitLabServers.get().getServers();
            GitLabSCMNavigator navigator =
                    new GitLabSCMNavigator(servers.isEmpty() ? null : servers.get(0).getServerUrl(), name);
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
            NamedArrayList.select(all, "Repositories", new NamedArrayList.Predicate<SCMTraitDescriptor<?>>() {
                        @Override
                        public boolean test(SCMTraitDescriptor<?> scmTraitDescriptor) {
                            return scmTraitDescriptor instanceof SCMNavigatorTraitDescriptor;
                        }
                    },
                    true, result);
            NamedArrayList.select(all, "Within repository", NamedArrayList
                            .anyOf(NamedArrayList.withAnnotation(Discovery.class),
                                    NamedArrayList.withAnnotation(Selection.class)),
                    true, result);
            NamedArrayList.select(all, "Additional", null, true, result);
            return result;
        }

        public List<SCMTrait<? extends SCMTrait<?>>> getTraitsDefaults() {
            GitLabSCMSource.DescriptorImpl descriptor =
                    ExtensionList.lookup(Descriptor.class).get(GitLabSCMSource.DescriptorImpl.class);
            if (descriptor == null) {
                throw new AssertionError();
            }
            return new ArrayList<>(descriptor.getTraitsDefaults());
        }
    }

}
