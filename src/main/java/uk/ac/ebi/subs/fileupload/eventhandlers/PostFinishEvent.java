package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.Utils;
import uk.ac.ebi.subs.repository.model.fileupload.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

/**
 * This class is handling the 'post-finish' hook event that is coming from the tusd server.
 * It is responsible to update the relevant existed file document in the MongoDB database.
 */
@Component
public class PostFinishEvent implements TusEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostFinishEvent.class);

    private static final String BIN_FILE_EXTENSION_BY_TUS = ".bin";

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    @Value("${file-upload.sourceBasePath}")
    private String sourcePath;
    @Value("${file-upload.targetBasePath}")
    private String targetBasePath;
    @Value("${file-upload.filePrefixForLocalProcessing}")
    private String filePrefixForLocalProcessing;

    @Override
    public ResponseEntity<Object> handle(TUSFileInfo tusFileInfo, EventHandlerService eventHandlerService) {
        File file = FileHelper.convertTUSFileInfoToFile(tusFileInfo);

        file.setStatus(FileStatus.UPLOADED);
        file.setUploadFinishDate(LocalDateTime.now());

        LOGGER.debug(String.format("File object: %s", file));

        ResponseEntity<Object> response =  eventHandlerService.persistOrUpdateFileInformation(file);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            String sourceFileName = file.getGeneratedTusId() + BIN_FILE_EXTENSION_BY_TUS;
            String fullSourcePath = assembleFullSourcePath(sourceFileName);
            String targetPath = Utils.generateFolderName(file.getSubmissionId());
            String fullTargetPath = assembleFullTargetPath(targetBasePath, targetPath);

            setFilePropertiesBeforeMoveFile(file, fullSourcePath, fullTargetPath, eventHandlerService);

            response = moveFile(file, eventHandlerService, fullSourcePath, fullTargetPath);
            file = eventHandlerService.validateFileReference(file.getGeneratedTusId());

            processFile(eventHandlerService, file);
        }

        return response;
    }

    private void processFile(EventHandlerService eventHandlerService, File file) {
        if (!file.getFilename().startsWith(filePrefixForLocalProcessing)) {
            eventHandlerService.executeFileProcessingOnCluster(file);
        } else {
            eventHandlerService.executeFileProcessingOnVM(file);
        }
    }

    ResponseEntity<Object> moveFile(File file, EventHandlerService eventHandlerService, String fullSourcePath, String fullTargetPath) {
        ResponseEntity<Object> response;
        try {
            moveFile(file.getFilename(), fullSourcePath, fullTargetPath);
            deleteInfoFile(fullSourcePath);

            response = setFilePropertiesAfterMoveFile(file, fullTargetPath, eventHandlerService);
        } catch (IOException e) {
            response = ErrorResponse.assemble(HttpStatus.ACCEPTED, ErrorMessages.FILE_CREATION_ERROR);
        }

        return response;
    }

    String assembleFullSourcePath(String sourceFileName) {
        return String.join(FILE_SEPARATOR, sourcePath, sourceFileName);
    }

    String assembleFullTargetPath(String targetBasePath, String targetPath) {
        return String.join(FILE_SEPARATOR, sourcePath, targetBasePath, targetPath);
    }

    private ResponseEntity<Object> setFilePropertiesBeforeMoveFile(File file, String fullSourcePath, String fullTargetPath, EventHandlerService eventHandlerService) {
        file.setUploadPath(fullSourcePath);
        file.setTargetPath(String.join(FILE_SEPARATOR, fullTargetPath, file.getFilename()));

        return eventHandlerService.persistOrUpdateFileInformation(file);
    }

    private ResponseEntity<Object> setFilePropertiesAfterMoveFile(File file, String fullTargetPath, EventHandlerService eventHandlerService) {
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

}
