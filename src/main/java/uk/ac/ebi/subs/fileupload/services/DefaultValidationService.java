package uk.ac.ebi.subs.fileupload.services;

import org.springframework.stereotype.Service;

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
        boolean isSubmissionModifiable = submissionService.isModifiable(submissionUuid);

        return isValidToken && isSubmissionModifiable;
    }


}
