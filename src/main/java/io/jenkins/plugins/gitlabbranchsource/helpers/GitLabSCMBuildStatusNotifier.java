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
import io.jenkins.plugins.gitlabbranchsource.MergeRequestSCMRevision;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class GitLabSCMBuildStatusNotifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMBuildStatusNotifier.class.getName());

    /**
     * Sends notifications to GitLab on Checkout (for the "In Progress" Status).
     */
    private static void sendNotifications(Run<?, ?> build, TaskListener listener)
            throws IOException, InterruptedException {
        final SCMSource s = SCMSource.SourceByItem.findSource(build.getParent());
        if (!(s instanceof GitLabSCMSource)) {
            return;
        }
        GitLabSCMSource source = (GitLabSCMSource) s;
        final GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(null, SCMHeadObserver.none())
                .withTraits((source.getTraits()));
        if (sourceContext
                .notificationsDisabled()) {
            return;
        }
        String url;
        try {
            url = DisplayURLProvider.get().getRunURL(build);
        } catch (IllegalStateException e) {
            listener.getLogger().println(
                    "Can not determine Jenkins root URL. Commit status notifications are disabled until a root URL is"
                            + " configured in Jenkins global configuration.");
            return;
        }
        Result result = build.getResult();
        CommitStatus status = new CommitStatus();
        Constants.CommitBuildState state;
        status.setTargetUrl(url);
        status.setName(build.getParent().getFullName());
        if (Result.SUCCESS.equals(result)) {
            status.setDescription("This commit looks good");
            status.setStatus("SUCCESS");
            state = Constants.CommitBuildState.SUCCESS;
        } else if (Result.UNSTABLE.equals(result)) {
            status.setDescription("This commit has test failures");
            status.setStatus("FAILED");
            state = Constants.CommitBuildState.FAILED;
        } else if (Result.FAILURE.equals(result)) {
            status.setDescription("There was a failure building this commit");
            status.setStatus("FAILED");
            state = Constants.CommitBuildState.FAILED;
        } else if (result != null) { // ABORTED etc.
            status.setDescription("Something is wrong with the build of this commit");
            status.setStatus("CANCELED");
            state = Constants.CommitBuildState.CANCELED;
        } else {
            status.setDescription("Build started...");
            status.setStatus("RUNNING");
            state = Constants.CommitBuildState.RUNNING;
        }

        SCMRevision revision = SCMRevisionAction.getRevision(source, build);
        String hash;
        if (revision instanceof BranchSCMRevision) {
            listener.getLogger().format("[GitLab Pipeline Status] Notifying branch build status: %s %s%n",
                    status.getStatus(), status.getDescription());
            hash = ((BranchSCMRevision) revision).getHash();
        } else if (revision instanceof MergeRequestSCMRevision) {
            listener.getLogger().format("[GitLab Pipeline Status] Notifying merge request build status: %s %s%n",
                    status.getStatus(), status.getDescription());
            hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
        } else {
            // TODO tags
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
            gitLabApi.getCommitsApi().addCommitStatus(
                    source.getProjectOwner()+'/'+source.getProject(),
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
                        if (revision instanceof BranchSCMRevision) {
                            LOGGER.log(Level.INFO, "Notifying branch pending build {0}", job.getFullName());
                            hash = ((BranchSCMRevision) revision).getHash();
                        } else if (revision instanceof MergeRequestSCMRevision) {
                            LOGGER.log(Level.INFO, "Notifying merge request pending build {0}", job.getFullName());
                            hash = ((MergeRequestSCMRevision) revision).getOrigin().getHash();
                        } else {
                            // TODO tags
                            return;
                        }
                        String url;
                        try {
                            url = DisplayURLProvider.get().getJobURL(job);
                        } catch (IllegalStateException e) {
                            // no root url defined, cannot notify, let's get out of here
                            return;
                        }
                        CommitStatus status = new CommitStatus();
                        status.setTargetUrl(url);
                        status.setName(job.getFullName());
                        status.setDescription("Build queued...");
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
                                    source.getProjectOwner()+'/'+source.getProject(),
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
                               SCMRevisionState pollingBaseline) throws Exception {
            try {
                sendNotifications(build, listener);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(listener.error("Could not send notifications"));
            }
        }
    }

    /**
     * Sends notifications to GitLab on Run completed.
     */
    @Extension
    public static class JobCompletedListener extends RunListener<Run<?, ?>> {

        @Override
        public void onCompleted(Run<?, ?> build, TaskListener listener) {
            try {
                sendNotifications(build, listener);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(listener.error("Could not send notifications"));
            }
        }

        @Override
        public void onStarted(Run<?, ?> run, TaskListener listener) {
            try {
                sendNotifications(run, listener);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(listener.error("Could not send notifications"));
            }
        }
    }
}
