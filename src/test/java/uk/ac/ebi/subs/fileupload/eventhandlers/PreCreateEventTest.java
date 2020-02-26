package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.services.ValidationService;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusApiClient;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusService;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PreCreateEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "some.jwt.token";
    private static final String SUBMISSION_UUID = "submission_1234";
    private static final String FILENAME = "test_file.cram";

    @SpyBean
    private EventHandlerService eventHandlerService;

    @SpyBean
    private ValidationService validationService;

    @MockBean
    private FileRepository fileRepository;

    @MockBean
    private GlobusApiClient globusApiClient;

    @MockBean
    private GlobusService globusService;

    PreCreateEvent preCreateEvent;

    private ResponseEntity<Object> mockedResponseOK = new ResponseEntity<>(HttpStatus.OK);
    private ResponseEntity<Object> mockedResponseNotAcceptable = new ResponseEntity<>(HttpStatus.CONFLICT);

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_UUID, FILENAME);
        given(this.validationService.validateMetadata(tusFileInfo.getMetadata())).willReturn(mockedResponseOK);

        preCreateEvent = new PreCreateEvent();
    }

    @Test
    public void whenRequestIsInvalid_ShouldReturnHTTPStatusNotAcceptable() {
        doReturn(mockedResponseNotAcceptable)
                .when(this.eventHandlerService).validateUploadRequest(tusFileInfo);

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.CONFLICT)));
    }

    @Test
    public void whenRequestIsValidAndFileIsNotDuplicated_ShouldReturnHTTPStatusOK() {
        doReturn(mockedResponseOK)
                .when(this.eventHandlerService).validateUploadRequest(tusFileInfo);

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    }

    @Test
    public void whenRequestIsValidAndFileIsDuplicated_ShouldReturnHTTPStatusConflictAndDuplicatedFileMessage() {
        doReturn(mockedResponseOK)
                .when(this.eventHandlerService).validateUploadRequest(tusFileInfo);
        doReturn(true)
                .when(this.eventHandlerService).isFileDuplicated(FILENAME, SUBMISSION_UUID);

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.CONFLICT)));

        FileApiError fileApiError = (FileApiError)response.getBody();
        assertThat(fileApiError.getErrors().get(0),
                is(equalTo(String.format(ErrorMessages.DUPLICATED_FILE_ERROR, FILENAME, SUBMISSION_UUID))));
    }

    @Test
    public void whenNotEnoughDiskSpaceExists_ShouldReturnHTTPStatusUnprocessableEntityAndNotEnoughDiskSpaceMessage() {
        doReturn(false).when(eventHandlerService).isEnoughDiskSpaceExists(anyLong());

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));

        FileApiError fileApiError = (FileApiError)response.getBody();
        assertThat(fileApiError.getErrors().get(0),
                is(equalTo(String.format(ErrorMessages.NOT_ENOUGH_DISKSPACE, tusFileInfo.getMetadata().getFilename()))));
    }
}
