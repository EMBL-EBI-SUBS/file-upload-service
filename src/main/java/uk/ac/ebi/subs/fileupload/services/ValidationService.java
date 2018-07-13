package uk.ac.ebi.subs.fileupload.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.validator.data.FileUploadValidationEnvelopeToCoordinator;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.util.UUID;

/**
 * This class is responsible for validating the requests sent by the tusd server.
 */
@Service
@RequiredArgsConstructor
public class ValidationService {

    @NonNull
    private SubmissionService submissionService;
    @NonNull
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @NonNull
    private ValidationResultRepository validationResultRepository;
    @NonNull
    private FileRepository fileRepository;

    private static final String FILE_REF_VALIDATION_ROUTING_KEY = "usi.file.created";

    /**
     * Check the validity of the file upload request.
     * First validate if the JWT token is valid, then check if the given submission exists and modifiable.
     *
     * @param jwtToken the JWT security token from the original TUS client's file upload request
     * @param submissionUuid the UUID of the submission the file would belong to
     * @return response entity with OK HTTP status if everything is fine, otherwise other HTTP error status
     */
    public ResponseEntity<Object> validateFileUploadRequest(String jwtToken, String submissionUuid) {

        boolean isSubmissionModifiable = submissionService.isModifiable(submissionUuid, jwtToken);

        if (!isSubmissionModifiable) {
            return ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format(ErrorMessages.SUBMISSION_NOT_MODIFIABLE, submissionUuid));
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<Object> validateMetadata(TUSFileInfo.MetaData fileMetadata) {
        String jwtToken = fileMetadata.getJwtToken();
        String submissionId = fileMetadata.getSubmissionID();
        String filename = fileMetadata.getFilename();

        ResponseEntity<Object> response = new ResponseEntity<>(HttpStatus.OK);

        if (StringUtils.isEmpty(jwtToken)) {
            return ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY, ErrorMessages.JWT_TOKEN_MANDATORY);
        }

        if (StringUtils.isEmpty(submissionId)) {
            return ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY, ErrorMessages.SUBMISSION_ID_MANDATORY);
        }

        if (StringUtils.isEmpty(filename)) {
            return ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY, ErrorMessages.FILENAME_MANDATORY);
        }

        return response;
    }

    public void validateFileReference(String tusId) {
        File persistedFile = fileRepository.findByGeneratedTusId(tusId);
        createValidationResult(persistedFile);
        sendFileReferenceValidationEvent(persistedFile);
    }

    private void createValidationResult(File file) {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setEntityUuid(file.getId());
        validationResult.setUuid(UUID.randomUUID().toString());

        validationResult.setSubmissionId(file.getSubmissionId());
        validationResultRepository.save(validationResult);

        String tusId = file.getGeneratedTusId();
        File persistedFile = fileRepository.findByGeneratedTusId(tusId);

        persistedFile.setValidationResult(validationResult);

        fileRepository.save(persistedFile);
    }

    private void sendFileReferenceValidationEvent(uk.ac.ebi.subs.data.fileupload.File file) {
        FileUploadValidationEnvelopeToCoordinator validationEnvelope =
                new FileUploadValidationEnvelopeToCoordinator(file.getSubmissionId(), file);

        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, FILE_REF_VALIDATION_ROUTING_KEY, validationEnvelope);
    }
}
