package uk.ac.ebi.subs.fileupload.util;

public enum TUSEvent {

    PRE_CREATE("pre-create"), POST_CREATE("post-create"), POST_RECEIVE("post-receive"),
    POST_FINISH("post-finish"), POST_TERMINATE("post-terminate");

    String eventType;

    TUSEvent(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return this.eventType;
    }
}
