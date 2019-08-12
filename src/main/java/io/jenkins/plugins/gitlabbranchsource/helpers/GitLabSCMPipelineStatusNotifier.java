package io.jenkins.plugins.gitlabbranchsource.helpers;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.Computer;
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
import javax.annotation.Nonnull;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.CommitStatus;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

/**
 * Publishes Build-Status to GitLab using separate threads so it does not block while sending messages
 * TODO: Multi-Threading is easy to get wrong and wreak havoc. Check if there is no better way to do this built into Jenkins
 */
public class GitLabSCMPipelineStatusNotifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMPipelineStatusNotifier.class.getName());

    private static String getRootUrl(Run<?,?> build) {
        try {
            return DisplayURLProvider.get().getRunURL(build);
        } catch (IllegalStateException e) {
            return "";
        }
    }

    private static GitLabSCMSourceContext getSourceContext(Run<?,?> build, GitLabSCMSource source) {
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
        if(buildName.contains("merge")) {
            return suffix + "mr-merge";
        }
        return suffix + "mr-head";
    }

    /**
     * Log comment on Commits and Merge Requests upon build complete.
     */
    private static void logComment(Run<?,?> build, TaskListener listener) {
        GitLabSCMSource source = getSource(build);
        if(source == null) {
            return;
        }
        final GitLabSCMSourceContext sourceContext = getSourceContext(build, source);
        if (!sourceContext.logComment()) {
            return;
        }
        String url = getRootUrl(build);
        if(url.isEmpty()) {
            listener.getLogger().println(
                    "Can not determine Jenkins root URL. Comments are disabled until a root URL is"
                            + " configured in Jenkins global configuration.");
            return;
        }
        Result result = build.getResult();
        LOGGER.info("Log Comment Result: " + result);
        String note = "";
        String symbol = "";
        if (Result.SUCCESS.equals(result)) {
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
        String suffix = " - [Details](" + url + "console)";
        SCMRevision revision = SCMRevisionAction.getRevision(source, build);
        try {
            GitLabApi gitLabApi = GitLabHelper.apiBuilder(source.getServerName());
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
            } else if (revision instanceof GitTagSCMRevision){
                hash = ((GitTagSCMRevision) revision).getHash();
                String buildName = "**jenkinsci/tag:** ";
                gitLabApi.getCommitsApi().addComment(
                        source.getProjectPath(),
                        hash,
                        symbol + buildName + note + suffix
                );
            }
        } catch (NoSuchFieldException | GitLabApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends notifications to GitLab on Checkout (for the "In Progress" Status).
     */
    private static void sendNotifications(Run<?, ?> build, TaskListener listener) {
        GitLabSCMSource source = getSource(build);
        if(source == null) {
            return;
        }
        final GitLabSCMSourceContext sourceContext = getSourceContext(build, source);
        if (sourceContext.notificationsDisabled()) {
            return;
        }
        String url = getRootUrl(build);
        if(url.isEmpty()) {
            listener.getLogger().println(
                    "Can not determine Jenkins root URL. Commit status notifications are disabled until a root URL is"
                            + " configured in Jenkins global configuration.");
            return;
        }
        Result result = build.getResult();
        LOGGER.info("Result: " + result);

        CommitStatus status = new CommitStatus();
        Constants.CommitBuildState state;
        status.setTargetUrl(url);

        if (Result.SUCCESS.equals(result)) {
            status.setDescription(build.getParent().getFullName() + ": This commit looks good");
            status.setStatus("SUCCESS");
            state = Constants.CommitBuildState.SUCCESS;
        } else if (Result.UNSTABLE.equals(result)) {
            status.setDescription(build.getParent().getFullName() + ": This commit has test failures");
            status.setStatus("FAILED");
            state = Constants.CommitBuildState.FAILED;
        } else if (Result.FAILURE.equals(result)) {
            status.setDescription(build.getParent().getFullName() + ": There was a failure building this commit");
            status.setStatus("FAILED");
            state = Constants.CommitBuildState.FAILED;
        } else if (result != null) { // ABORTED, NOT_BUILT.
            status.setDescription(build.getParent().getFullName() + ": Something is wrong with the build of this commit");
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
            listener.getLogger().format("[GitLab Pipeline Status] Notifying branch build status: %s %s%n",
                    status.getStatus(), status.getDescription());
            hash = ((BranchSCMRevision) revision).getHash();
            status.setName("jenkinsci/branch");
        } else if (revision instanceof MergeRequestSCMRevision) {
            listener.getLogger().format("[GitLab Pipeline Status] Notifying merge request build status: %s %s%n",
                    status.getStatus(), status.getDescription());
            hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
            status.setName(getMrBuildName(build.getFullDisplayName()));
        } else if (revision instanceof GitTagSCMRevision){
            listener.getLogger().format("[GitLab Pipeline Status] Notifying tag build status: %s %s%n",
                    status.getStatus(), status.getDescription());
            hash = ((GitTagSCMRevision) revision).getHash();
            status.setName("jenkinsci/tag");
        } else {
            return;
        }
        JobScheduledListener jsl = ExtensionList.lookup(QueueListener.class).get(JobScheduledListener.class);
        if (jsl != null) {
            // we are setting the status, so don't let the queue listener background thread change it to pending
            synchronized (jsl.resolving) {
                jsl.resolving.remove(build.getParent());
            }
        }
        try {
            GitLabApi gitLabApi = GitLabHelper.apiBuilder(source.getServerName());
            LOGGER.info("COMMIT: " + hash);
            gitLabApi.getCommitsApi().addCommitStatus(
                    source.getProjectPath(),
                    hash,
                    state,
                    status);
            listener.getLogger().format("[GitLab Pipeline Status] Notified%n");
        } catch (NoSuchFieldException | GitLabApiException e) {
            e.printStackTrace();
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
            LOGGER.info("QueueListener: Waiting" + job.getFullDisplayName());
            final SCMSource src = SCMSource.SourceByItem.findSource(job);
            if (!(src instanceof GitLabSCMSource)) {
                return;
            }
            final GitLabSCMSource source = (GitLabSCMSource) src;
            final GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                    .withTraits((source.getTraits()));
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
            Computer.threadPoolForRemoting.submit(new Runnable() {
                @Override
                public void run() {
                    SecurityContext context = ACL.impersonate(Tasks.getAuthenticationOf(wi.task));
                    try {
                        SCMRevision revision = source.fetch(head, new LogTaskListener(LOGGER, Level.INFO));
                        String hash;
                        CommitStatus status = new CommitStatus();
                        if (revision instanceof BranchSCMRevision) {
                            LOGGER.log(Level.INFO, "Notifying branch pending build {0}", job.getFullName());
                            hash = ((BranchSCMRevision) revision).getHash();
                            status.setName("jenkinsci/branch");
                        } else if (revision instanceof MergeRequestSCMRevision) {
                            LOGGER.log(Level.INFO, "Notifying merge request pending build {0}", job.getFullName());
                            hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
                            status.setName(getMrBuildName(job.getFullDisplayName()));
                        } else if (revision instanceof GitTagSCMRevision){
                            LOGGER.log(Level.INFO, "Notifying tag pending build {0}", job.getFullName());
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
                        } catch (NoSuchFieldException | GitLabApiException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException | InterruptedException e) {
                        LOGGER.log(Level.INFO,
                                "Could not send commit status notification for " + job.getFullName() + " to " + source
                                        .getServerName(), e);
                    } finally {
                        SecurityContextHolder.setContext(context);
                    }
                }
            });
        }

    }

    @Extension
    public static class JobCheckOutListener extends SCMListener {
        @Override
        public void onCheckout(Run<?, ?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile,
                               SCMRevisionState pollingBaseline) {
            LOGGER.info("SCMListener: Checkout" + build.getFullDisplayName());
                sendNotifications(build, listener);
        }
    }

    /**
     * Sends notifications to GitLab on Run completed.
     */
    @Extension
    public static class JobCompletedListener extends RunListener<Run<?, ?>> {

        @Override
        public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
            LOGGER.info("RunListener: Complete" + build.getFullDisplayName());
            sendNotifications(build, listener);
            logComment(build, listener);

        }

        @Override
        public void onStarted(Run<?, ?> run, TaskListener listener) {
            LOGGER.info("RunListener: Started"+run.getFullDisplayName());
            sendNotifications(run, listener);
        }
    }
}
