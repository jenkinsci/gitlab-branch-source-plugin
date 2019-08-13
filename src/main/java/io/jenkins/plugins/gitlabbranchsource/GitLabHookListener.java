package io.jenkins.plugins.gitlabbranchsource;


import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.security.ACL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jenkins.branch.BranchProperty;
import jenkins.branch.MultiBranchProject;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.SCMSourceOwners;
import org.gitlab4j.api.systemhooks.GroupSystemHookEvent;
import org.gitlab4j.api.systemhooks.ProjectSystemHookEvent;
import org.gitlab4j.api.systemhooks.SystemHookListener;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.NoteEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;

public class GitLabHookListener implements WebHookListener, SystemHookListener {

    public static final Logger LOGGER = Logger.getLogger(GitLabHookListener.class.getName());

    private String origin;

    public GitLabHookListener(String origin) {
        this.origin = origin;
    }

    @Override
    public void onNoteEvent(NoteEvent event) {
        LOGGER.info("NOTE EVENT");
        LOGGER.info(event.toString());
        if(event.getObjectAttributes().getNoteableType().equals(NoteEvent.NoteableType.MERGE_REQUEST)) {
            Integer mergeRequestId = event.getMergeRequest().getIid();
            final Pattern mergeRequestJobNamePattern = Pattern.compile("^MR-" + mergeRequestId + "\\b.*$",
                    Pattern.CASE_INSENSITIVE);
            final String commentBody = event.getObjectAttributes().getNote();
            final String commentUrl = event.getObjectAttributes().getUrl();
            ACL.impersonate(ACL.SYSTEM, new Runnable() {
                @Override
                public void run() {
                    boolean jobFound = false;
                    for (final SCMSourceOwner owner : SCMSourceOwners.all()) {
                        for (SCMSource source : owner.getSCMSources()) {
                            if (!(source instanceof GitLabSCMSource)) {
                                continue;
                            }
                            GitLabSCMSource gitLabSCMSource = (GitLabSCMSource) source;
                            if (gitLabSCMSource.getProjectId() == event.getMergeRequest().getTargetProjectId()) {
                                for (Job<?, ?> job : owner.getAllJobs()) {
                                    if (mergeRequestJobNamePattern.matcher(job.getName()).matches()) {
                                        boolean propFound = false;
                                        for (BranchProperty prop : ((MultiBranchProject) job.getParent()).getProjectFactory().getBranch(job).getProperties()) {
                                            if (!(prop instanceof TriggerMRCommentBranchProperty)) {
                                                continue;
                                            }
                                            propFound = true;
                                            String expectedCommentBody = ((TriggerMRCommentBranchProperty) prop).getCommentBody();
                                            Pattern pattern = Pattern.compile(expectedCommentBody,
                                                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                                            if (commentBody == null || pattern.matcher(commentBody).matches()) {
                                                ParameterizedJobMixIn.scheduleBuild2(job, 0,
                                                        new CauseAction(new GitLabMergeRequestCommentCause(commentUrl)));
                                                LOGGER.log(Level.FINE,
                                                        "Triggered build for {0} due to MR comment on {1}",
                                                        new Object[] {
                                                                job.getFullName(),
                                                                event.getProject().getPathWithNamespace()
                                                        }
                                                );
                                            } else {
                                                LOGGER.log(Level.FINER,
                                                        "MR comment does not match the trigger build string ({0}) for {1}",
                                                        new Object[] { expectedCommentBody, job.getFullName() }
                                                );
                                            }
                                            break;
                                        }

                                        if (!propFound) {
                                            LOGGER.log(Level.FINE,
                                                    "Job {0} for {1} does not have a trigger PR comment branch property",
                                                    new Object[] {
                                                            job.getFullName(),
                                                            event.getProject().getPathWithNamespace()
                                                    }
                                            );
                                        }
                                        jobFound = true;
                                    }
                                }
                            }
                        }
                    }
                    if (!jobFound) {
                        LOGGER.log(Level.FINE, "MR comment on {0} did not match any job",
                                new Object[] {
                                        event.getProject().getPathWithNamespace()
                                }
                        );
                    }
                }
            });
        }
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent event) {
        LOGGER.info("MR EVENT");
        LOGGER.info(event.toString());
        GitLabMergeRequestSCMEvent trigger = new GitLabMergeRequestSCMEvent(event, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.info("PUSH EVENT");
        LOGGER.info(pushEvent.toString());
        GitLabPushSCMEvent trigger = new GitLabPushSCMEvent(pushEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.info("TAG EVENT");
        LOGGER.info(tagPushEvent.toString());
        GitLabTagPushSCMEvent trigger = new GitLabTagPushSCMEvent(tagPushEvent, origin);
        SCMHeadEvent.fireNow(trigger);
    }

    @Override
    public void onProjectEvent(ProjectSystemHookEvent projectSystemHookEvent) {
        LOGGER.info("PROJECT EVENT");
        LOGGER.info(projectSystemHookEvent.toString());
        // TODO: implement handling `project_transfer` and `project_renamed`
        if(!projectSystemHookEvent.getEventName().equals("project_transfer") && !projectSystemHookEvent.getEventName().equals("project_renamed")) {
            GitLabProjectSCMEvent trigger = new GitLabProjectSCMEvent(projectSystemHookEvent, origin);
            SCMSourceEvent.fireNow(trigger);
        }
    }

    @Override
    public void onGroupEvent(GroupSystemHookEvent groupSystemHookEvent) {
        LOGGER.info("GROUP EVENT");
        LOGGER.info(groupSystemHookEvent.toString());
    }
}
