package uk.ac.ebi.subs.fileupload.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.ValidationService;
import uk.ac.ebi.subs.fileupload.util.TUSEvent;

/**
 * This is a REST controller that responsible for handling HTTP POST request events
 * come from the the tusd server, while a file is being upload.
 */
@RestController
public class TUSEventController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TUSEventController.class);

    private ValidationService validationService;

    public TUSEventController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @RequestMapping(value = "/tusevent", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Object> fileUploadEventPost(@RequestBody TUSFileInfo tusFileInfo,
                                                    @RequestHeader(value = "Hook-Name") String eventName) throws Exception {
        LOGGER.info(String.format("Name of the POST event: %s", eventName));
        LOGGER.info(tusFileInfo.toString());
        ResponseEntity<Object> response;

        String jwtToken = tusFileInfo.getMetadata().getJwtToken();
        String submissionId = tusFileInfo.getMetadata().getSubmissionID();

        // TODO: karoly - refactor it using the command pattern to use it with more event types
        boolean isValidRequest;
        if (eventName.equals(TUSEvent.PRE_CREATE.getEventType())) {
            isValidRequest = validationService.validateFileUploadRequest(jwtToken, submissionId);
            if (isValidRequest) {
                response = new ResponseEntity<>(HttpStatus.OK);
            } else {
                // make it an error object
                FileApiError fileApiError = new FileApiError(HttpStatus.NOT_ACCEPTABLE, ErrorMessages.INVALID_PARAMETERS);
                response = new ResponseEntity<>(fileApiError, HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            FileApiError fileApiError = new FileApiError(HttpStatus.NOT_ACCEPTABLE, ErrorMessages.NOT_SUPPORTED_EVENT);
            response = new ResponseEntity<>(fileApiError, HttpStatus.NOT_ACCEPTABLE);
        }

        return response;
    }
}
