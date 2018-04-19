package uk.ac.ebi.subs.fileupload.services;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.repository.model.fileupload.File;

/**
 * This class is responsible for validating the requests sent by the tusd server.
 */
public interface ValidationService {

    /**
     * Check the validity of the file upload request.
     * First validate if the JWT token is valid, then check if the given submission exists and modifiable.
     *
     * @param jwt the JWT security token from the original TUS client's file upload request
     * @param submissionUuid the UUID of the submission the file would belong to
     * @return response entity with OK HTTP status if everything is fine, otherwise other HTTP error status
     */
    ResponseEntity<Object> validateFileUploadRequest(String jwt, String submissionUuid);

    ResponseEntity<Object> validateMetadata(TUSFileInfo.MetaData fileMetadata);

    void validateFileReference(File file);
}
