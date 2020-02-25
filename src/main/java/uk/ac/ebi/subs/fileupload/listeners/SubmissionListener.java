package uk.ac.ebi.subs.fileupload.listeners;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionListener.class);

    private RabbitMessagingTemplate rabbitMessagingTemplate;

//    @RabbitListener(queues = MessagingConfiguration.SUBMISSION_SUBMITTED_QUEUE)
//    public void onSubmissionSubmitted(SubmissionEnvelope submissionEnvelope) {
//        LOGGER.debug("Submission submitted : {}", submissionEnvelope.getSubmission().getId());
//
//        //todo
//    }
}
