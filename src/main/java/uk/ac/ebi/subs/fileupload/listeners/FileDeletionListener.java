package uk.ac.ebi.subs.fileupload.listeners;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.config.MessagingConfiguration;
import uk.ac.ebi.subs.fileupload.errors.FileNotFoundException;
import uk.ac.ebi.subs.fileupload.model.FileDeleteMessage;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.fileupload.File;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileDeletionListener {

    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionListener.class);

    private static final String EVENT_ASSAYDATA_FILEREF_VALIDATION_BY_FILE_DELETION = "file.deleted";
    private static final String SUBMISSION_ID_CANT_BE_NULL = "Submission ID can not be null";

    @RabbitListener(queues = MessagingConfiguration.USI_FILE_DELETION_QUEUE)
    public void deleteFileFromStorage(FileDeleteMessage fileDeleteMessage) throws IOException {
        String filePathForDeletion = fileDeleteMessage.getTargetFilePath();

        String commandForFileDeletion = "rm " + filePathForDeletion;

        int exitValue = 0;
        try {
            LOGGER.info(
                    "Deleting file: {} from the staging area", filePathForDeletion);
            java.lang.Runtime rt = java.lang.Runtime.getRuntime();
            Process process = rt.exec(commandForFileDeletion);

            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            handleFileDeletionFailure(filePathForDeletion);
        }

        if (exitValue != 0) {
            handleFileDeletionFailure(filePathForDeletion);
        }

        notifyFileReferenceValidatorOfFileDeletion(fileDeleteMessage.getSubmissionId());
    }

    private void handleFileDeletionFailure(String filePathForDeletion) {
        throw new RuntimeException(
                String.format("The file deletion command went wrong with file: %s.", filePathForDeletion));
    }

    private void notifyFileReferenceValidatorOfFileDeletion(String submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException(SUBMISSION_ID_CANT_BE_NULL);
        }

        ValidationMessageByFileDeletion validationMessageByFileDeletion = new ValidationMessageByFileDeletion();
        validationMessageByFileDeletion.setSubmissionId(submissionId);

        LOGGER.debug("Sending assay data to file reference validation queue");
        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, EVENT_ASSAYDATA_FILEREF_VALIDATION_BY_FILE_DELETION, validationMessageByFileDeletion);
    }
}
