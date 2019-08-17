package io.jenkins.plugins.gitlabbranchsource;

public abstract class AbstractGitLabJobTrigger<E> {

    private final E payload;

    protected AbstractGitLabJobTrigger(E payload) {
        this.payload = payload;
    }

    public E getPayload() {
        return this.payload;
    }

    public abstract void isMatch();

    public static void fireNow(AbstractGitLabJobTrigger trigger) {
        trigger.isMatch();
    }
}
