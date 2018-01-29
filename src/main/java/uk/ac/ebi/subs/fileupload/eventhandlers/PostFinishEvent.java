package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.FileStatus;
import uk.ac.ebi.subs.fileupload.util.PropertiesLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * This class is handling the 'post-finish' hook event that is coming from the tusd server.
 * It is responsible to update the relevant existed file document in the MongoDB database.
 */
public class PostFinishEvent implements TusEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostFinishEvent.class);

    private static final String APPLICATION_PROPERTIES_FILE = "application.yml";
    private static final String SOURCE_BASE_PATH_PROPERTIES_KEY = "sourceBasePath";
    private static final String TARGET_BASE_PATH_PROPERTIES_KEY = "targetBasePath";
    private static final String BIN_FILE_EXTENSION_BY_TUS = ".bin";

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private String sourcePath;
    private String targetBasePath;

    private static final int DIR_NAME_SIZE = 3;

    PostFinishEvent() {
        Properties properties = PropertiesLoader.loadProperties(APPLICATION_PROPERTIES_FILE);

        sourcePath = properties.get(SOURCE_BASE_PATH_PROPERTIES_KEY).toString();
        targetBasePath = properties.get(TARGET_BASE_PATH_PROPERTIES_KEY).toString();
    }

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);

        file.setStatus(FileStatus.UPLOADED);

        LOGGER.debug(String.format("File object: %s", file));

        ResponseEntity<Object> response =  eventHandlerService.persistOrUpdateFileInformation(file);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            response = moveFile(file, eventHandlerService);
        }

        return response;
    }

    ResponseEntity<Object> moveFile(File file, EventHandlerService eventHandlerService) {
        ResponseEntity<Object> response;
        try {
            String sourceFileName = file.getGeneratedTusId() + BIN_FILE_EXTENSION_BY_TUS;
            String fullSourcePath = assembleFullSourcePath(sourceFileName);
            String targetPath = generateFolderName(file.getSubmissionId());
            String targetFilename = file.getFilename();
            String fullTargetPath = assembleFullTargetPath(targetBasePath, targetPath);

            setFilePpropertiesBeforeMoveFile(file, fullSourcePath, fullTargetPath, eventHandlerService);

            moveFile(targetFilename, fullSourcePath, fullTargetPath);
            deleteInfoFile(fullSourcePath);

            response = setFilePpropertiesAfterMoveFile(file, fullTargetPath, eventHandlerService);
        } catch (IOException e) {
            FileApiError fileApiError = new FileApiError(HttpStatus.ACCEPTED, ErrorMessages.FILE_CREATION_ERROR);
            response = new ResponseEntity<>(fileApiError, HttpStatus.ACCEPTED);
        }

        return response;
    }

    String assembleFullSourcePath(String sourceFileName) {
        return String.join(FILE_SEPARATOR, sourcePath, sourceFileName);
    }

    String assembleFullTargetPath(String targetBasePath, String targetPath) {
        return String.join(FILE_SEPARATOR, sourcePath, targetBasePath, targetPath);
    }

    private ResponseEntity<Object> setFilePpropertiesBeforeMoveFile(File file, String fullSourcePath, String fullTargetPath, EventHandlerService eventHandlerService) {
        file.setUploadPath(fullSourcePath);
        file.setTargetPath(String.join(FILE_SEPARATOR, fullTargetPath, file.getFilename()));

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }

    private ResponseEntity<Object> setFilePpropertiesAfterMoveFile(File file, String fullTargetPath, EventHandlerService eventHandlerService) {
        file.setStatus(FileStatus.READY_FOR_CHECKSUM);
        file.setUploadPath(String.join(FILE_SEPARATOR, fullTargetPath, file.getFilename()));

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }

    void moveFile(String filename, String fullSourcePath, String fullTargetPath) throws IOException {
        Files.createDirectories(Paths.get(fullTargetPath));
        Files.move(Paths.get(fullSourcePath), Paths.get(fullTargetPath + FILE_SEPARATOR + filename), StandardCopyOption.ATOMIC_MOVE);
    }

    void deleteInfoFile(String fullSourcePath) throws IOException {
        Files.deleteIfExists(Paths.get(fullSourcePath.substring(0, fullSourcePath.length() - 3) + "info"));
    }

    private String generateFolderName(String submissionUUID) {
        StringBuilder folderName = new StringBuilder();
        folderName.append(submissionUUID.substring(0, 1));
        folderName.append(FILE_SEPARATOR);
        folderName.append(submissionUUID.substring(1, 2));
        folderName.append(FILE_SEPARATOR);
        folderName.append(submissionUUID);

        return folderName.toString();
    }
}
