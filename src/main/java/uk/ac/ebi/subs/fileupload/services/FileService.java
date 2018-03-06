package uk.ac.ebi.subs.fileupload.services;

import uk.ac.ebi.subs.repository.model.fileupload.File;

import java.io.IOException;

/**
 * This class is responsible for dealing with {@code File} documents.
 */
public interface FileService {

    File getFileByTusId(String tusId);

    File markFileForDeletion(File fileToMarkForDeletion);

    void deleteFileFromFileSystem(String filePath) throws IOException;

    void removeDocumentMarkedForDeletion(File fileToRemove);
}
