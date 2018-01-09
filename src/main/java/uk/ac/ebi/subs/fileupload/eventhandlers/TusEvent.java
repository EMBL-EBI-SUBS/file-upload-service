package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;

/**
 * Interface used by the {@code EventHandlerSupplier} to instantiate the proper handler
 * for the tusd server's hook event.
 */
public interface TusEvent {

    ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService);
}
