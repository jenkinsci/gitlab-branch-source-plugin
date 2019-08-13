package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceRequest;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitLabSCMSourceRequest extends SCMSourceRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabSCMSourceRequest.class);
    /**
     * {@code true} if branch details need to be fetched.
     */
    private final boolean fetchBranches;
    /**
     * {@code true} if tag details need to be fetched.
     */
    private final boolean fetchTags;
    /**
     * {@code true} if origin merge requests need to be fetched.
     */
    private final boolean fetchOriginMRs;
    /**
     * {@code true} if fork merge requests need to be fetched.
     */
    private final boolean fetchForkMRs;
    /**
     * The {@link ChangeRequestCheckoutStrategy} to create for each origin merge request.
     */
    @NonNull
    private final Set<ChangeRequestCheckoutStrategy> originMRStrategies;
    /**
     * The {@link ChangeRequestCheckoutStrategy} to create for each fork merge request.
     */
    @NonNull
    private final Set<ChangeRequestCheckoutStrategy> forkMRStrategies;
    /**
     * The set of merge request numbers that the request is scoped to or {@code null} if the request is not limited.
     */
    @CheckForNull
    private final Set<Long> requestedMergeRequestNumbers;
    /**
     * The set of origin branch names that the request is scoped to or {@code null} if the request is not limited.
     */
    @CheckForNull
    private final Set<String> requestedOriginBranchNames;
    /**
     * The set of tag names that the request is scoped to or {@code null} if the request is not limited.
     */
    @CheckForNull
    private final Set<String> requestedTagNames;
    /**
     * The merge request details or {@code null} if not {@link #isFetchMRs()}.
     */
    @CheckForNull
    private Iterable<MergeRequest> mergeRequests;
    /**
     * The branch details or {@code null} if not {@link #isFetchBranches()}.
     */
    @CheckForNull
    private Iterable<Branch> branches;
    /**
     * The tag details or {@code null} if not {@link #isFetchTags()}.
     */
    @CheckForNull
    private Iterable<Tag> tags;
    /**
     * The list of project {@link Member} or {@code null} if not provided.
     */
    private HashMap<String, AccessLevel> members = new HashMap<>();
    /**
     * The project.
     */
    @CheckForNull
    private Project gitlabProject;
    /**
     * A connection to the GitLab API or {@code null} if none established yet.
     */
    @CheckForNull
    private GitLabApi gitLabApi;

    /**
     * Constructor.
     *
     * @param source   the source.
     * @param context  the context.
     * @param listener the listener.
     */
    GitLabSCMSourceRequest(SCMSource source, GitLabSCMSourceContext context, TaskListener listener) {
        super(source, context, listener);
        fetchBranches = context.wantBranches();
        fetchTags = context.wantTags();
        fetchOriginMRs = context.wantOriginMRs();
        fetchForkMRs = context.wantForkMRs();
        originMRStrategies = fetchOriginMRs && !context.originMRStrategies().isEmpty()
                ? Collections.unmodifiableSet(EnumSet.copyOf(context.originMRStrategies()))
                : Collections.<ChangeRequestCheckoutStrategy>emptySet();
        forkMRStrategies = fetchForkMRs && !context.forkMRStrategies().isEmpty()
                ? Collections.unmodifiableSet(EnumSet.copyOf(context.forkMRStrategies()))
                : Collections.<ChangeRequestCheckoutStrategy>emptySet();
        Set<SCMHead> includes = context.observer().getIncludes();
        if (includes != null) {
            Set<Long> mergeRequestNumbers = new HashSet<>(includes.size());
            Set<String> branchNames = new HashSet<>(includes.size());
            Set<String> tagNames = new HashSet<>(includes.size());
            for (SCMHead h : includes) {
                if (h instanceof BranchSCMHead) {
                    branchNames.add(h.getName());
                } else if (h instanceof MergeRequestSCMHead) {
                    mergeRequestNumbers.add(Long.parseLong(((MergeRequestSCMHead) h).getId()));
                    if (SCMHeadOrigin.DEFAULT.equals(h.getOrigin())) {
                        branchNames.add(((MergeRequestSCMHead) h).getOriginName());
                    }
                } else if (h instanceof GitLabTagSCMHead) {
                    tagNames.add(h.getName());
                }
            }
            this.requestedMergeRequestNumbers = Collections.unmodifiableSet(mergeRequestNumbers);
            this.requestedOriginBranchNames = Collections.unmodifiableSet(branchNames);
            this.requestedTagNames = Collections.unmodifiableSet(tagNames);
        } else {
            requestedMergeRequestNumbers = null;
            requestedOriginBranchNames = null;
            requestedTagNames = null;
        }
    }

    /**
     * Returns {@code true} if branch details need to be fetched.
     *
     * @return {@code true} if branch details need to be fetched.
     */
    public final boolean isFetchBranches() {
        return fetchBranches;
    }

    /**
     * Returns {@code true} if tag details need to be fetched.
     *
     * @return {@code true} if tag details need to be fetched.
     */
    public final boolean isFetchTags() {
        return fetchTags;
    }

    /**
     * Returns {@code true} if merge request details need to be fetched.
     *
     * @return {@code true} if merge request details need to be fetched.
     */
    public final boolean isFetchMRs() {
        return isFetchOriginMRs() || isFetchForkMRs();
    }

    /**
     * Returns {@code true} if origin merge request details need to be fetched.
     *
     * @return {@code true} if origin merge request details need to be fetched.
     */
    public final boolean isFetchOriginMRs() {
        return fetchOriginMRs;
    }

    /**
     * Returns {@code true} if fork merge request details need to be fetched.
     *
     * @return {@code true} if fork merge request details need to be fetched.
     */
    public final boolean isFetchForkMRs() {
        return fetchForkMRs;
    }

    /**
     * Returns the {@link ChangeRequestCheckoutStrategy} to create for each origin merge request.
     *
     * @return the {@link ChangeRequestCheckoutStrategy} to create for each origin merge request.
     */
    @NonNull
    public final Set<ChangeRequestCheckoutStrategy> getOriginMRStrategies() {
        return originMRStrategies;
    }

    /**
     * Returns the {@link ChangeRequestCheckoutStrategy} to create for each fork merge request.
     *
     * @return the {@link ChangeRequestCheckoutStrategy} to create for each fork merge request.
     */
    @NonNull
    public final Set<ChangeRequestCheckoutStrategy> getForkMRStrategies() {
        return forkMRStrategies;
    }

    /**
     * Returns the {@link ChangeRequestCheckoutStrategy} to create for merge requests of the specified type.
     *
     * @param fork {@code true} to return strategies for the fork merge requests, {@code false} for origin merge requests.
     * @return the {@link ChangeRequestCheckoutStrategy} to create for each merge request.
     */
    @NonNull
    public final Set<ChangeRequestCheckoutStrategy> getMRStrategies(boolean fork) {
        if (fork) {
            return fetchForkMRs ? getForkMRStrategies() : Collections.<ChangeRequestCheckoutStrategy>emptySet();
        }
        return fetchOriginMRs ? getOriginMRStrategies() : Collections.<ChangeRequestCheckoutStrategy>emptySet();
    }

    /**
     * Returns the {@link ChangeRequestCheckoutStrategy} to create for each merge request.
     *
     * @return a map of the {@link ChangeRequestCheckoutStrategy} to create for each merge request keyed by whether the
     * strategy applies to forks or not ({@link Boolean#FALSE} is the key for origin merge requests)
     */
    public final Map<Boolean, Set<ChangeRequestCheckoutStrategy>> getMRStrategies() {
        Map<Boolean, Set<ChangeRequestCheckoutStrategy>> result = new HashMap<>();
        for (Boolean fork : new Boolean[]{Boolean.TRUE, Boolean.FALSE}) {
            result.put(fork, getMRStrategies(fork));
        }
        return result;
    }

    /**
     * Returns requested merge request numbers.
     *
     * @return the requested merge request numbers or {@code null} if the request was not scoped to a subset of merge
     * requests.
     */
    @CheckForNull
    public final Set<Long> getRequestedMergeRequestNumbers() {
        return requestedMergeRequestNumbers;
    }

    /**
     * Gets requested origin branch names.
     *
     * @return the requested origin branch names or {@code null} if the request was not scoped to a subset of branches.
     */
    @CheckForNull
    public final Set<String> getRequestedOriginBranchNames() {
        return requestedOriginBranchNames;
    }

    /**
     * Gets requested tag names.
     *
     * @return the requested tag names or {@code null} if the request was not scoped to a subset of tags.
     */
    @CheckForNull
    public final Set<String> getRequestedTagNames() {
        return requestedTagNames;
    }

    /**
     * Returns the merge request details or an empty list if either the request did not specify to {@link #isFetchMRs()}
     * or if the merge request details have not been provided by {@link #setMergeRequests(Iterable)} yet.
     *
     * @return the details of merge requests, may be limited by {@link #getRequestedMergeRequestNumbers()} or
     * may be empty if not {@link #isFetchMRs()}
     */
    @NonNull
    public Iterable<MergeRequest> getMergeRequests() {
        return Util.fixNull(mergeRequests);
    }

    /**
     * Provides the requests with the merge request details.
     *
     * @param mergeRequests the merge request details.
     */
    public void setMergeRequests(@CheckForNull Iterable<MergeRequest> mergeRequests) {
        this.mergeRequests = mergeRequests;
    }

    /**
     * Returns the branch details or an empty list if either the request did not specify to {@link #isFetchBranches()}
     * or if the branch details have not been provided by {@link #setBranches(Iterable)} yet.
     *
     * @return the branch details (may be empty)
     */
    @NonNull
    public final Iterable<Branch> getBranches() {
        return Util.fixNull(branches);
    }

    /**
     * Provides the requests with the branch details.
     *
     * @param branches the branch details.
     */
    public final void setBranches(@CheckForNull Iterable<Branch> branches) {
        this.branches = branches;
    }

    /**
     * Provides the requests with the tag details.
     *
     * @param tags the tag details.
     */
    public final void setTags(@CheckForNull Iterable<Tag> tags) {
        this.tags = tags;
    }

    /**
     * Returns the tag details or an empty list if either the request did not specify to {@link #isFetchTags()} ()}
     * or if the tag details have not been provided by {@link #setTags(Iterable)} yet.
     *
     * @return the tag details (may be empty)
     */
    @NonNull
    public final Iterable<Tag> getTags() {
        return Util.fixNull(tags);
    }

    /**
     * Returns the Map of project {@link Member} or {@code null} if those details have not been provided yet.
     *
     * @return the Map of project {@link Member} or {@code null} if those details have not been provided yet.
     */
    public final HashMap<String, AccessLevel> getMembers() {
        return members;
    }

    /**
     * Provides the Map of project {@link Member} username and {@link AccessLevel} of the member.
     *
     * @param members the Map of project {@link Member} username and {@link AccessLevel} of the member.
     */
    public final void setMembers(@CheckForNull HashMap<String, AccessLevel> members) {
        this.members = members;
    }


    /**
     * Returns the {@link GitLabApi} to use for the request.
     *
     * @return the {@link GitLabApi} to use for the request or {@code null} if caller should establish
     * their own.
     */
    @CheckForNull
    public GitLabApi getGitLabApi() {
        return gitLabApi;
    }

    /**
     * Provides the {@link GitLabApi} to use for the request.
     *
     * @param gitLabApi {@link GitLabApi} to use for the request.
     */
    public void setGitLabApi(@CheckForNull GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }


    /**
     * Returns the permissions of the supplied user.
     *
     * @param username the username of MR author
     * @return {@link AccessLevel} the permissions of the supplied user.
     */
    public AccessLevel getPermission(String username){
        if(getGitLabApi() == null || getMembers() == null) {
            return null;
        }
        if(getMembers().containsKey(username)) {
            return getMembers().get(username);
        }
        return null;
    }

    public boolean isMember(String username) {
        if(getMembers() == null) {
            throw new NullPointerException("No members! :O");
        }
        return getMembers().containsKey(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (mergeRequests instanceof Closeable) {
            ((Closeable) mergeRequests).close();
        }
        if (branches instanceof Closeable) {
            ((Closeable) branches).close();
        }
        super.close();
    }

    /**
     * Sets the {@link Project}.
     *
     * @param gitlabProject the {@link Project}.
     */
    public void setProject(Project gitlabProject) {
        this.gitlabProject = gitlabProject;
    }

    @CheckForNull
    public Project getGitlabProject() {
        return gitlabProject;
    }
}
