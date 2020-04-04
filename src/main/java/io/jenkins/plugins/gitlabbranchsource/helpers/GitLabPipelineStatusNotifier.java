package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.InvisibleAction;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.model.listeners.SCMListener;
import hudson.model.queue.QueueListener;
import hudson.model.queue.Tasks;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.LogTaskListener;
import io.jenkins.plugins.gitlabbranchsource.BranchSCMRevision;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSource;
import io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMHead;
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMRevision;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.Constants.CommitBuildState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.CommitStatus;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import static hudson.model.Result.ABORTED;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;

/**
 * Publishes Build-Status to GitLab using separate threads so it does not block while sending
 * messages TODO: Multi-Threading is easy to get wrong and wreak havoc. Check if there is no better
 * way to do this built into Jenkins
 */
public class GitLabPipelineStatusNotifier {

    private static final Logger LOGGER = Logger
        .getLogger(GitLabPipelineStatusNotifier.class.getName());

    private static String getRootUrl(Run<?, ?> build) {
        try {
            return DisplayURLProvider.get().getRunURL(build);
        } catch (IllegalStateException e) {
            return "";
        }
    }

    private static GitLabSCMSourceContext getSourceContext(Run<?, ?> build,
        GitLabSCMSource source) {
        return new GitLabSCMSourceContext(null, SCMHeadObserver.none())
            .withTraits((source.getTraits()));
    }

    private static GitLabSCMSource getSource(Run<?, ?> build) {
        final SCMSource s = SCMSource.SourceByItem.findSource(build.getParent());
        if (s instanceof GitLabSCMSource) {
            return (GitLabSCMSource) s;
        }
        return null;
    }

    private static String getMrBuildName(String buildName) {
        String suffix = "jenkinsci/";
        if (buildName.contains("merge")) {
            return suffix + "mr-merge";
        }
        return suffix + "mr-head";
    }

    /**
     * Log comment on Commits and Merge Requests upon build complete.
     */
    private static void logComment(Run<?, ?> build, TaskListener listener) {
        GitLabSCMSource source = getSource(build);
        if (source == null) {
            return;
        }
        final GitLabSCMSourceContext sourceContext = getSourceContext(build, source);
        if (!sourceContext.logCommentEnabled()) {
            return;
        }
        String url = getRootUrl(build);
        if (url.isEmpty()) {
            listener.getLogger().println(
                "Can not determine Jenkins root URL. Comments are disabled until a root URL is"
                    + " configured in Jenkins global configuration.");
            return;
        }
        Result result = build.getResult();
        LOGGER.log(Level.FINE, String.format("Log Comment Result: %s", result));
        String note = "";
        String symbol = "";
        if (SUCCESS.equals(result)) {
            if (!sourceContext.doLogSuccess()) {
                return;
            }
            symbol = ":heavy_check_mark: ";
            note = "The Jenkins CI build passed ";
        } else if (UNSTABLE.equals(result)) {
            symbol = ":exclamation:  ";
            note = "The Jenkins CI build is unstable ";
        } else if (Result.FAILURE.equals(result)) {
            symbol = ":heavy_multiplication_x: ";
            note = "The Jenkins CI build failed ";
        } else if (result != null) { // ABORTED, NOT_BUILT.
            symbol = ":no_entry_sign: ";
            note = "The Jenkins CI build aborted ";
        }
        String suffix = " - [Details](" + url + ")";
        SCMRevision revision = SCMRevisionAction.getRevision(source, build);
        try {
            GitLabApi gitLabApi = GitLabHelper.apiBuilder(source.getServerName());
            String sudoUsername = sourceContext.getSudoUser();
            if (!sudoUsername.isEmpty()) {
                gitLabApi.sudo(sudoUsername);
            }
            String hash;
            if (revision instanceof BranchSCMRevision) {
                hash = ((BranchSCMRevision) revision).getHash();
                String buildName = "**jenkinsci/branch:** ";
                gitLabApi.getCommitsApi().addComment(
                    source.getProjectPath(),
                    hash,
                    symbol + buildName + note + suffix
                );
            } else if (revision instanceof MergeRequestSCMRevision) {
                MergeRequestSCMHead head = (MergeRequestSCMHead) revision.getHead();
                String buildName = "**" + getMrBuildName(build.getFullDisplayName()) + "**: ";
                gitLabApi.getNotesApi().createMergeRequestNote(
                    source.getProjectPath(),
                    Integer.valueOf(head.getId()),
                    symbol + buildName + note + suffix
                );
            } else if (revision instanceof GitTagSCMRevision) {
                hash = ((GitTagSCMRevision) revision).getHash();
                String buildName = "**jenkinsci/tag:** ";
                gitLabApi.getCommitsApi().addComment(
                    source.getProjectPath(),
                    hash,
                    symbol + buildName + note + suffix
                );
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING, "Exception caught:" + e, e);
        }
    }

