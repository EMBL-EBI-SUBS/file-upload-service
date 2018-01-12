package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.FileStatus;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostReceiveEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "xxxxx.yyyyy.zzzz";
    private static final String SUBMISSION_ID = "submission_1234";
    private static final String FILENAME = "test_file.cram";

    private static final long OFFSET_SIZE_1 = 1000L;

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired
    private FileRepository fileRepository;

    private File fileToPersist;

    @Before
    public void setup() {
        fileRepository.deleteAll();;

        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILENAME);
        tusFileInfo.setSize(8000L);
        tusFileInfo.setOffsetValue(0L);

        fileToPersist = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        fileToPersist.setStatus(FileStatus.INITIALIZED);

        fileRepository.save(fileToPersist);
    }

    @Test
    public void whenPostReceiveEventTriesToUpdateANonExistingFile_ThenReturnsNotFoundHttpStatus() {
        fileRepository.deleteAll();

        tusFileInfo.setOffsetValue(OFFSET_SIZE_1);

        PostReceiveEvent postReceiveEvent = new PostReceiveEvent();

        ResponseEntity<Object> response = postReceiveEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.NOT_FOUND)));
        assertThat(fileRepository.count(), is(0L));
    }

    @Test
    public void whenReceivingAPostReceiveEvent_ItShouldReturnHTTPStatusOKAndDocumentStatusAndUploadedSizeShouldBeUpdated() {
        assertThat(fileRepository.count(), is(1L));

        File persistedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(persistedFile.getStatus(), is(equalTo(FileStatus.INITIALIZED)));

        tusFileInfo.setOffsetValue(OFFSET_SIZE_1);

        PostReceiveEvent postReceiveEvent = new PostReceiveEvent();

        ResponseEntity<Object> response = postReceiveEvent.handle(tusFileInfo, eventHandlerService);

        File modifiedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));

        assertThat(fileRepository.count(), is(1L));
        assertThat(modifiedFile.getUploadedSize(), is(equalTo(OFFSET_SIZE_1)));
        assertThat(modifiedFile.getStatus(), is(equalTo(FileStatus.UPLOADING)));
    }
}
