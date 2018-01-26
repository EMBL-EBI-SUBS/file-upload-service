package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostFinishEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1MTYzNjk4NTEsImV4cCI6MTU0NzkwNTg1MSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIm5hbWUiOiJLYXJlbCIsIkVtYWlsIjoia2FyZWxAZXhhbXBsZS5jb20iLCJEb21haW5zIjpbInRlYW1fYSIsInRlYW1fYiJdfQ.uGvNNVZZjb3CNc0zX5zj_QPz2pOAGZ7HQZbFeCU7a7g";
    private static final String SUBMISSION_ID = "submission_1234";
    private static final String FILENAME = "test_file.cram";
    private static final String TUS_ID = "abcd1234efgh5678.bin";

    private static final String TARGET_FOLDER_BASE = "src/test/resources/ready_to_agent";
    private static final String FOLDER1 = "/ab12";
    private static final String FOLDER2 = "/cd34/";
    private static final String TARGET_FOLDER = TARGET_FOLDER_BASE + FOLDER1 + FOLDER2;
    private static final String READY_TO_AGENT_FILE = "test_file.cram";
    private static final String TEST_FILE_TO_UPLOAD = "src/test/resources/abcd1234efgh5678.bin";

    private static final long OFFSET_SIZE_1 = 1000L;
    private static final long TOTAL_SIZE = 8000L;

    @SpyBean
    private EventHandlerService eventHandlerService;

    @Autowired
    private FileRepository fileRepository;

    private File fileToPersist;

    PostFinishEvent postFinishEvent = spy(new PostFinishEvent());

    @Before
    public void setup() throws  IOException {
        fileRepository.deleteAll();;

        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILENAME);
        tusFileInfo.setSize(TOTAL_SIZE);
        tusFileInfo.setOffsetValue(OFFSET_SIZE_1);
        tusFileInfo.setTusId(TUS_ID);

        fileToPersist = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        fileToPersist.setStatus(FileStatus.UPLOADING);

        fileRepository.save(fileToPersist);

        createTestResources();

        doReturn(TEST_FILE_TO_UPLOAD)
                .when(postFinishEvent).assembleFullSourcePath(any(String.class));
        doReturn(TARGET_FOLDER + READY_TO_AGENT_FILE)
                .when(postFinishEvent).assembleFullTargetPath(any(String.class), any(String.class));
        doNothing()
                .when(postFinishEvent).moveFile(any(String.class), any(String.class), any(String.class));
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get(TARGET_FOLDER + READY_TO_AGENT_FILE));
        Files.deleteIfExists(Paths.get(TEST_FILE_TO_UPLOAD));
        Files.deleteIfExists(Paths.get(TARGET_FOLDER_BASE + FOLDER1 + FOLDER2));
        Files.deleteIfExists(Paths.get(TARGET_FOLDER_BASE + FOLDER1));
        Files.deleteIfExists(Paths.get(TARGET_FOLDER_BASE));
    }

    @Test
    public void whenPostFinishEventTriesToUpdateANonExistingFile_ThenReturnsNotFoundHttpStatus() {
        fileRepository.deleteAll();

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

        // mock the moveFile method to check the initial status change
        doReturn(new ResponseEntity<>(HttpStatus.OK)).when(postFinishEvent).moveFile(any(File.class), Mockito.eq(eventHandlerService));

        ResponseEntity<Object> response = postFinishEvent.handle(tusFileInfo, eventHandlerService);

        File finishedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));

        assertThat(fileRepository.count(), is(1L));
        assertThat(finishedFile.getUploadedSize(), is(equalTo(TOTAL_SIZE)));
        assertThat(finishedFile.getStatus(), is(equalTo(FileStatus.UPLOADED)));
    }

    @Test
    public void whenSuccessfullyReceivedAFileButFailedWhenMovingIt_thenItShouldRespondWithAcceptedStatus() throws IOException {
        doThrow(IOException.class)
                .when(postFinishEvent).moveFile(any(String.class), any(String.class), any(String.class));

        tusFileInfo.setOffsetValue(TOTAL_SIZE);

        ResponseEntity<Object> response = postFinishEvent.handle(tusFileInfo, eventHandlerService);

        File uploadedFileDocument = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(uploadedFileDocument.getUploadPath(), is(TEST_FILE_TO_UPLOAD));
        assertThat(uploadedFileDocument.getTargetPath(), is(TARGET_FOLDER + READY_TO_AGENT_FILE));

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.ACCEPTED)));
        assertTrue(Files.notExists(Paths.get(TARGET_FOLDER + READY_TO_AGENT_FILE)));
    }

    @Test
    public void whenSuccessfullyReceivedAFileAndSuccessfullyMovedIt_thenItShouldRespondWithOKStatusAndStatusShouldBeReadyToCheck() {
        tusFileInfo.setOffsetValue(TOTAL_SIZE);

        ResponseEntity<Object> response = postFinishEvent.handle(tusFileInfo, eventHandlerService);

        File finishedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));

        assertThat(fileRepository.count(), is(1L));
        assertThat(finishedFile.getUploadedSize(), is(equalTo(TOTAL_SIZE)));
        assertThat(finishedFile.getStatus(), is(equalTo(FileStatus.READY_FOR_CHECKSUM)));

        assertThat(finishedFile.getUploadPath(), is(TARGET_FOLDER + READY_TO_AGENT_FILE));
        assertThat(finishedFile.getTargetPath(), is(TARGET_FOLDER + READY_TO_AGENT_FILE));
    }

    private void createTestResources() throws IOException {
        Files.createDirectories(Paths.get(TARGET_FOLDER));
        List<String> lines = Arrays.asList("This is a TEST file.", "This is the second line.");
        Path file = Paths.get(TEST_FILE_TO_UPLOAD);
        Files.write(file, lines, Charset.forName("UTF-8"));
    }
}
