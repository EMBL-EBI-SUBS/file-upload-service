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
import uk.ac.ebi.subs.fileupload.eventhandlers.EventHandlerSupplier;
import uk.ac.ebi.subs.fileupload.eventhandlers.TusEvent;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;

/**
 * This is a REST controller that responsible for handling HTTP POST request events
 * come from the the tusd server, while a file is being upload.
 */
@RestController
public class TUSEventController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TUSEventController.class);

    private EventHandlerService eventHandlerService;

    public TUSEventController(EventHandlerService eventHandlerService) {
        this.eventHandlerService = eventHandlerService;
    }

    @RequestMapping(value = "/tusevent", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Object> fileUploadEventPost(@RequestBody TUSFileInfo tusFileInfo,
                                                    @RequestHeader(value = "Hook-Name") String eventName) throws Exception {
        LOGGER.info(String.format("Name of the POST event: %s", eventName));
        LOGGER.info(tusFileInfo.toString());
        ResponseEntity<Object> response;

        EventHandlerSupplier eventHandlerSupplier = new EventHandlerSupplier();
        TusEvent tusEvent;
        try {
            tusEvent = eventHandlerSupplier.supplyEventHandler(eventName);
            response = tusEvent.handle(tusFileInfo, eventHandlerService);
        } catch (IllegalArgumentException ex) {
            FileApiError fileApiError = new FileApiError(HttpStatus.CONFLICT, ErrorMessages.NOT_SUPPORTED_EVENT);
            response = new ResponseEntity<>(fileApiError, HttpStatus.CONFLICT);
        }

        return response;
    }
}
