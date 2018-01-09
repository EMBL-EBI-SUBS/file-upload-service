package uk.ac.ebi.subs.fileupload.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;

@Service
public class DefaultEventHandlerService implements EventHandlerService {

    private ValidationService validationService;
    private FileRepository fileRepository;

    public DefaultEventHandlerService(ValidationService validationService, FileRepository fileRepository) {
        this.validationService = validationService;
        this.fileRepository = fileRepository;
    }

    @Override
    public ResponseEntity<Object> validateUploadRequest(TUSFileInfo tusFileInfo) {
        ResponseEntity<Object> response;

        String jwtToken = tusFileInfo.getMetadata().getJwtToken();
        String submissionId = tusFileInfo.getMetadata().getSubmissionID();

        boolean isValidRequest = validationService.validateFileUploadRequest(jwtToken, submissionId);
        if (isValidRequest) {
            response = new ResponseEntity<>(HttpStatus.OK);
        } else {
            // make it an error object
            FileApiError fileApiError = new FileApiError(HttpStatus.NOT_ACCEPTABLE, ErrorMessages.INVALID_PARAMETERS);
            response = new ResponseEntity<>(fileApiError, HttpStatus.NOT_ACCEPTABLE);
        }

        return response;
    }

    @Override
    public ResponseEntity<Object> persistFileInformation(TUSFileInfo tusFileInfo) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        fileRepository.insert(file);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
