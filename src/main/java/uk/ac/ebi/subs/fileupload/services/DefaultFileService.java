package uk.ac.ebi.subs.fileupload.services;

import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class DefaultFileService implements FileService {

    private FileRepository fileRepository;

    public DefaultFileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public File getFileByTusId(String tusId) {
        return fileRepository.findByGeneratedTusId(tusId);
    }

    @Override
    public File markFileForDeletion(File fileToMarkForDeletion) {
        fileToMarkForDeletion.setStatus(FileStatus.MARK_FOR_DELETION);

        return fileRepository.save(fileToMarkForDeletion);
    }

    @Override
    public void deleteFileFromFileSystem(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    @Override
    public void removeDocumentMarkedForDeletion(File fileToRemove) {
        fileRepository.delete(fileToRemove);
    }
}
