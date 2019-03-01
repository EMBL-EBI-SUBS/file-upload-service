package uk.ac.ebi.subs.fileupload.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.errors.FileDeletionException;
import uk.ac.ebi.subs.fileupload.listeners.FileDeletedMessage;
import uk.ac.ebi.subs.fileupload.model.ChecksumGenerationMessage;
import uk.ac.ebi.subs.fileupload.model.FileContentValidationMessage;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.util.FileType;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;

/**
 * This class is responsible for handling the various events published by the tusd server.
 */
@Service
public class EventHandlerService {

    private ValidationService validationService;
    private FileRepository fileRepository;
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @Value("${file-upload.sourceBasePath}")
    private String sourcePath;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${fileProcessing-listener.checksum-calculator.jobName}")
    private String checksumCalculatorJobName;
    @Value("${fileProcessing-listener.content-validator.jobName}")
    private String contentValidatorJobName;

    private static final String EVENT_FILE_CHECKSUM_GENERATION = "file.checksum.generation";
    private static final String EVENT_FILE_CONTENT_VALIDATION = "file.content.validation";
    private static final String SUBMISSION_EXCHANGE = "usi-1:submission-exchange";

    private static final String EVENT_ASSAYDATA_FILEREF_VALIDATION_BY_FILE_DELETION = "file.deleted.validation";
    private static final String SUBMISSION_ID_CANT_BE_NULL = "Submission ID can not be null.";

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandlerService.class);

    public EventHandlerService(ValidationService validationService, FileRepository fileRepository, RabbitMessagingTemplate rabbitMessagingTemplate) {
        this.validationService = validationService;
        this.fileRepository = fileRepository;
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
    }

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

    public boolean isFileDuplicated(String fileName, String submissionUUID) {

        File existedFile = fileRepository.findByFilenameAndSubmissionId(fileName, submissionUUID);

        return existedFile != null;
    }

    public boolean isEnoughDiskSpaceExists(long fileSize) {
        java.io.File file = new java.io.File(sourcePath);
        long usableSpace = file.getUsableSpace();

        return usableSpace > fileSize;
    }

    public boolean isFileExists(String tusID) {
        return fileRepository.findByGeneratedTusId(tusID) != null;
    }

    public File getFileByTusID(String tusID) {
        return fileRepository.findByGeneratedTusId(tusID);
    }

    public void deleteFileFromDB(String tusID) {
        if (isFileExists(tusID)) {
            fileRepository.delete(fileRepository.findByGeneratedTusId(tusID));
        }
    }

    public void deleteFileFromStorage(String filePathForDeletion, String submissionID) {
        try {
            Files.deleteIfExists(Paths.get(filePathForDeletion));
            notifyFileReferenceValidatorOfFileDeletion(submissionID);
        } catch (IOException e) {
            throw new FileDeletionException(filePathForDeletion);
        }
    }

    private void notifyFileReferenceValidatorOfFileDeletion(String submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException(SUBMISSION_ID_CANT_BE_NULL);
        }

        FileDeletedMessage fileDeletedMessage = new FileDeletedMessage();
        fileDeletedMessage.setSubmissionId(submissionId);

        LOGGER.debug("Sending assay data to file reference validation queue");
        rabbitMessagingTemplate.convertAndSend(Exchanges.SUBMISSIONS, EVENT_ASSAYDATA_FILEREF_VALIDATION_BY_FILE_DELETION, fileDeletedMessage);
    }

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

    public void executeFileProcessingOnCluster(File file) {
        executeChecksumCalculationOnCluster(file);
        executeFileContentValidationOnCluster(file);
    }

    public void executeFileProcessingOnVM(File file) {
        try {
            executeChecksumCalculationOnVM(file);
            executeFileContentValidationOnVM(file);
        } catch (IOException ex) {
            LOGGER.error("Error occurred while processing the file: {}. Error message: {}",
                    file.getFilename(), ex.getMessage());
            // TODO send validationResult message to the aggregator
        }
    }

    private void executeChecksumCalculationOnCluster(File file) {
        ChecksumGenerationMessage checksumGenerationMessage = new ChecksumGenerationMessage();
        checksumGenerationMessage.setGeneratedTusId(file.getGeneratedTusId());

        LOGGER.info("Sending the following message to {} exchange with {} routing key: {}",
                SUBMISSION_EXCHANGE, EVENT_FILE_CHECKSUM_GENERATION, checksumGenerationMessage);

        rabbitMessagingTemplate.convertAndSend(SUBMISSION_EXCHANGE, EVENT_FILE_CHECKSUM_GENERATION, checksumGenerationMessage);
    }

    private void executeChecksumCalculationOnVM(File file) throws IOException {
        StringJoiner commandForComputeMD5OnVM = new StringJoiner(" ");
        commandForComputeMD5OnVM.add(checksumCalculatorJobName).add(file.getGeneratedTusId())
                .add(activeProfile);

        LOGGER.info("Executing the following command on the VM: {}", commandForComputeMD5OnVM);

        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        rt.exec(commandForComputeMD5OnVM.toString());
    }

    private void executeFileContentValidationOnCluster(File file) {
        final String fileTargetPath = file.getTargetPath();
        String fileType = FileType.getFileTypeByExtension(fileTargetPath);
        if (fileType != null) {
            FileContentValidationMessage fileContentValidationMessage = new FileContentValidationMessage();
            fileContentValidationMessage.setFileUUID(file.getId());
            fileContentValidationMessage.setFilePath(fileTargetPath);
            fileContentValidationMessage.setFileType(fileType);
            fileContentValidationMessage.setValidationResultUUID(file.getValidationResult().getUuid());
            fileContentValidationMessage.setValidationResultVersion(file.getValidationResult().getVersion());

            LOGGER.info("Sending the following message to {} exchange with {} routing key: {}",
                    SUBMISSION_EXCHANGE, EVENT_FILE_CONTENT_VALIDATION, fileContentValidationMessage);

            rabbitMessagingTemplate.convertAndSend(SUBMISSION_EXCHANGE, EVENT_FILE_CONTENT_VALIDATION, fileContentValidationMessage);
        } else {
            LOGGER.info("The uploaded file: {} is not supported for file content validation", fileTargetPath);
        }
    }

    private void executeFileContentValidationOnVM(File file) throws IOException {
        StringJoiner commandForValidateFileContent = new StringJoiner(" ");
        commandForValidateFileContent.add(contentValidatorJobName)
                .add(assembleCommandLineParameters(file))
                .add(activeProfile);

        LOGGER.info("Executing the following command on the VM: {}", commandForValidateFileContent);

        Runtime rt = Runtime.getRuntime();
        rt.exec(commandForValidateFileContent.toString());
    }

    private String assembleCommandLineParameters(File file) {
        final String targetPath = file.getTargetPath();

        StringJoiner commandLineParameters = new StringJoiner(" ");
        commandLineParameters
                .add(file.getId())
                .add(targetPath)
                .add(FileType.getFileTypeByExtension(targetPath))
                .add(file.getValidationResult().getUuid())
                .add(String.valueOf(file.getValidationResult().getVersion()));

        return commandLineParameters.toString();
    }


    public File validateFileReference(String tusId) {
        return validationService.validateFileReference(tusId);
    }

    private File updateFileProperties(File newFile, File persistedFile) {
        persistedFile.setStatus(newFile.getStatus());
        persistedFile.setUploadedSize(newFile.getUploadedSize());
        persistedFile.setUploadPath(newFile.getUploadPath());
        persistedFile.setTargetPath(newFile.getTargetPath());
        if (persistedFile.getUploadStartDate() == null) {
            persistedFile.setUploadStartDate(newFile.getUploadStartDate());
        }
        if (persistedFile.getUploadFinishDate() == null) {
            persistedFile.setUploadFinishDate(newFile.getUploadFinishDate());
        }

        return persistedFile;
    }

}
