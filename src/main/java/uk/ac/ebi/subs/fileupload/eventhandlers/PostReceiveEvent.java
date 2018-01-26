package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

/**
 * This class is handling the 'post-receive' hook event that is coming from the tusd server.
 * It is responsible to update the relevant existed file document in the MongoDB database.
 */
public class PostReceiveEvent implements TusEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostReceiveEvent.class);

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);

        file.setStatus(FileStatus.UPLOADING);

        LOGGER.info(String.format("File object: %s", file));

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }
}
