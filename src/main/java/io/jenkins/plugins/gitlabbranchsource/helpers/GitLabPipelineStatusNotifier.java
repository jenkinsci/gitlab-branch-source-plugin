package io.jenkins.plugins.gitlabbranchsource.helpers;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Node;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.CommitStatus;
import org.gitlab4j.api.models.MergeRequest;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

/**
 * Publishes Build-Status to GitLab using separate threads so it does not block
 * while sending
 * messages TODO: Multi-Threading is easy to get wrong and wreak havoc. Check if
 * there is no better
 * way to do this built into Jenkins
 */
public class GitLabPipelineStatusNotifier {

    public static final Logger LOGGER = Logger.getLogger(GitLabPipelineStatusNotifier.class.getName());

    static final String GITLAB_PIPELINE_STATUS_PREFIX = "jenkinsci";

    static final String GITLAB_PIPELINE_STATUS_DELIMITER = "/";

    static final Pattern MERGE_REQUEST_JOB_NAME_FORMAT = Pattern.compile("MR-(\\d+)(|-(merge|head))");

    private static String getRootUrl(Run<?, ?> build) {
        try {
            return DisplayURLProvider.get().getRunURL(build);
        } catch (IllegalStateException e) {
            return "";
        }
    }

    private static GitLabSCMSourceContext getSourceContext(Run<?, ?> build, GitLabSCMSource source) {
        return new GitLabSCMSourceContext(null, SCMHeadObserver.none()).withTraits((source.getTraits()));
    }

    private static GitLabSCMSource getSource(Run<?, ?> build) {
        final SCMSource s = SCMSource.SourceByItem.findSource(build.getParent());
        if (s instanceof GitLabSCMSource) {
            return (GitLabSCMSource) s;
        }
        return null;
    }

    private static String getStatusName(
            final GitLabSCMSourceContext sourceContext,
            final Run<?, ?> build,
            @NonNull EnvVars envVars,
            final SCMRevision revision) {
        return getStatusName(sourceContext, build.getFullDisplayName(), revision, envVars);
    }

    private static String getStatusName(
            final GitLabSCMSourceContext sourceContext,
            final Job<?, ?> job,
            @NonNull EnvVars envVars,
            final SCMRevision revision) {
        return getStatusName(sourceContext, job.getFullDisplayName(), revision, envVars);
    }

    static String getStatusName(
            final GitLabSCMSourceContext sourceContext,
            final String fullDisplayName,
            final SCMRevision revision,
            @NonNull EnvVars envVars) {
        final String type;
        if (revision instanceof BranchSCMRevision) {
            type = "branch";
        } else if (revision instanceof MergeRequestSCMRevision) {
            type = getMrBuildName((MergeRequestSCMRevision) revision);
        } else if (revision instanceof GitTagSCMRevision) {
            type = "tag";
        } else {
            type = "UNKNOWN";
            LOGGER.log(
                    Level.WARNING,
                    () -> "Unknown SCMRevision implementation "
                            + revision.getClass().getName() + ", append" + type + " to status name");
        }

        // Some comments regarding this implementation :
        // Old code was :
        // GITLAB_PIPELINE_STATUS_PREFIX + GITLAB_PIPELINE_STATUS_DELIMITER + (optional:
        // pipelinePrefix + GITLAB_PIPELINE_STATUS_DELIMITER) + type
        // New implementation:
        // 1- BuildStatusNameCustomPart empty : use default
        // GITLAB_PIPELINE_STATUS_PREFIX
        // 2- else: depending on BuildStatusNameOverwrite : use
        // BuildStatusNameCustomPart appended to GITLAB_PIPELINE_STATUS_PREFIX or use it
        // as is
        // 3- add type only if required (no ignoreTypeInStatusName)

        String pipelinePrefix = sourceContext.getBuildStatusNameCustomPart().trim();
        if (!pipelinePrefix.isEmpty()) {
            pipelinePrefix = envVars.expand(pipelinePrefix);
            if (!sourceContext.getBuildStatusNameOverwrite()) {
                pipelinePrefix = GITLAB_PIPELINE_STATUS_PREFIX + GITLAB_PIPELINE_STATUS_DELIMITER + pipelinePrefix;
            }
        } else {
            pipelinePrefix = GITLAB_PIPELINE_STATUS_PREFIX;
        }
        if (!sourceContext.getIgnoreTypeInStatusName()) {
            pipelinePrefix = pipelinePrefix + GITLAB_PIPELINE_STATUS_DELIMITER + type;
        }
        final String statusName = pipelinePrefix;
        LOGGER.log(Level.FINEST, () -> "Retrieved status name is: " + statusName);
        return statusName;
    }

