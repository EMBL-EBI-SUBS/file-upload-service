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
import uk.ac.ebi.subs.messaging.Exchanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class FileDeletionListener {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionListener.class);

    private static final String EVENT_ASSAYDATA_FILEREF_VALIDATION_BY_FILE_DELETION = "file.deleted";
    private static final String SUBMISSION_ID_CANT_BE_NULL = "Submission ID can not be null";

    @RabbitListener(queues = MessagingConfiguration.USI_FILE_DELETION_QUEUE)
    public void deleteFileFromStorage(FileDeleteMessage fileDeleteMessage) {
        String filePathForDeletion = fileDeleteMessage.getTargetFilePath();

        try {
            Files.deleteIfExists(Paths.get(filePathForDeletion));
            notifyFileReferenceValidatorOfFileDeletion(fileDeleteMessage.getSubmissionId());
        } catch (IOException e) {
            throw new FileDeletionException(filePathForDeletion);
        }
    }

    private void notifyFileReferenceValidatorOfFileDeletion(String submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException(SUBMISSION_ID_CANT_BE_NULL);
        }

        FileDeletedMessage fileDeletedMessage = new FileDeletedMessage();
        fileDeletedMessage.setSubmissionId(submissionId);

        LOGGER.debug("Sending assay data to file reference validation queue");
        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, EVENT_ASSAYDATA_FILEREF_VALIDATION_BY_FILE_DELETION, fileDeletedMessage);
    }
}
