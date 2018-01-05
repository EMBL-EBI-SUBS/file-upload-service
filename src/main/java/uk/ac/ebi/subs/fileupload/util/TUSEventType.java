package uk.ac.ebi.subs.fileupload.util;

/**
 * Enumerated list of hook event types coming from the tusd server.
 */
public enum TUSEventType {

    PRE_CREATE("pre-create"), POST_CREATE("post-create"), POST_RECEIVE("post-receive"),
    POST_FINISH("post-finish"), POST_TERMINATE("post-terminate");

    String eventType;

    TUSEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return this.eventType;
    }
}