    static String getRevisionRef(final SCMRevision revision) {
        final String refName;
        if (revision instanceof MergeRequestSCMRevision) {
            refName = ((MergeRequestSCMRevision) revision).getOrigin().getHead().getName();
        } else {
            refName = revision.getHead().getName();
        }
        LOGGER.log(Level.FINEST, () -> "Retrieved revision ref is: " + refName);
        return refName;
    }

    private static String getMrBuildName(final MergeRequestSCMRevision revision) {
        return (revision.isMerge() ? "mr-merge" : "mr-head");
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
            listener.getLogger()
                    .println("Can not determine Jenkins root URL. Comments are disabled until a root URL is"
                            + " configured in Jenkins global configuration.");
            return;
        }
        Result result = build.getResult();
        LOGGER.log(Level.FINE, String.format("Log Comment Result: %s", result));
        String note = "";
        String symbol = "";
        if (Result.SUCCESS.equals(result)) {
            if (!sourceContext.doLogSuccess()) {
                return;
            }
            symbol = ":heavy_check_mark: ";
            note = "The Jenkins CI build passed ";
        } else if (Result.UNSTABLE.equals(result)) {
            symbol = ":heavy_multiplication_x: ";
            note = "The Jenkins CI build failed ";
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
            GitLabApi gitLabApi =
                    GitLabHelper.apiBuilder(build.getParent(), source.getServerName(), source.getCredentialsId());
            String sudoUsername = sourceContext.getSudoUser();
            if (!sudoUsername.isEmpty()) {
                gitLabApi.sudo(sudoUsername);
            }
            try {
                EnvVars envVars = build.getEnvironment(listener);
                final String buildName = "**" + getStatusName(sourceContext, build, envVars, revision) + ":** ";
                final String hash;
                if (revision instanceof BranchSCMRevision) {
                    hash = ((BranchSCMRevision) revision).getHash();
                    gitLabApi
                            .getCommitsApi()
                            .addComment(source.getProjectPath(), hash, symbol + buildName + note + suffix);
                } else if (revision instanceof MergeRequestSCMRevision) {
                    MergeRequestSCMHead head = (MergeRequestSCMHead) revision.getHead();
                    gitLabApi
                            .getNotesApi()
                            .createMergeRequestNote(
                                    source.getProjectPath(),
                                    Long.valueOf(head.getId()),
                                    symbol + buildName + note + suffix);
                } else if (revision instanceof GitTagSCMRevision) {
                    hash = ((GitTagSCMRevision) revision).getHash();
                    gitLabApi
                            .getCommitsApi()
                            .addComment(source.getProjectPath(), hash, symbol + buildName + note + suffix);
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.log(
                        Level.INFO,
                        "Could not send status notification for " + build.getFullDisplayName() + " to "
                                + source.getServerName(),
                        e);
            }
        } catch (GitLabApiException e) {
            LOGGER.log(Level.WARNING, "Exception caught:" + e, e);
        }
    }

    /**
     * Retrieves the source project ID for a merge request
     */
    static Long getSourceProjectId(Job job, GitLabApi gitLabApi, String projectPath) {
        LOGGER.log(Level.INFO, "Getting source project ID from MR");
        Matcher m = MERGE_REQUEST_JOB_NAME_FORMAT.matcher(job.getName());
        if (!m.matches()) {
            LOGGER.log(
                    Level.WARNING,
                    String.format(
                            "Job name does not match expected format: [%s], [%s]",
                            job.getName(), MERGE_REQUEST_JOB_NAME_FORMAT.pattern()));
            return null;
        }

        Long mrId = Long.parseLong(m.group(1));
        MergeRequest mr;
        try {
            mr = gitLabApi.getMergeRequestApi().getMergeRequest(projectPath, mrId);
        } catch (GitLabApiException e) {
            if (!e.getMessage().contains(("Cannot transition status"))) {
                LOGGER.log(Level.WARNING, String.format("Exception caught: %s", e.getMessage()));
            }
            return null;
        }
        Long sourceProjectId = mr.getSourceProjectId();
        LOGGER.log(Level.INFO, "Got source project ID from MR: {0}", String.valueOf(sourceProjectId));

        return sourceProjectId;
    }

