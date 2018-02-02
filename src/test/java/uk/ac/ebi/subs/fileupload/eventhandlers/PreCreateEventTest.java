package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.errors.FileApiError;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PreCreateEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "some.jwt.token";
    private static final String SUBMISSION_UUID = "submission_1234";
    private static final String FILENAME = "test_file.cram";

    @MockBean
    private EventHandlerService eventHandlerService;

    private ResponseEntity<Object> mockedResponseOK = new ResponseEntity<>(HttpStatus.OK);
    private ResponseEntity<Object> mockedResponseNotAcceptable = new ResponseEntity<>(HttpStatus.CONFLICT);

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_UUID, FILENAME);
    }

    @Test
    public void whenRequestIsInvalid_ShouldReturnHTTPStatusNotAcceptable() {
        given(this.eventHandlerService.validateUploadRequest(tusFileInfo)).willReturn(mockedResponseNotAcceptable);

        PreCreateEvent preCreateEvent = new PreCreateEvent();

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.CONFLICT)));
    }

    @Test
    public void whenRequestIsValidAndFileIsNotDuplicated_ShouldReturnHTTPStatusOK() {
        given(this.eventHandlerService.validateUploadRequest(tusFileInfo)).willReturn(mockedResponseOK);

        PreCreateEvent preCreateEvent = new PreCreateEvent();

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    }

    @Test
    public void whenRequestIsValidAndFileIsDuplicated_ShouldReturnHTTPStatusConflictAndDuplicatedFileMessage() {
        given(this.eventHandlerService.validateUploadRequest(tusFileInfo)).willReturn(mockedResponseOK);
        given(this.eventHandlerService.isFileDuplicated(FILENAME, SUBMISSION_UUID)).willReturn(true);

        PreCreateEvent preCreateEvent = new PreCreateEvent();

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.CONFLICT)));

        FileApiError fileApiError = (FileApiError)response.getBody();
        assertThat(fileApiError.getErrors().get(0),
                is(equalTo(String.format(ErrorMessages.DUPLICATED_FILE_ERROR, FILENAME, SUBMISSION_UUID))));
    }
}
