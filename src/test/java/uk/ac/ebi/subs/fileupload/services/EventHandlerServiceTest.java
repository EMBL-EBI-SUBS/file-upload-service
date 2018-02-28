package uk.ac.ebi.subs.fileupload.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.ErrorResponse;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class EventHandlerServiceTest {

    private static final String EXISTING_FILE_NAME = "already_uploaded_file.cram";
    private static final String NEW_FILE_NAME = "new_file.cram";
    private static final String SUBMISSION_ID = "12ab34cd56ef";
    private static final String JWT_TOKEN = "dummy.jwt.token";

    private File persistedFile;
    private TUSFileInfo tusFileInfo;

    @MockBean
    private FileRepository fileRepository;

    @MockBean
    private ValidationService validationService;

    private EventHandlerService eventHandlerService;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, EXISTING_FILE_NAME);
        persistedFile = FileHelper.convertTUSFileInfoToFile(tusFileInfo);

        eventHandlerService = new DefaultEventHandlerService(validationService, fileRepository, rabbitMessagingTemplate);
    }

    @Test
    public void whenFileAlreadyUploadedToAGivenSubmission_ThenUploadAgainFailsValidation() {
        given(this.fileRepository.findByFilenameAndSubmissionId(EXISTING_FILE_NAME, SUBMISSION_ID))
                .willReturn(persistedFile);

        boolean isDuplicatedFile = eventHandlerService.isFileDuplicated(EXISTING_FILE_NAME, SUBMISSION_ID);

        assertTrue(isDuplicatedFile);
    }

    @Test
    public void whenFileNotYetUploadedToAGivenSubmission_ThenUploadWillSucceed() {
        given(this.fileRepository.findByFilenameAndSubmissionId(NEW_FILE_NAME, SUBMISSION_ID))
                .willReturn(null);

        boolean isDuplicatedFile = eventHandlerService.isFileDuplicated(NEW_FILE_NAME, SUBMISSION_ID);

        assertFalse(isDuplicatedFile);
    }

    @Test
    public void whenFilenameNotSentWithMetadata_ThenUploadFailsWithUnprocessableEntityStatus() {
        given(this.validationService.validateMetadata(tusFileInfo.getMetadata()))
                .willReturn(
                        ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY,
                                ErrorMessages.FILENAME_MANDATORY));

        ResponseEntity<Object> response = eventHandlerService.validateUploadRequest(tusFileInfo);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));
    }

    @Test
    public void whenSubmissionIdNotSentWithMetadata_ThenUploadFailsWithUnprocessableEntityStatus() {
        given(this.validationService.validateMetadata(tusFileInfo.getMetadata()))
                .willReturn(
                        ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY,
                                ErrorMessages.SUBMISSION_ID_MANDATORY));

        ResponseEntity<Object> response = eventHandlerService.validateUploadRequest(tusFileInfo);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));
    }

    @Test
    public void whenJWTTokenNotSentWithMetadata_ThenUploadFailsWithUnprocessableEntityStatus() {
        given(this.validationService.validateMetadata(tusFileInfo.getMetadata()))
                .willReturn(
                        ErrorResponse.assemble(HttpStatus.UNPROCESSABLE_ENTITY,
                                ErrorMessages.JWT_TOKEN_MANDATORY));

        ResponseEntity<Object> response = eventHandlerService.validateUploadRequest(tusFileInfo);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));
    }
}

