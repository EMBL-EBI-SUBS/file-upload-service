package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;

/**
 * This class is handling the 'post-create' hook event that is coming from the tusd server.
 * It is responsible to persist the information about the file to upload into the MongoDB database.
 */
public class PostCreateEvent implements TusEvent {

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {

        return eventHandlerService.persistFileInformation(tusFileInfo);
    }
}
