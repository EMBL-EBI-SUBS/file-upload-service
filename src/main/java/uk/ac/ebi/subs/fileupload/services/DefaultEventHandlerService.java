package uk.ac.ebi.subs.fileupload.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Random;

@Service
public class DefaultEventHandlerService implements EventHandlerService {

    private ValidationService validationService;
    private FileRepository fileRepository;

    @Value("${file-upload.sourceBasePath}")
    private String sourcePath;
    @Value("${file-upload.targetBasePath}")
    private String targetBasePath;

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
    public ResponseEntity<Object> persistOrUpdateFileInformation(File file) {
        if (!file.getStatus().equals(FileStatus.INITIALIZED)) {
            File persistedFile = fileRepository.findByFilenameAndSubmissionId(file.getFilename(), file.getSubmissionId());

            if (persistedFile == null) {
                FileApiError fileApiError = new FileApiError(HttpStatus.NOT_FOUND, ErrorMessages.FILE_DOCUMENT_NOT_FOUND);
                return new ResponseEntity<>(fileApiError, HttpStatus.NOT_FOUND);
            }
        }

        fileRepository.save(file);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public void moveFile(File file) throws IOException {
        String sourceFileName = file.getTusId();

        String targetPath = generateFolderName(file.getTusId());

        createTargetFolder(sourcePath, targetBasePath, targetPath);

        String targetFilename = file.getFilename();

        String fullTargetPath = assembleFullTargetPath(sourcePath, targetBasePath, targetPath, targetFilename);

        String fullSourcePath = assembleFullSourcePath(sourcePath, sourceFileName);

        Files.move(Paths.get(fullSourcePath), Paths.get(fullTargetPath), StandardCopyOption.ATOMIC_MOVE);
    }

    private String generateFolderName(String tusId) {
        StringBuffer folderName = new StringBuffer();
        Random random = new Random();
        for (int i = 0; i < 2; i++) {
            if (folderName.length() > 0) {
                folderName.append("/");
            }
            int startIndex = random.nextInt(10);
            folderName.append(tusId.substring(startIndex, startIndex + 4));
        }

        return folderName.toString();
    }
}
