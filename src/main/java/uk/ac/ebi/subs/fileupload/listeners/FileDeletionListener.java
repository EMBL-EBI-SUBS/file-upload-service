package uk.ac.ebi.subs.fileupload.listeners;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.config.MessagingConfiguration;
import uk.ac.ebi.subs.fileupload.errors.FileDeletionException;
import uk.ac.ebi.subs.fileupload.model.FileDeleteMessage;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.messaging.Exchanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class FileDeletionListener {

    @NonNull
    private EventHandlerService eventHandlerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionListener.class);

    @RabbitListener(queues = MessagingConfiguration.USI_FILE_DELETION_QUEUE)
    public void deleteFileFromStorage(FileDeleteMessage fileDeleteMessage) {
        String filePathForDeletion = fileDeleteMessage.getTargetFilePath();

        LOGGER.debug("Delete file: {} from the storage.", filePathForDeletion);

        eventHandlerService.deleteFileFromStorage(filePathForDeletion, fileDeleteMessage.getSubmissionId());
    }
}
