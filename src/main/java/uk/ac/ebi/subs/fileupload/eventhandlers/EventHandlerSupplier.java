package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.subs.fileupload.util.TUSEventType;

import java.util.HashMap;
import java.util.Map;

/**
 * This class' role is to supply an event handler for handling the various hook events coming from the TUSD server.
 * It is done by applying the command pattern.
 */
@Configuration
public class EventHandlerSupplier {

    private PreCreateEvent preCreateEvent;
    private PostCreateEvent postCreateEvent;
    private PostReceiveEvent postReceiveEvent;
    private PostFinishEvent postFinishEvent;

    @Bean
    public Map<String, TusEvent> eventHandlers() {
        final Map<String, TusEvent> eventHandlers = new HashMap<>();
        eventHandlers.put(TUSEventType.PRE_CREATE.getEventType(), preCreateEvent);
        eventHandlers.put(TUSEventType.POST_CREATE.getEventType(), postCreateEvent);
        eventHandlers.put(TUSEventType.POST_RECEIVE.getEventType(), postReceiveEvent);
        eventHandlers.put(TUSEventType.POST_FINISH.getEventType(), postFinishEvent);

        return eventHandlers;
    }

    public EventHandlerSupplier(PreCreateEvent preCreateEvent, PostCreateEvent postCreateEvent, PostReceiveEvent postReceiveEvent, PostFinishEvent postFinishEvent) {
        this.preCreateEvent = preCreateEvent;
        this.postCreateEvent = postCreateEvent;
        this.postReceiveEvent = postReceiveEvent;
        this.postFinishEvent = postFinishEvent;
    }

    public TusEvent supplyEventHandler(String eventType) {
        TusEvent eventHandler = eventHandlers().get(eventType);

        if (eventHandler == null) {
            throw new IllegalArgumentException("Invalid event type: "
                    + eventType);
        }

        return eventHandler;
    }
}
