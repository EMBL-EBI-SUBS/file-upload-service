package uk.ac.ebi.subs.fileupload.services;

import org.springframework.http.ResponseEntity;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class is responsible for handling the various events published by the tusd server.
 */
public interface EventHandlerService {

    ResponseEntity<Object> validateUploadRequest(TUSFileInfo tusFileInfo);

    ResponseEntity<Object> persistOrUpdateFileInformation(File file);

    void moveFile(File file) throws IOException;

    default void createTargetFolder(String sourcePath, String targetBasePath, String targetPath) throws IOException {
        Files.createDirectories(Paths.get(String.join("/", sourcePath, targetBasePath, targetPath)));
    }

    default String assembleFullSourcePath(String sourcePath, String sourceFileName) {
        return String.join("/", sourcePath, sourceFileName);
    }

    default String assembleFullTargetPath(String sourcePath, String targetBasePath, String targetPath, String targetFilename) {
        return String.join("/", sourcePath, targetBasePath, targetPath, targetFilename);
    }
}
