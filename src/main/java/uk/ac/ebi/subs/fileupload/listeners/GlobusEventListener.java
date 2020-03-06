package uk.ac.ebi.subs.fileupload.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.config.MessagingConfiguration;
import uk.ac.ebi.subs.fileupload.model.globus.GlobusShareRequest;
import uk.ac.ebi.subs.fileupload.model.globus.GlobusUploadedFilesNotification;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusService;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;

@Service
@RequiredArgsConstructor
public class GlobusEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobusEventListener.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private GlobusService globusService;

    @RabbitListener(queues = MessagingConfiguration.GLOBUS_SHARE_REQUEST_QUEUE)
    public String onGlobusShareRequest(GlobusShareRequest shareRequest) {
        LOGGER.debug("Globus share request received : {}", shareRequest);

        try {
            return globusService.getShareLink(shareRequest.getOwner(), shareRequest.getSubmissionId());
        } catch (Exception ex) {
            LOGGER.error("Error creating share for request : {}", shareRequest, ex);
        }

        return null;
    }

    @RabbitListener(queues = MessagingConfiguration.GLOBUS_UPLOADED_FILES_NOTIFICATION_QUEUE)
    public void onUploadedFilesNotification(GlobusUploadedFilesNotification notification) {
        LOGGER.debug("Globus uploaded files notification received : {}", notification);

        try {
            globusService.processUploadedFiles(
                    notification.getOwner(), notification.getSubmissionId(), notification.getFiles());
        } catch (Exception ex) {
            LOGGER.error("Error processing uploaded files : {}", notification, ex);
        }
    }

    @RabbitListener(queues = MessagingConfiguration.GLOBUS_SUB_UNREGISTER_QUEUE)
    public void onGlobusSubmissionUnregister(SubmissionEnvelope subEnvelope) {
        String submissionId = subEnvelope.getSubmission().getId();

        Submission submission = submissionRepository.findOne(submissionId);

        LOGGER.debug("Globus submission unregister message received. Owner : {}, SubmissionID : {}",
                submissionId, submission.getCreatedBy());

        try {
            globusService.unregisterSubmission(submission.getCreatedBy(), submissionId);
        } catch (Exception ex) {
            LOGGER.error("Error unregistering submission. Owner : {}, SubmissionID : {}, ",
                    submission.getCreatedBy(), submissionId, ex);
        }
    }
}
