package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.ValidationService;

/**
 * This class is handling the 'pre-create' hook event that is coming from the tusd server.
 * It is checking if the JWT token is valid and the submission is modifiable.
 */
public class PreCreateEvent implements TusEvent {

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, ValidationService validationService) {
        ResponseEntity<Object> response;

        String jwtToken = tusFileInfo.getMetadata().getJwtToken();
        String submissionId = tusFileInfo.getMetadata().getSubmissionID();

        boolean isValidRequest = validationService.validateFileUploadRequest(jwtToken, submissionId);
        if (isValidRequest) {
            response = new ResponseEntity<>(HttpStatus.OK);
        } else {
            // make it an error object
            FileApiError fileApiError = new FileApiError(HttpStatus.NOT_ACCEPTABLE, ErrorMessages.INVALID_PARAMETERS);
            response = new ResponseEntity<>(fileApiError, HttpStatus.NOT_ACCEPTABLE);
        }

        return response;
    }
}
