package uk.ac.ebi.subs.fileupload.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.config.MessagingConfiguration;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusService;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;

@Service
@RequiredArgsConstructor
public class GlobusEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobusEventListener.class);

    @Autowired
    private GlobusService globusService;

    @RabbitListener(queues = MessagingConfiguration.USI_FU_GLOBUS_SHARE_REQUEST)
    public String onGlobusShareRequest(String submitterEmail) {
        LOGGER.debug("Globus share requested for {}", submitterEmail);

        try {
            return globusService.getShareLink(submitterEmail);
        } catch (Exception ex) {
            LOGGER.error("Error getting share for requested submitter : {}", submitterEmail, ex);
        }

        return null;
    }

    @RabbitListener(queues = MessagingConfiguration.FU_GLOBUS_SUB_UNREGISTER)
    public void onGlobusSubmissionUnregister(SubmissionEnvelope subEnvelope) {
        String submitterEmail = subEnvelope.getSubmission().getSubmitter().getEmail();
        String submissionId = subEnvelope.getSubmission().getId();

        LOGGER.debug("Globus submission unregister message received. SubmissionID : {}, SubmitterEmail : {}",
                submitterEmail, submissionId);

        try {
            globusService.unregisterSubmission(submitterEmail, submissionId);
        } catch (Exception ex) {
            LOGGER.error("Error unregistering submission. SubmitterEmail : {}, SubmissionID : {}",
                    submitterEmail, submissionId, ex);
        }
    }
}
