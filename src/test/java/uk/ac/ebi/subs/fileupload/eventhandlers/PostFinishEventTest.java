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
public class PostFinishEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "xxxxx.yyyyy.zzzz";
    private static final String SUBMISSION_ID = "submission_1234";
    private static final String FILENAME = "test_file.cram";

    private static final long OFFSET_SIZE_1 = 1000L;
    private static final long TOTAL_SIZE = 8000L;

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired
    private FileRepository fileRepository;

    private File fileToPersist;

    @Before
    public void setup() {
        fileRepository.deleteAll();;

        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILENAME);
        tusFileInfo.setSize(TOTAL_SIZE);
        tusFileInfo.setOffsetValue(OFFSET_SIZE_1);

        fileToPersist = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        fileToPersist.setStatus(FileStatus.UPLOADING);

        fileRepository.save(fileToPersist);
    }

    @Test
    public void whenPostFinishEventTriesToUpdateANonExistingFile_ThenReturnsNotFoundHttpStatus() {
        fileRepository.deleteAll();

        PostFinishEvent postFinishEvent = new PostFinishEvent();

        ResponseEntity<Object> response = postFinishEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.NOT_FOUND)));
        assertThat(fileRepository.count(), is(0L));
    }

    @Test
    public void whenReceivingAPostFinishEvent_ItShouldReturnHTTPStatusOKAndDocumentStatusAndUploadedSizeShouldBeUpdated() {
        assertThat(fileRepository.count(), is(1L));

        File persistedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(persistedFile.getStatus(), is(equalTo(FileStatus.UPLOADING)));

        tusFileInfo.setOffsetValue(TOTAL_SIZE);

        PostFinishEvent postFinishEvent = new PostFinishEvent();

        ResponseEntity<Object> response = postFinishEvent.handle(tusFileInfo, eventHandlerService);

        File finishedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));

        assertThat(fileRepository.count(), is(1L));
        assertThat(finishedFile.getUploadedSize(), is(equalTo(TOTAL_SIZE)));
        assertThat(finishedFile.getStatus(), is(equalTo(FileStatus.UPLOADED)));
    }
}
