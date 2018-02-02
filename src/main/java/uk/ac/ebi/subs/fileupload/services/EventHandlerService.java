package uk.ac.ebi.subs.fileupload.services;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;

/**
 * This class is responsible for handling the various events published by the tusd server.
 */
public interface EventHandlerService {

    ResponseEntity<Object> validateUploadRequest(TUSFileInfo tusFileInfo);

    ResponseEntity<Object> persistOrUpdateFileInformation(File file);

    boolean isFileDuplicated(String fileName, String submissionUUID);
}
