package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSource;
import org.gitlab4j.api.webhook.ChangeContainer;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.MergeRequestChanges;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.junit.Test;

public class GitLabMergeRequestSCMEventTest {

    private MergeRequestEvent mrEvent(String state, String action) {
        MergeRequestEvent.ObjectAttributes attributes = new MergeRequestEvent.ObjectAttributes();
        attributes.setIid(123454L);
        attributes.setState(state);
        attributes.setAction(action);

        EventProject project = new EventProject();
        project.setName("bar");
        project.setPathWithNamespace("foo/bar");

        MergeRequestEvent event = new MergeRequestEvent();
        event.setObjectAttributes(attributes);
        event.setProject(project);
        return event;
    }

    private <T> ChangeContainer<T> change(T previous, T current) {
        ChangeContainer<T> container = new ChangeContainer<>();
        container.setPrevious(previous);
        container.setCurrent(current);
        return container;
    }

    private ChangeContainer<Object> changeObj(Object previous, Object current) {
        ChangeContainer<Object> container = new ChangeContainer<>();
        container.setPrevious(previous);
        container.setCurrent(current);
        return container;
    }

    private GitLabMergeRequestSCMEvent event(MergeRequestEvent mrEvent) {
        return new GitLabMergeRequestSCMEvent(mrEvent, "test");
    }

    @Test
    public void describesSingleFieldChange() {
        MergeRequestEvent mrEvent = mrEvent("opened", "update");
        MergeRequestChanges changes = new MergeRequestChanges();
        changes.setTitle(change("old title", "new title"));
        mrEvent.setChanges(changes);

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 title changed in project foo/bar", event.description());
        assertEquals(
                "Merge request !123454 title changed in project bar", event.descriptionFor(mock(SCMNavigator.class)));
        assertEquals("Merge request !123454 title changed", event.descriptionFor(mock(SCMSource.class)));
    }

    @Test
    public void describesMultipleFieldChanges() {
        MergeRequestEvent mrEvent = mrEvent("opened", "update");
        MergeRequestChanges changes = new MergeRequestChanges();
        changes.setTitle(change("old title", "new title"));
        changes.setLabels(change(null, null));
        mrEvent.setChanges(changes);

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 title, labels changed in project foo/bar", event.description());
    }

    @Test
    public void describesPush() {
        MergeRequestEvent mrEvent = mrEvent("opened", "update");
        mrEvent.getObjectAttributes().setOldrev("abc123");

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 push in project foo/bar", event.description());
    }

    @Test
    public void describesPushAndFieldChanges() {
        MergeRequestEvent mrEvent = mrEvent("opened", "update");
        mrEvent.getObjectAttributes().setOldrev("abc123");
        MergeRequestChanges changes = new MergeRequestChanges();
        changes.setTitle(change("old title", "new title"));
        mrEvent.setChanges(changes);

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 push, title changed in project foo/bar", event.description());
    }

    @Test
    public void describesTargetBranchChange() {
        MergeRequestEvent mrEvent = mrEvent("opened", "update");
        MergeRequestChanges changes = new MergeRequestChanges();
        changes.set("target_branch", changeObj("main", "develop"));
        mrEvent.setChanges(changes);

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 target branch changed in project foo/bar", event.description());
    }

    @Test
    public void describesDraftStatusChange() {
        MergeRequestEvent mrEvent = mrEvent("opened", "update");
        MergeRequestChanges changes = new MergeRequestChanges();
        changes.set("draft", changeObj(true, false));
        mrEvent.setChanges(changes);

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 draft status changed in project foo/bar", event.description());
    }

    @Test
    public void fallsBackToRawActionForApprovalEvents() {
        MergeRequestEvent approval = mrEvent("opened", "approval");
        assertEquals(
                "Merge request !123454 approval in project foo/bar",
                event(approval).description());

        MergeRequestEvent unapproval = mrEvent("opened", "unapproval");
        assertEquals(
                "Merge request !123454 unapproval in project foo/bar",
                event(unapproval).description());
    }

    @Test
    public void fallsBackToActionWhenNothingChanged() {
        MergeRequestEvent mrEvent = mrEvent("opened", "approved");

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 approved in project foo/bar", event.description());
    }

    @Test
    public void fallsBackToActionWhenStateMissing() {
        MergeRequestEvent mrEvent = mrEvent(null, "update");

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 update in project foo/bar", event.description());
        assertEquals("Merge request !123454 update in project bar", event.descriptionFor(mock(SCMNavigator.class)));
        assertEquals("Merge request !123454 update", event.descriptionFor(mock(SCMSource.class)));
    }

    @Test
    public void fallsBackToUpdatedWhenNothingIsKnown() {
        MergeRequestEvent mrEvent = mrEvent(null, null);

        GitLabMergeRequestSCMEvent event = event(mrEvent);
        assertEquals("Merge request !123454 updated in project foo/bar", event.description());
    }
}
