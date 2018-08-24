package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.repository.model.fileupload.File;

public class PostTerminateEvent implements TusEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostTerminateEvent.class);

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        final String tusID = tusFileInfo.getTusId();
        if (!eventHandlerService.isFileExists(tusID)) {
            LOGGER.debug("File with name: {} and generated TUS ID: is not exists in the database.", tusFileInfo.getMetadata().getFilename(), tusFileInfo.getTusId());
            return ErrorResponse.assemble(
                    HttpStatus.NOT_FOUND, String.format(ErrorMessages.FILE_DOCUMENT_NOT_FOUND, tusID));
        }

        File fileToDelete = eventHandlerService.getFileByTusID(tusID);

        eventHandlerService.deleteFileFromStorage(fileToDelete.getTargetPath(), fileToDelete.getSubmissionId());

        eventHandlerService.deleteFileFromDB(tusID);

        return ResponseEntity.accepted().build();
    }
}
