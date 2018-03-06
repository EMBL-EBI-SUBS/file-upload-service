package uk.ac.ebi.subs.fileupload.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.services.FileService;
import uk.ac.ebi.subs.fileupload.services.SubmissionService;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.model.fileupload.FileStatus;

import java.util.Arrays;
import java.util.List;

/**
 * This is a REST controller that responsible for handling File document related requests.
 */
@RestController
public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private static final String JWT_TOKEN_PREFIX = "Bearer ";

    private FileService fileService;
    private SubmissionService submissionService;

    public FileController(FileService fileService, SubmissionService submissionService) {
        this.fileService = fileService;
        this.submissionService = submissionService;
    }

    @RequestMapping(value = "/file/{tusId}", method = RequestMethod.DELETE)
    public @ResponseBody
    ResponseEntity<Object> deleteFile(@PathVariable("tusId") String tusId,
                                      @RequestHeader(value = "Authorization") String authToken) throws Exception {
        LOGGER.debug(String.format("Delete file with id: %s", tusId));

        ResponseEntity<Object> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        File fileToDelete = fileService.getFileByTusId(tusId);

        if (fileToDelete == null) {
            return ErrorResponse.assemble(HttpStatus.NOT_FOUND,
                    String.format(ErrorMessages.FILE_DOCUMENT_NOT_FOUND, tusId));
        }

        final FileStatus fileToDeleteStatus = fileToDelete.getStatus();
        if (notDeletableFileStatuses().contains(fileToDeleteStatus)) {
            return ErrorResponse.assemble(HttpStatus.CONFLICT,
                    String.format(ErrorMessages.FILE_NOT_IN_DELETABLE_STATUS, fileToDeleteStatus));
        }

        String submissionId = fileToDelete.getSubmissionId();

        boolean submissionModifiable;
        try {
            submissionModifiable = submissionService.isModifiable(submissionId, authToken.substring(JWT_TOKEN_PREFIX.length()));
        } catch (HttpClientErrorException httpClientException) {
            return ErrorResponse.assemble(HttpStatus.UNAUTHORIZED, ErrorMessages.UNAUTHORIZED_REQUEST);
        }

        if (!submissionModifiable) {
            return ErrorResponse.assemble(HttpStatus.CONFLICT,
                    String.format(ErrorMessages.SUBMISSION_NOT_MODIFIABLE, submissionId));
        }

        fileToDelete = fileService.markFileForDeletion(fileToDelete);

        fileService.deleteFileFromFileSystem(fileToDelete.getTargetPath());

        fileService.removeDocumentMarkedForDeletion(fileToDelete);

        return response;
    }

    private List<FileStatus> notDeletableFileStatuses() {
        return Arrays.asList(FileStatus.INITIALIZED, FileStatus.UPLOADING);
    }
}
