package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;

/**
 * This class is handling the 'pre-create' hook event that is coming from the tusd server.
 * It is checking if the JWT token is valid and the submission is modifiable.
 */
@Component
public class PreCreateEvent implements TusEvent {

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {

        ResponseEntity<Object> response = eventHandlerService.validateUploadRequest(tusFileInfo);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            String filename = tusFileInfo.getMetadata().getFilename();
            String submissionId = tusFileInfo.getMetadata().getSubmissionID();

            if (eventHandlerService.isFileDuplicated(filename, submissionId)) {
                response = ErrorResponse.assemble(HttpStatus.CONFLICT,
                        String.format(ErrorMessages.DUPLICATED_FILE_ERROR, filename, submissionId));
            }
        }

        return response;
    }
}
