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
    public boolean validateFileUploadRequest(String jwtToken, String submissionUuid) {

        boolean isValidToken = tokenHandlerService.validateToken(jwtToken);

        JWTExtractor jwtExtractor = new JWTExtractor(jwtToken);

        boolean isUserAllowedToModifyGivenSubmission =
                submissionService.isUserAllowedToModifyGivenSubmission(submissionUuid, jwtExtractor.getUserDomains(), jwtToken);
        boolean isSubmissionModifiable = submissionService.isModifiable(submissionUuid, jwtToken);

        return isValidToken && isUserAllowedToModifyGivenSubmission && isSubmissionModifiable;
    }
}
