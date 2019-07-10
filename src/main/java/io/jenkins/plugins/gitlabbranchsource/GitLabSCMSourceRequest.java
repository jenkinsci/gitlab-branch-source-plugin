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
import java.util.Map;
import java.util.Set;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.mixin.TagSCMHead;
import jenkins.scm.api.trait.SCMSourceRequest;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.MergeRequest;

public class GitLabSCMSourceRequest extends SCMSourceRequest {

    private final boolean fetchBranches;
    private final boolean fetchTags;
    private final boolean fetchOriginMRs;
    private final boolean fetchForkMRs;

    @NonNull
    private final Set<ChangeRequestCheckoutStrategy> originMRStrategies;
    @NonNull
    private final Set<ChangeRequestCheckoutStrategy> forkMRStrategies;
    @CheckForNull
    private final Set<Long> requestedMergeRequestNumbers;
    @CheckForNull
    private final Set<String> requestedOriginBranchNames;
    @CheckForNull
    private final Set<String> requestedTagNames;

    @CheckForNull
    private Iterable<MergeRequest> mergeRequests;
    @CheckForNull
    private Iterable<Branch> branches;

    /**
     * The project collaborator names or {@code null} if not provided.
     */
    @CheckForNull
    private Set<String> collaboratorNames;

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
                } else if (h instanceof TagSCMHead) { // TODO replace with concrete class when tag support added
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

    // TODO Iterable<Tag> getTags() and setTags(...)

    /**
     * Returns the names of the project collaborators or {@code null} if those details have not been provided yet.
     *
     * @return the names of the project collaborators or {@code null} if those details have not been provided yet.
     */
    public final Set<String> getCollaboratorNames() {
        return collaboratorNames;
    }

    /**
     * Provides the request with the names of the project collaborators.
     *
     * @param collaboratorNames the names of the project collaborators.
     */
    public final void setCollaboratorNames(@CheckForNull Set<String> collaboratorNames) {
        this.collaboratorNames = collaboratorNames;
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

}
