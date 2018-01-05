package uk.ac.ebi.subs.fileupload.eventhandlers;

import uk.ac.ebi.subs.fileupload.util.TUSEventType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This class' role is to supply an event handler for handling the various hook events coming from the TUSD server.
 * It is done by applying the command pattern.
 */
public class EventHandlerSupplier {

    private static final Map<String, Supplier<TusEvent>> EVENT_HANDLER_SUPPLIER;

    static {
        final Map<String, Supplier<TusEvent>> eventhandlers = new HashMap<>();
        eventhandlers.put(TUSEventType.PRE_CREATE.getEventType(), PreCreateEvent::new);

        EVENT_HANDLER_SUPPLIER = Collections.unmodifiableMap(eventhandlers);
    }

    public TusEvent supplyEventHandler(String eventType) {
        Supplier<TusEvent> eventHandler = EVENT_HANDLER_SUPPLIER.get(eventType);

        if (eventHandler == null) {
            throw new IllegalArgumentException("Invalid event type: "
                    + eventType);
        }

        return eventHandler.get();
    }
}