    /**
     * Sends notifications to GitLab on Checkout (for the "In Progress" Status).
     */
    private static void sendNotifications(Run<?, ?> build, TaskListener listener) {
        GitLabSCMSource source = getSource(build);
        if (source == null) {
            return;
        }
        final GitLabSCMSourceContext sourceContext = getSourceContext(build, source);
        if (sourceContext.notificationsDisabled() || sourceContext.getPipelineStatusStrategy()
            .contains(GitLabPipelineStatusStrategy.NONE)) {
            return;
        } else if (sourceContext.getPipelineStatusStrategy()
            .contains(GitLabPipelineStatusStrategy.STAGES)) {
            attachGraphListener((WorkflowRun) build, new GitLabSCMGraphListener(build));
        } else {
            String url = getRootUrl(build);
            if (url.isEmpty()) {
                listener.getLogger().println(
                    "Can not determine Jenkins root URL. Commit status notifications are disabled until a root URL is"
                        + " configured in Jenkins global configuration.");
                return;
            }
            Result result = build.getResult();
            LOGGER.log(Level.FINE, String.format("Result: %s", result));

            CommitStatus status = new CommitStatus();
            CommitBuildState state;
            status.setTargetUrl(url);

            if (SUCCESS.equals(result)) {
                status.setDescription(build.getParent().getFullName() + ": This commit looks good");
                status.setStatus("SUCCESS");
                state = Constants.CommitBuildState.SUCCESS;
            } else if (UNSTABLE.equals(result)) {
                status.setDescription(
                    build.getParent().getFullName() + ": This commit has test failures");
                if (sourceContext.isMarkUnstableAsSuccess()) {
                    status.setStatus("SUCCESS");
                } else {
                    status.setStatus("FAILED");
                }
                state = Constants.CommitBuildState.FAILED;
            } else if (Result.FAILURE.equals(result)) {
                status.setDescription(
                    build.getParent().getFullName() + ": There was a failure building this commit");
                status.setStatus("FAILED");
                state = Constants.CommitBuildState.FAILED;
            } else if (result != null) { // ABORTED, NOT_BUILT.
                status.setDescription(build.getParent().getFullName()
                    + ": Something is wrong with the build of this commit");
                status.setStatus("CANCELED");
                state = Constants.CommitBuildState.CANCELED;
            } else {
                status.setDescription(build.getParent().getFullName() + ": Build started...");
                status.setStatus("RUNNING");
                state = Constants.CommitBuildState.RUNNING;
            }

            SCMRevision revision = SCMRevisionAction.getRevision(source, build);
            String hash;
            if (revision instanceof BranchSCMRevision) {
                listener.getLogger()
                    .format("[GitLab Pipeline Status] Notifying branch build status: %s %s%n",
                        status.getStatus(), status.getDescription());
                hash = ((BranchSCMRevision) revision).getHash();
                status.setName("jenkinsci/branch");
            } else if (revision instanceof MergeRequestSCMRevision) {
                listener.getLogger()
                    .format(
                        "[GitLab Pipeline Status] Notifying merge request build status: %s %s%n",
                        status.getStatus(), status.getDescription());
                hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
                status.setName(getMrBuildName(build.getFullDisplayName()));
            } else if (revision instanceof GitTagSCMRevision) {
                listener.getLogger()
                    .format("[GitLab Pipeline Status] Notifying tag build status: %s %s%n",
                        status.getStatus(), status.getDescription());
                hash = ((GitTagSCMRevision) revision).getHash();
                status.setName("jenkinsci/tag");
            } else {
                return;
            }
            JobScheduledListener jsl = ExtensionList.lookup(QueueListener.class)
                .get(JobScheduledListener.class);
            if (jsl != null) {
                // we are setting the status, so don't let the queue listener background thread change it to pending
                synchronized (jsl.resolving) {
                    jsl.resolving.remove(build.getParent());
                }
            }
            try {
                GitLabApi gitLabApi = GitLabHelper.apiBuilder(source.getServerName());
                LOGGER.log(Level.FINE, String.format("Notifiying commit: %s", hash));
                gitLabApi.getCommitsApi().addCommitStatus(
                    source.getProjectPath(),
                    hash,
                    state,
                    status);
                listener.getLogger().format("[GitLab Pipeline Status] Notified%n");
            } catch (GitLabApiException e) {
                LOGGER.log(Level.WARNING, "Exception caught adding commit status:" + e, e);
            }
        }
    }

