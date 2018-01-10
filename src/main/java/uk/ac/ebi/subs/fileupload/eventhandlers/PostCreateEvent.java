package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

/**
 * This class is handling the 'post-create' hook event that is coming from the tusd server.
 * It is responsible to persist the information about the file to upload into the MongoDB database.
 */
public class PostCreateEvent implements TusEvent {

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        file.setStatus(FileStatus.INITIALIZED);

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }
}
