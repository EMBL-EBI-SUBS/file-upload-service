package uk.ac.ebi.subs.fileupload.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

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

        TUSFileInfo.MetaData fileMetadata = tusFileInfo.getMetadata();

        String jwtToken = fileMetadata.getJwtToken();
        String submissionId = fileMetadata.getSubmissionID();

        boolean isValidRequest = validationService.validateFileUploadRequest(jwtToken, submissionId);
        if (isValidRequest) {
            response = new ResponseEntity<>(HttpStatus.OK);
        } else {
            // make it an error object
            FileApiError fileApiError = new FileApiError(HttpStatus.CONFLICT, ErrorMessages.INVALID_PARAMETERS);
            response = new ResponseEntity<>(fileApiError, HttpStatus.CONFLICT);
        }

        return response;
    }

    @Override
    public boolean isFileDuplicated(String fileName, String submissionUUID) {

        File existedFile = fileRepository.findByFilenameAndSubmissionId(fileName, submissionUUID);

        return existedFile != null;
    }

    @Override
    public ResponseEntity<Object> persistOrUpdateFileInformation(File file) {
        File fileToPersist = file;

        if (!file.getStatus().equals(FileStatus.INITIALIZED)) {
            String tusId = file.getGeneratedTusId();
            File persistedFile = fileRepository.findByGeneratedTusId(tusId);

            if (persistedFile == null) {
                FileApiError fileApiError = new FileApiError(HttpStatus.NOT_FOUND,
                        String.format(ErrorMessages.FILE_DOCUMENT_NOT_FOUND, tusId));
                return new ResponseEntity<>(fileApiError, HttpStatus.NOT_FOUND);
            }
            fileToPersist = updateFileProperties(file, persistedFile);
        }

        fileRepository.save(fileToPersist);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private File updateFileProperties(File newFile, File persistedFile) {
        persistedFile.setStatus(newFile.getStatus());
        persistedFile.setUploadedSize(newFile.getUploadedSize());
        persistedFile.setUploadPath(newFile.getUploadPath());
        persistedFile.setTargetPath(newFile.getTargetPath());

        return persistedFile;
    }
}
