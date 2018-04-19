package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.repository.model.fileupload.File;

import java.util.UUID;

/**
 * This class is handling the 'post-create' hook event that is coming from the tusd server.
 * It is responsible to persist the information about the file to upload into the MongoDB database.
 */
@Component
public class PostCreateEvent implements TusEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostCreateEvent.class);

    public JWTExtractor setupJWTExtractor(String jwtToken) {
         return new JWTExtractor(jwtToken);
    }

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        file.setId(UUID.randomUUID().toString());
        file.setStatus(FileStatus.INITIALIZED);

        JWTExtractor jwtExtractor = setupJWTExtractor(tusFileInfo.getMetadata().getJwtToken());

        file.setCreatedBy(jwtExtractor.getUsername());

        LOGGER.debug(String.format("File object: %s", file));

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }
}
