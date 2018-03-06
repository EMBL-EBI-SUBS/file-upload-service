package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.util.JWTExtractor;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostCreateEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "some.jwt.token";
    private static final String SUBMISSION_ID = "submission_1234";
    private static final String FILENAME = "test_file.cram";
    private static final String UPLOAD_USER = "Karel";

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired
    private FileRepository fileRepository;

    private PostCreateEvent postCreateEvent = spy(new PostCreateEvent());

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILENAME);
        fileRepository.deleteAll();

        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put(JWTExtractor.USERNAME_KEY, UPLOAD_USER);
        jsonPayload.put(JWTExtractor.DOMAINS_KEY, "Test_domain");

        JWTExtractor jwtExtractor = spy(new JWTExtractor(JWT_TOKEN));

        doReturn(jsonPayload)
                .when(jwtExtractor).decodePayload(any(String.class));
        doReturn(jwtExtractor)
                .when(postCreateEvent).setupJWTExtractor(JWT_TOKEN);
    }

    @Test
    public void whenRequestIsValid_ShouldReturnHTTPStatusOKAndDocumentShouldHaveBeenCreated() {
        ResponseEntity<Object> response = postCreateEvent.handle(tusFileInfo, eventHandlerService);

        File persistedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));

        assertThat(fileRepository.count(), is(1L));
        assertThat(persistedFile.getFilename(), is(equalTo(FILENAME)));
        assertThat(persistedFile.getCreatedBy(), is(equalTo(UPLOAD_USER)));
    }
}
