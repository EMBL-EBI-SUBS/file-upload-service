package uk.ac.ebi.subs.fileupload.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

@Service
public class DefaultEventHandlerService implements EventHandlerService {

    private ValidationService validationService;
    private FileRepository fileRepository;

    @Value("${file-upload.sourceBasePath}")
    private String sourcePath;

    public DefaultEventHandlerService(ValidationService validationService, FileRepository fileRepository) {
        this.validationService = validationService;
        this.fileRepository = fileRepository;
    }

    @Override
    public ResponseEntity<Object> validateUploadRequest(TUSFileInfo tusFileInfo) {
        TUSFileInfo.MetaData fileMetadata = tusFileInfo.getMetadata();

        ResponseEntity<Object> response = validationService.validateMetadata(fileMetadata);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            return response;
        }

        if (!isEnoughDiskSpaceExists(tusFileInfo.getSize())) {
            return ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format(ErrorMessages.NOT_ENOUGH_DISKSPACE, tusFileInfo.getMetadata().getFilename()));
        }

        String jwtToken = fileMetadata.getJwtToken();
        String submissionId = fileMetadata.getSubmissionID();

        return validationService.validateFileUploadRequest(jwtToken, submissionId);
    }

    @Override
    public boolean isFileDuplicated(String fileName, String submissionUUID) {

        File existedFile = fileRepository.findByFilenameAndSubmissionId(fileName, submissionUUID);

        return existedFile != null;
    }

    @Override
    public boolean isEnoughDiskSpaceExists(long fileSize) {
        java.io.File file = new java.io.File(sourcePath);
        long usableSpace = file.getUsableSpace();

        return usableSpace > fileSize;
    }

    @Override
    public ResponseEntity<Object> persistOrUpdateFileInformation(File file) {
        File fileToPersist = file;

        if (!file.getStatus().equals(FileStatus.INITIALIZED)) {
            String tusId = file.getGeneratedTusId();
            File persistedFile = fileRepository.findByGeneratedTusId(tusId);

            if (persistedFile == null) {
                return ErrorResponse.assemble(HttpStatus.NOT_FOUND, String.format(ErrorMessages.FILE_DOCUMENT_NOT_FOUND, tusId));
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
