package uk.ac.ebi.subs.fileupload.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.config.MessagingConfiguration;
import uk.ac.ebi.subs.fileupload.model.FileDeleteMessage;

import java.io.IOException;

@Service
public class FileDeletionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionListener.class);

    @RabbitListener(queues = MessagingConfiguration.USI_FILE_DELETION_QUEUE)
    public void deleteFileFromStorage(FileDeleteMessage fileDeleteMessage) throws IOException {
        String filePathForDeletion = fileDeleteMessage.getTargetFilePath();

        String commandForFileDeletion = "rm " + filePathForDeletion;

        LOGGER.info(
                "Deleting file: {} from the staging area", filePathForDeletion);
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        rt.exec(commandForFileDeletion);
    }
}
