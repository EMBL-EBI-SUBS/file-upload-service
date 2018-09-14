package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostTerminateEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "some.jwt.token";
    private static final String SUBMISSION_ID = "submission_1234";
    private static final String FILENAME = "test_file.cram";

    private static final long OFFSET_SIZE_1 = 1000L;

    @SpyBean
    private EventHandlerService eventHandlerService;

    @Autowired
    private FileRepository fileRepository;

    private File temporaryUploadedFile;

    @Before
    public void setup() {
        fileRepository.deleteAll();;

        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILENAME);
        tusFileInfo.setSize(8000L);
        tusFileInfo.setOffsetValue(4440L);

        temporaryUploadedFile = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        temporaryUploadedFile.setStatus(FileStatus.UPLOADING);

        fileRepository.save(temporaryUploadedFile);
    }

    @Test
    public void whenPostTerminateEventTriesToDeleteANonExistingFile_ThenReturnsNotFoundHttpStatus() {
        fileRepository.deleteAll();

        tusFileInfo.setOffsetValue(OFFSET_SIZE_1);

        PostTerminateEvent postTerminateEvent = new PostTerminateEvent();

        ResponseEntity<Object> response = postTerminateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.NOT_FOUND)));
        assertThat(fileRepository.count(), is(0L));
    }

    @Test
    public void whenReceivingAPostTerminateEvent_ItShouldReturnHTTPStatusAcceptedAndFileDocumentShouldBeDeleted() {
        doNothing().when(eventHandlerService).deleteFileFromStorage(any(String.class), any(String.class));

        assertThat(fileRepository.count(), is(1L));

        File fileUnderUpload = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(fileUnderUpload.getStatus(), is(equalTo(FileStatus.UPLOADING)));

        PostTerminateEvent postTerminateEvent = new PostTerminateEvent();

        ResponseEntity<Object> response = postTerminateEvent.handle(tusFileInfo, eventHandlerService);

        File deletedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.ACCEPTED)));

        assertThat(fileRepository.count(), is(0L));
        assertThat(deletedFile, is(nullValue()));
    }
}
