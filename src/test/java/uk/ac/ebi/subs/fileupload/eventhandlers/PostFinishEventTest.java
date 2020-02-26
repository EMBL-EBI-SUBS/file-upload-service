package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusApiClient;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusService;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.validator.data.ValidationResult;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostFinishEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "some.jwt.token";
    private static final String SUBMISSION_ID = "submission_1234";
    private static final String FILENAME = "test_file.fastq.gz";
    private static final String TUS_ID = "abcdefgh12345678";

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static final String TARGET_FOLDER_BASE = "src/test/resources/ready_to_agent";
    private static final String FOLDER1 = FILE_SEPARATOR + SUBMISSION_ID.substring(0, 1);
    private static final String FOLDER2 = FILE_SEPARATOR + SUBMISSION_ID.substring(1, 2);
    private static final String FOLDER3 = FILE_SEPARATOR + SUBMISSION_ID;
    private static final String TARGET_FOLDER = TARGET_FOLDER_BASE + FOLDER1 + FOLDER2 + FOLDER3;
    private static final String READY_TO_AGENT_FILE = FILENAME;
    private static final String TEST_FILE_TO_UPLOAD = "src/test/resources/abcdefgh12345678.bin";

    private static final long OFFSET_SIZE_1 = 1000L;
    private static final long TOTAL_SIZE = 8000L;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @SpyBean
    private EventHandlerService eventHandlerService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ValidationResultRepository validationResultRepository;

    private File fileToPersist;

    @SpyBean
    private PostFinishEvent postFinishEvent;

    @MockBean
    private GlobusApiClient globusApiClient;

    @MockBean
    private GlobusService globusService;

    @Before
    public void setup() throws  IOException {
        fileRepository.deleteAll();;

        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILENAME);
        tusFileInfo.setSize(TOTAL_SIZE);
        tusFileInfo.setOffsetValue(OFFSET_SIZE_1);
        tusFileInfo.setTusId(TUS_ID);

        fileToPersist = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        fileToPersist.setStatus(FileStatus.UPLOADING);

        fileToPersist.setValidationResult(createValidationResult(fileToPersist));

        fileRepository.save(fileToPersist);

        createTestResources();

        doNothing()
                .when(postFinishEvent).moveFile(any(String.class), any(String.class), any(String.class));
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get(TARGET_FOLDER + READY_TO_AGENT_FILE));
        Files.deleteIfExists(Paths.get(TEST_FILE_TO_UPLOAD));
        Files.deleteIfExists(Paths.get(TARGET_FOLDER_BASE + FOLDER1 + FOLDER2 + FOLDER3));
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
        doReturn(new ResponseEntity<>(HttpStatus.OK)).when(postFinishEvent)
                .moveFile(any(File.class), Mockito.eq(eventHandlerService), any(String.class), any(String.class));

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
        assertThat(uploadedFileDocument.getTargetPath(), is(TARGET_FOLDER + FILE_SEPARATOR + READY_TO_AGENT_FILE));

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
        assertThat(finishedFile.getUploadFinishDate(), is(notNullValue()));

        assertThat(finishedFile.getUploadPath(), is(TARGET_FOLDER + FILE_SEPARATOR + READY_TO_AGENT_FILE));
        assertThat(finishedFile.getTargetPath(), is(TARGET_FOLDER+ FILE_SEPARATOR + READY_TO_AGENT_FILE));
    }

    private void createTestResources() throws IOException {
        Files.createDirectories(Paths.get(TARGET_FOLDER));
        List<String> lines = Arrays.asList("This is a TEST file.", "This is the second line.");
        Path file = Paths.get(TEST_FILE_TO_UPLOAD);
        Files.write(file, lines, Charset.forName("UTF-8"));
    }

    private ValidationResult createValidationResult(File file) {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setEntityUuid(file.getId());
        validationResult.setUuid(UUID.randomUUID().toString());

        validationResult.setSubmissionId(file.getSubmissionId());
        validationResultRepository.save(validationResult);

        return validationResult;
    }
}
