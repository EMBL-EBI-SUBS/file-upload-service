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
            moveFile(file);
            file.setStatus(FileStatus.READY_TO_CHECK);
            response =  eventHandlerService.persistOrUpdateFileInformation(file);
        } catch (IOException e) {
            FileApiError fileApiError = new FileApiError(HttpStatus.ACCEPTED, ErrorMessages.FILE_CREATION_ERROR);
            response = new ResponseEntity<>(fileApiError, HttpStatus.ACCEPTED);
        }

        return response;
    }

    void moveFile(File file) throws IOException {
        String sourceFileName = file.getTusId();

        String targetPath = generateFolderName(file.getTusId());

        createTargetFolder(sourcePath, targetBasePath, targetPath);

        String targetFilename = file.getFilename();

        String fullTargetPath = assembleFullTargetPath(sourcePath, targetBasePath, targetPath, targetFilename);

        String fullSourcePath = assembleFullSourcePath(sourcePath, sourceFileName);

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

    void createTargetFolder(String sourcePath, String targetBasePath, String targetPath) throws IOException {
        Files.createDirectories(Paths.get(String.join("/", sourcePath, targetBasePath, targetPath)));
    }

    String assembleFullSourcePath(String sourcePath, String sourceFileName) {
        return String.join("/", sourcePath, sourceFileName);
    }

    String assembleFullTargetPath(String sourcePath, String targetBasePath, String targetPath, String targetFilename) {
        return String.join("/", sourcePath, targetBasePath, targetPath, targetFilename);
    }

}