    /**
     * Sends notifications to GitLab on Checkout (for the "In Progress" Status).
     */
    private static void sendNotifications(Run<?, ?> build, TaskListener listener, Boolean useResult) {
        GitLabSCMSource source = getSource(build);
        if (source == null) {
            return;
        }
        final GitLabSCMSourceContext sourceContext = getSourceContext(build, source);
        if (sourceContext.notificationsDisabled()) {
            return;
        }
        String url = getRootUrl(build);
        if (url.isEmpty()) {
            listener.getLogger()
                    .println(
                            "Can not determine Jenkins root URL. Commit status notifications are disabled until a root URL is"
                                    + " configured in Jenkins global configuration.");
            return;
        }
        Result result = null;
        if (useResult) {
            result = build.getResult();
            LOGGER.log(Level.FINE, String.format("Result: %s", result));
        }

        CommitStatus status = new CommitStatus();
        Constants.CommitBuildState state;
        status.setTargetUrl(url);

        if (Result.SUCCESS.equals(result)) {
            status.setDescription(build.toString() + ": This commit looks good.");
            status.setStatus("SUCCESS");
            state = Constants.CommitBuildState.SUCCESS;
        } else if (Result.UNSTABLE.equals(result)) {
            status.setDescription(build.toString() + ": This commit is unstable with partial failure.");
            if (sourceContext.getMarkUnstableAsSuccess()) {
                status.setStatus("SUCCESS");
                state = Constants.CommitBuildState.SUCCESS;
            } else {
                status.setStatus("FAILED");
                state = Constants.CommitBuildState.FAILED;
            }
        } else if (Result.FAILURE.equals(result)) {
            status.setDescription(build.toString() + ": There was a failure building this commit.");
            status.setStatus("FAILED");
            state = Constants.CommitBuildState.FAILED;
        } else if (result != null) { // ABORTED, NOT_BUILT.
            status.setDescription(build.toString() + ": Something is wrong with the build of this commit.");
            status.setStatus("CANCELED");
            state = Constants.CommitBuildState.CANCELED;
        } else {
            status.setDescription(build.toString() + ": Build started...");
            status.setStatus("RUNNING");
            state = Constants.CommitBuildState.RUNNING;
        }

        final SCMRevision revision = SCMRevisionAction.getRevision(source, build);
        String hash;
        if (revision instanceof BranchSCMRevision) {
            listener.getLogger()
                    .format(
                            "[GitLab Pipeline Status] Notifying branch build status: %s %s%n",
                            status.getStatus(), status.getDescription());
            hash = ((BranchSCMRevision) revision).getHash();
        } else if (revision instanceof MergeRequestSCMRevision) {
            listener.getLogger()
                    .format(
                            "[GitLab Pipeline Status] Notifying merge request build status: %s %s%n",
                            status.getStatus(), status.getDescription());
            hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
        } else if (revision instanceof GitTagSCMRevision) {
            listener.getLogger()
                    .format(
                            "[GitLab Pipeline Status] Notifying tag build status: %s %s%n",
                            status.getStatus(), status.getDescription());
            hash = ((GitTagSCMRevision) revision).getHash();
        } else {
            return;
        }
        try {
            EnvVars envVars = build.getEnvironment(listener);
            status.setName(getStatusName(sourceContext, build, envVars, revision));
            status.setRef(getRevisionRef(revision));

            final JobScheduledListener jsl =
                    ExtensionList.lookup(QueueListener.class).get(JobScheduledListener.class);
            if (jsl != null) {
                // we are setting the status, so don't let the queue listener background thread
                // change it to pending
                synchronized (jsl.resolving) {
                    jsl.resolving.remove(build.getParent());
                }
            }
            try {
                GitLabApi gitLabApi =
                        GitLabHelper.apiBuilder(build.getParent(), source.getServerName(), source.getCredentialsId());
                LOGGER.log(Level.FINE, String.format("Notifiying commit: %s", hash));

                if (revision instanceof MergeRequestSCMRevision) {
                    Long projectId = getSourceProjectId(build.getParent(), gitLabApi, source.getProjectPath());
                    status.setRef(((MergeRequestSCMRevision) revision)
                            .getOrigin()
                            .getHead()
                            .getName());
                    gitLabApi.getCommitsApi().addCommitStatus(projectId, hash, state, status);
                } else {
                    gitLabApi.getCommitsApi().addCommitStatus(source.getProjectPath(), hash, state, status);
                }

                listener.getLogger().format("[GitLab Pipeline Status] Notified%n");
            } catch (GitLabApiException e) {
                if (!e.getMessage().contains(("Cannot transition status"))) {
                    LOGGER.log(Level.WARNING, String.format("Exception caught: %s", e.getMessage()));
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(
                    Level.INFO,
                    "Could not send status notification for " + build.getFullDisplayName() + " to "
                            + source.getServerName(),
                    e);
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
            LOGGER.log(Level.FINE, String.format("QueueListener: Waiting > %s", job.getFullDisplayName()));
            final SCMSource src = SCMSource.SourceByItem.findSource(job);
            if (!(src instanceof GitLabSCMSource)) {
                return;
            }
            final GitLabSCMSource source = (GitLabSCMSource) src;
            final GitLabSCMSourceContext sourceContext =
                    new GitLabSCMSourceContext(null, SCMHeadObserver.none()).withTraits((source.getTraits()));
            if (sourceContext.notificationsDisabled()) {
                return;
            }
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
                    final TaskListener listener = new LogTaskListener(LOGGER, Level.INFO);
                    final SCMRevision revision = source.fetch(head, listener);
                    String hash;
                    final CommitStatus status = new CommitStatus();
                    if (revision instanceof BranchSCMRevision) {
                        LOGGER.log(Level.INFO, "Notifying branch pending build {0}", job.getFullName());
                        hash = ((BranchSCMRevision) revision).getHash();
                    } else if (revision instanceof MergeRequestSCMRevision) {
                        LOGGER.log(Level.INFO, "Notifying merge request pending build {0}", job.getFullName());
                        hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
                    } else if (revision instanceof GitTagSCMRevision) {
                        LOGGER.log(Level.INFO, "Notifying tag pending build {0}", job.getFullName());
                        hash = ((GitTagSCMRevision) revision).getHash();
                    } else {
                        return;
                    }
                    Computer c = Computer.currentComputer();
                    Node n = c == null ? null : c.getNode();
                    EnvVars envVars = job.getEnvironment(n, listener);
                    status.setName(getStatusName(sourceContext, job, envVars, revision));
                    status.setRef(getRevisionRef(revision));

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
                        GitLabApi gitLabApi =
                                GitLabHelper.apiBuilder(job, source.getServerName(), source.getCredentialsId());
                        // check are we still the task to set pending
                        synchronized (resolving) {
                            if (!nonce.equals(resolving.get(job))) {
                                // it's not our nonce, so drop
                                LOGGER.log(
                                        Level.INFO,
                                        "{0} has already started, skipping notification of queued",
                                        job.getFullName());
                                return;
                            }
                            // it is our nonce, so remove it
                            resolving.remove(job);
                        }

                        if (revision instanceof MergeRequestSCMRevision) {
                            Long projectId = getSourceProjectId(job, gitLabApi, source.getProjectPath());
                            status.setRef(((MergeRequestSCMRevision) revision)
                                    .getOrigin()
                                    .getHead()
                                    .getName());
                            gitLabApi.getCommitsApi().addCommitStatus(projectId, hash, state, status);
                        } else {
                            gitLabApi.getCommitsApi().addCommitStatus(source.getProjectPath(), hash, state, status);
                        }

                        LOGGER.log(Level.INFO, "{0} Notified", job.getFullName());
                    } catch (GitLabApiException e) {
                        if (!e.getMessage().contains("Cannot transition status")) {
                            LOGGER.log(Level.WARNING, String.format("Exception caught: %s", e.getMessage()));
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.log(
                            Level.INFO,
                            "Could not send commit status notification for " + job.getFullName() + " to "
                                    + source.getServerName(),
                            e);
                }
            });
        }
    }

    @Extension
    public static class JobCheckOutListener extends SCMListener {

        @Override
        public void onCheckout(
                Run<?, ?> build,
                SCM scm,
                FilePath workspace,
                TaskListener listener,
                File changelogFile,
                SCMRevisionState pollingBaseline) {
            LOGGER.log(Level.FINE, String.format("SCMListener: Checkout > %s", build.getFullDisplayName()));
            sendNotifications(build, listener, false);
        }
    }

    /**
     * Sends notifications to GitLab on Run completed.
     */
    @Extension
    public static class JobCompletedListener extends RunListener<Run<?, ?>> {

        @Override
        public void onCompleted(Run<?, ?> build, @NonNull TaskListener listener) {
            LOGGER.log(Level.FINE, String.format("RunListener: Complete > %s", build.getFullDisplayName()));
            sendNotifications(build, listener, true);
            logComment(build, listener);
        }

        @Override
        public void onStarted(Run<?, ?> run, TaskListener listener) {
            LOGGER.log(Level.FINE, String.format("RunListener: Started > %s", run.getFullDisplayName()));
            sendNotifications(run, listener, false);
        }
    }
}
