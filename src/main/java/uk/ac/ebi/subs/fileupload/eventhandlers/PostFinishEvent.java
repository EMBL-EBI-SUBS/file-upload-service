package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * This class is handling the 'post-finish' hook event that is coming from the tusd server.
 * It is responsible to update the relevant existed file document in the MongoDB database.
 */
public class PostFinishEvent implements TusEvent {

    @Value("${file-upload.sourceBasePath}")
    private String sourcePath;
    @Value("${file-upload.targetBasePath}")
    private String targetBasePath;

    private static final int DIR_NAME_SIZE = 3;


    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        file.setStatus(FileStatus.UPLOADED);

        ResponseEntity<Object> response =  eventHandlerService.persistOrUpdateFileInformation(file);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            response = moveFile(file, eventHandlerService);
        }

        return response;
    }

    ResponseEntity<Object> moveFile(File file, EventHandlerService eventHandlerService) {
        ResponseEntity<Object> response;
        try {
            String sourceFileName = file.getTusId();
            String fullSourcePath = assembleFullSourcePath(sourceFileName);
            String targetPath = generateFolderName(sourceFileName);
            String targetFilename = file.getFilename();
            String fullTargetPath = assembleFullTargetPath(targetBasePath, targetPath, targetFilename);

            setFilePpropertiesBeforeMoveFile(file, fullSourcePath, fullTargetPath, eventHandlerService);

            moveFile(file, fullSourcePath, fullTargetPath);

            response = setFilePpropertiesAfterMoveFile(file, fullTargetPath, eventHandlerService);
        } catch (IOException e) {
            FileApiError fileApiError = new FileApiError(HttpStatus.ACCEPTED, ErrorMessages.FILE_CREATION_ERROR);
            response = new ResponseEntity<>(fileApiError, HttpStatus.ACCEPTED);
        }

        return response;
    }

    private ResponseEntity<Object> setFilePpropertiesBeforeMoveFile(File file, String fullSourcePath, String fullTargetPath, EventHandlerService eventHandlerService) {
        file.setUploadPath(fullSourcePath);
        file.setTargetPath(fullTargetPath);

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }

    private ResponseEntity<Object> setFilePpropertiesAfterMoveFile(File file, String fullTargetPath, EventHandlerService eventHandlerService) {
        file.setStatus(FileStatus.READY_TO_CHECK);
        file.setUploadPath(fullTargetPath);

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }

    void moveFile(File file, String fullSourcePath, String fullTargetPath) throws IOException {
        createTargetFolder(fullTargetPath);

        Files.move(Paths.get(fullSourcePath), Paths.get(fullTargetPath), StandardCopyOption.ATOMIC_MOVE);
    }

    private String generateFolderName(String tusId) {
        StringBuilder folderName = new StringBuilder();
        int startIndex = 0;
        for (int i = 0; i < 2; i++) {
            if (folderName.length() > 0) {
                folderName.append("/");
            }
            folderName.append(tusId.substring(startIndex, startIndex + DIR_NAME_SIZE));
            startIndex = DIR_NAME_SIZE;
        }

        return folderName.toString();
    }

    void createTargetFolder(String fullTargetPath) throws IOException {
        Files.createDirectories(Paths.get(fullTargetPath));
    }

    String assembleFullSourcePath(String sourceFileName) {
        return String.join("/", sourcePath, sourceFileName);
    }

    String assembleFullTargetPath(String targetBasePath, String targetPath, String targetFilename) {
        return String.join("/", sourcePath, targetBasePath, targetPath, targetFilename);
    }

}
