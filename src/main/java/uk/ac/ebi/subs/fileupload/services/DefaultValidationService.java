package uk.ac.ebi.subs.fileupload.services;

import org.springframework.stereotype.Service;
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
    public boolean validateFileUploadRequest(String jwt, String submissionUuid) {

        boolean isValidToken = tokenHandlerService.validateToken(jwt);

        JWTExtractor jwtExtractor = new JWTExtractor(jwt);

        boolean isUserAllowedToModifyGivenSubmission =
                submissionService.isUserAllowedToModifyGivenSubmission(submissionUuid, jwtExtractor.getUserDomains());
        boolean isSubmissionModifiable = submissionService.isModifiable(submissionUuid);

        return isValidToken && isUserAllowedToModifyGivenSubmission && isSubmissionModifiable;
    }
}
