package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

import java.io.IOException;

/**
 * This class is handling the 'post-finish' hook event that is coming from the tusd server.
 * It is responsible to update the relevant existed file document in the MongoDB database.
 */
public class PostFinishEvent implements TusEvent {

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        file.setStatus(FileStatus.UPLOADED);

        ResponseEntity<Object> response =  eventHandlerService.persistOrUpdateFileInformation(file);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            response = moveFile(file, eventHandlerService);
        }

        return response;
    }

    ResponseEntity<Object> moveFile(File file, EventHandlerService eventHandlerService) {
        ResponseEntity<Object> response;
        try {
            eventHandlerService.moveFile(file);
            file.setStatus(FileStatus.READY_TO_ARCHIVE);
            response =  eventHandlerService.persistOrUpdateFileInformation(file);
        } catch (IOException e) {
            FileApiError fileApiError = new FileApiError(HttpStatus.ACCEPTED, ErrorMessages.FILE_IO_ERROR);
            response = new ResponseEntity<>(fileApiError, HttpStatus.ACCEPTED);
        }

        return response;
    }
}
