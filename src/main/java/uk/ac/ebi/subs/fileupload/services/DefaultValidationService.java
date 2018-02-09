package uk.ac.ebi.subs.fileupload.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.util.JWTExtractor;

@Service
public class DefaultValidationService implements ValidationService {

    private TokenHandlerService tokenHandlerService;
    private SubmissionService submissionService;

    public DefaultValidationService(TokenHandlerService tokenHandlerService, SubmissionService submissionService) {
        this.tokenHandlerService = tokenHandlerService;
        this.submissionService = submissionService;
    }

    @Override
    public boolean validateFileUploadRequest(String jwtToken, String submissionUuid) {

        boolean isValidToken = tokenHandlerService.validateToken(jwtToken);

        JWTExtractor jwtExtractor = new JWTExtractor(jwtToken);

        boolean isUserAllowedToModifyGivenSubmission =
                submissionService.isUserAllowedToModifyGivenSubmission(submissionUuid, jwtExtractor.getUserDomains(), jwtToken);
        boolean isSubmissionModifiable = submissionService.isModifiable(submissionUuid, jwtToken);

        return isValidToken && isUserAllowedToModifyGivenSubmission && isSubmissionModifiable;
    }

    @Override
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
}
