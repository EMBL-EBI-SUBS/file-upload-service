package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;

/**
 * This class is handling the 'pre-create' hook event that is coming from the tusd server.
 * It is checking if the JWT token is valid and the submission is modifiable.
 */
public class PreCreateEvent implements TusEvent {

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {

        return eventHandlerService.validateUploadRequest(tusFileInfo);
    }
}
