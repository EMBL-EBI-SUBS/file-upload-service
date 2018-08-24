package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;

public class PostTerminateEvent implements TusEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostTerminateEvent.class);

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        final String tusId = tusFileInfo.getTusId();
        if (!eventHandlerService.isFileExists(tusId)) {
            LOGGER.debug("File with name: {} and generated TUS ID: is not exists in the database.", tusFileInfo.getMetadata().getFilename(), tusFileInfo.getTusId());
            return ErrorResponse.assemble(
                    HttpStatus.NOT_FOUND, String.format(ErrorMessages.FILE_DOCUMENT_NOT_FOUND, tusId));
        }

        eventHandlerService.deleteFile(tusId);

        return ResponseEntity.accepted().build();
    }
}