    @Extension
    public static class JobScheduledListener extends QueueListener {

        private final AtomicLong nonce = new AtomicLong();
        private final Map<Job, Long> resolving = new HashMap<>();

        /**
         * Manages the GitLab Commit Pending Status.
         */
        @Override
        public void onEnterWaiting(final Queue.WaitingItem wi) {
            if (!(wi.task instanceof Job)) {
                return;
            }
            final Job<?, ?> job = (Job) wi.task;
            LOGGER.log(Level.FINE,
                String.format("QueueListener: Waiting > %s", job.getFullDisplayName()));
            final SCMSource src = SCMSource.SourceByItem.findSource(job);
            if (!(src instanceof GitLabSCMSource)) {
                return;
            }
            final GitLabSCMSource source = (GitLabSCMSource) src;
            final GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null,
                SCMHeadObserver.none())
                .withTraits((source.getTraits()));
            if (sourceContext.notificationsDisabled()) {
                return;
            } else if (sourceContext.getPipelineStatusStrategy()
                .contains(GitLabPipelineStatusStrategy.RESULT)) {
                final SCMHead head = SCMHead.HeadByItem.findHead(job);
                if (head == null) {
                    return;
                }
                final Long nonce = this.nonce.incrementAndGet();
                synchronized (resolving) {
                    resolving.put(job, nonce);
                }
                // prevent delays in the queue when updating GitLab
                Computer.threadPoolForRemoting.submit(() -> {
                    try (ACLContext ctx = ACL.as(Tasks.getAuthenticationOf(wi.task))) {
                        SCMRevision revision = source
                            .fetch(head, new LogTaskListener(LOGGER, Level.INFO));
                        String hash;
                        CommitStatus status = new CommitStatus();
                        if (revision instanceof BranchSCMRevision) {
                            LOGGER.log(Level.INFO, "Notifying branch pending build {0}",
                                job.getFullName());
                            hash = ((BranchSCMRevision) revision).getHash();
                            status.setName("jenkinsci/branch");
                        } else if (revision instanceof MergeRequestSCMRevision) {
                            LOGGER.log(Level.INFO, "Notifying merge request pending build {0}",
                                job.getFullName());
                            hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
                            status.setName(getMrBuildName(job.getFullDisplayName()));
                        } else if (revision instanceof GitTagSCMRevision) {
                            LOGGER.log(Level.INFO, "Notifying tag pending build {0}",
                                job.getFullName());
                            hash = ((GitTagSCMRevision) revision).getHash();
                            status.setName("jenkinsci/tag");
                        } else {
                            return;
                        }
                        String url;
                        try {
                            url = DisplayURLProvider.get().getJobURL(job);
                        } catch (IllegalStateException e) {
                            // no root url defined, cannot notify, let's get out of here
                            return;
                        }
                        status.setTargetUrl(url);
                        status.setDescription(job.getFullName() + ": Build queued...");
                        status.setStatus("PENDING");
                        Constants.CommitBuildState state = Constants.CommitBuildState.PENDING;
                        try {
                            GitLabApi gitLabApi = GitLabHelper.apiBuilder(source.getServerName());
                            // check are we still the task to set pending
                            synchronized (resolving) {
                                if (!nonce.equals(resolving.get(job))) {
                                    // it's not our nonce, so drop
                                    LOGGER.log(Level.INFO,
                                        "{0} has already started, skipping notification of queued",
                                        job.getFullName());
                                    return;
                                }
                                // it is our nonce, so remove it
                                resolving.remove(job);
                            }
                            gitLabApi.getCommitsApi().addCommitStatus(
                                source.getProjectPath(),
                                hash,
                                state,
                                status);
                            LOGGER.log(Level.INFO, "{0} Notified", job.getFullName());
                        } catch (GitLabApiException e) {
                            LOGGER.log(Level.WARNING, "Exception caught: " + e, e);
                        }
                    } catch (IOException | InterruptedException e) {
                        LOGGER.log(Level.INFO,
                            "Could not send commit status notification for " + job.getFullName()
                                + " to " + source
                                .getServerName(), e);
                    }
                });
            }
        }
    }

    @Extension
    public static class JobCheckOutListener extends SCMListener {

        @Override
        public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener,
            File changelogFile,
            SCMRevisionState pollingBaseline) {
            LOGGER.log(Level.FINE,
                String.format("SCMListener: Checkout > %s", build.getFullDisplayName()));
            sendNotifications(build, listener);
        }
    }

    /**
     * Sends notifications to GitLab on Run completed.
     */
    @Extension
    public static class JobCompletedListener extends RunListener<Run<?, ?>> {

        @Override
        public void onCompleted(Run<?, ?> build, @NonNull TaskListener listener) {
            LOGGER.log(Level.FINE,
                String.format("RunListener: Complete > %s", build.getFullDisplayName()));
            sendNotifications(build, listener);
            logComment(build, listener);

        }

        @Override
        public void onStarted(Run<?, ?> run, TaskListener listener) {
            LOGGER.log(Level.FINE,
                String.format("RunListener: Started > %s", run.getFullDisplayName()));
            sendNotifications(run, listener);
        }
    }

    private static void publishBuildStatus(Run<?, ?> run, CommitBuildState state, String context,
        String description) {
        LOGGER.log(Level.INFO, context + "my msg");
    }


    private static void attachGraphListener(final WorkflowRun build, final GraphListener listener) {
        build.getExecutionPromise().addListener(
            new Runnable() {
                @Override
                public void run() {
                    build.addAction(new RunningContextsAction());
                    FlowExecution execution = build.getExecution();
                    if (execution != null) {
                        execution.addListener(listener);
                    } else {
                        LOGGER.log(Level.SEVERE,
                            "could not get flow-execution for build " + build.getFullDisplayName());
                    }
                }
            },
            Executors.newSingleThreadExecutor()
        );
    }

    public static CommitBuildState toBuildStateFromResult(final Result result,
        boolean markUnstableAsSuccess) {
        if ((result == SUCCESS) || ((result == UNSTABLE) && markUnstableAsSuccess)) {
            return CommitBuildState.SUCCESS;
        } else if (result == ABORTED) {
            return CommitBuildState.CANCELED;
        } else {
            return CommitBuildState.FAILED;
        }
    }


    private static final class GitLabSCMGraphListener implements GraphListener {

        private final Run<?, ?> build;


        GitLabSCMGraphListener(Run<?, ?> build) {
            this.build = build;
        }

        @Override
        public void onNewHead(FlowNode node) {
            if (isNamedStageStartNode(node)) {
                publishBuildStatus(build, CommitBuildState.RUNNING, getRunningContexts().push(node),
                    "");
            } else if (isStageEndNode(node, getRunningContexts().peekNodeId())) {
                // If this or a prior stage failed then build.result is set to 'FAILED'
                // otherwise build.result is still null and we assume success.
                CommitBuildState state = CommitBuildState.SUCCESS;
                if (build.getResult() != null) {
                    GitLabSCMSource source = getSource(build);
                    if (source == null) {
                        return;
                    }
                    final GitLabSCMSourceContext sourceContext = getSourceContext(build, source);
                    state = toBuildStateFromResult(build.getResult(),
                        sourceContext.isMarkUnstableAsSuccess());
                }
                // If there is an exception of some kind in Jenkins, the node will contain
                // an error and we publish this stage as failed.
                if (node.getError() != null) {
                    state = CommitBuildState.FAILED;
                }
                String context = getRunningContexts().pop();
                publishBuildStatus(build, state, context, "");
            }
        }

        private boolean isStageEndNode(FlowNode node, String startNodeId) {
            return startNodeId != null && node instanceof StepEndNode && ((StepEndNode) node)
                .getStartNode().getId().equals(startNodeId);
        }

        private boolean isNamedStageStartNode(FlowNode node) {
            return node instanceof StepStartNode && Objects
                .equals(((StepStartNode) node).getStepName(), "Stage") && !Objects
                .equals(node.getDisplayFunctionName(), "stage");
        }

        private RunningContextsAction getRunningContexts() {
            return build.getAction(RunningContextsAction.class);
        }
    }

    private static final class RunningContextsAction extends InvisibleAction implements
        Serializable {

        private final Stack<String> nodeIds;
        private final LinkedHashMap<String, String> contexts;
        private int stageCount = 0;

        RunningContextsAction() {
            nodeIds = new Stack<>();
            contexts = new LinkedHashMap<>();
        }

        RunningContextsAction(String context) {
            this();
            contexts.put(context, context);
        }

        String push(FlowNode node) {
            return push(node.getId(), node.getDisplayName());
        }

        private String push(String id, String name) {
            nodeIds.push(id);
            String context = "#" + (++stageCount) + " " + name;
            contexts.put(id, context);
            return context;
        }

        String peekNodeId() {
            return !nodeIds.isEmpty() ? nodeIds.peek() : null;
        }

        String pop() {
            String nodeId = nodeIds.pop();
            return contexts.remove(nodeId);
        }

        Collection<String> clear() {
            List<String> names = new ArrayList<>(contexts.values());

            nodeIds.clear();
            contexts.clear();

            Collections.reverse(names);
            return names;
        }
    }
}
