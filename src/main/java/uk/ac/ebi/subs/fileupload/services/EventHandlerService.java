package uk.ac.ebi.subs.fileupload.services;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;

/**
 * This class is responsible for handling the various events published by the tusd server.
 */
public interface EventHandlerService {

    ResponseEntity<Object> validateUploadRequest(TUSFileInfo tusFileInfo);

    ResponseEntity<Object> persistFileInformation(TUSFileInfo tusFileInfo);
}
