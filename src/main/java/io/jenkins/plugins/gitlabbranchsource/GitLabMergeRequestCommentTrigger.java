package io.jenkins.plugins.gitlabbranchsource;

import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.SCMSourceOwners;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.webhook.NoteEvent;

public class GitLabMergeRequestCommentTrigger extends AbstractGitLabJobTrigger<NoteEvent> {

    public static final Logger LOGGER = Logger
        .getLogger(GitLabMergeRequestCommentTrigger.class.getName());

    public GitLabMergeRequestCommentTrigger(NoteEvent payload) {
        super(payload);
    }

    @Override
    public void isMatch() {
        if (getPayload().getObjectAttributes().getNoteableType()
            .equals(NoteEvent.NoteableType.MERGE_REQUEST)) {
            Integer mergeRequestId = getPayload().getMergeRequest().getIid();
            final Pattern mergeRequestJobNamePattern = Pattern
                .compile("^MR-" + mergeRequestId + "\\b.*$",
                    Pattern.CASE_INSENSITIVE);
            final String commentBody = getPayload().getObjectAttributes().getNote();
            final String commentUrl = getPayload().getObjectAttributes().getUrl();
            try (ACLContext ctx = ACL.as(ACL.SYSTEM)) {
                boolean jobFound = false;
                for (final SCMSourceOwner owner : SCMSourceOwners.all()) {
                    LOGGER.log(Level.FINEST, String.format("Source Owner: %s", owner.getFullDisplayName()));
                    // This is a hack to skip owners which are children of a SCMNavigator
                    if (owner.getFullDisplayName().contains(" » ")) {
                        continue;
                    }
                    for (SCMSource source : owner.getSCMSources()) {
                        if (!(source instanceof GitLabSCMSource)) {
                            continue;
                        }
                        GitLabSCMSource gitLabSCMSource = (GitLabSCMSource) source;
                        final GitLabSCMSourceContext sourceContext = new GitLabSCMSourceContext(
                            null, SCMHeadObserver.none())
                            .withTraits(gitLabSCMSource.getTraits());
                        if (!sourceContext.mrCommentTriggerEnabled()) {
                            return;
                        }
                        if (gitLabSCMSource.getProjectId() == getPayload().getMergeRequest()
                            .getTargetProjectId() && isTrustedMember(gitLabSCMSource)) {
                            for (Job<?, ?> job : owner.getAllJobs()) {
                                if (mergeRequestJobNamePattern.matcher(job.getName()).matches()) {
                                    String expectedCommentBody = sourceContext.getCommentBody();
                                    Pattern pattern = Pattern.compile(expectedCommentBody,
                                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                                    if (commentBody == null || pattern.matcher(commentBody)
                                        .matches()) {
                                        ParameterizedJobMixIn.scheduleBuild2(job, 0,
                                            new CauseAction(
                                                new GitLabMergeRequestCommentCause(commentUrl)));
                                        LOGGER.log(Level.INFO,
                                            "Triggered build for {0} due to MR comment on {1}",
                                            new Object[]{
                                                job.getFullName(),
                                                getPayload().getProject().getPathWithNamespace()
                                            }
                                        );
                                    } else {
                                        LOGGER.log(Level.INFO,
                                            "MR comment does not match the trigger build string ({0}) for {1}",
                                            new Object[]{expectedCommentBody, job.getFullName()}
                                        );
                                    }
                                    break;
                                }
                                jobFound = true;
                            }
                        }
                    }
                }
                if (!jobFound) {
                    LOGGER.log(Level.INFO, "MR comment on {0} did not match any job",
                        new Object[]{
                            getPayload().getProject().getPathWithNamespace()
                        }
                    );
                }
            }
        }
    }

    private boolean isTrustedMember(GitLabSCMSource gitLabSCMSource) {
        AccessLevel permission = gitLabSCMSource.getMembers()
            .get(getPayload().getUser().getUsername());
        if (permission != null) {
            switch (permission) {
                case MAINTAINER:
                case DEVELOPER:
                case OWNER:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }
}
