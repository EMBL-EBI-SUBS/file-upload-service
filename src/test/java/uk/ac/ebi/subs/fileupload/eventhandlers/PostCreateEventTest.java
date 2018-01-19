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
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostCreateEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1MTYzNjk4NTEsImV4cCI6MTU0NzkwNTg1MSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIm5hbWUiOiJLYXJlbCIsIkVtYWlsIjoia2FyZWxAZXhhbXBsZS5jb20iLCJEb21haW5zIjpbInRlYW1fYSIsInRlYW1fYiJdfQ.uGvNNVZZjb3CNc0zX5zj_QPz2pOAGZ7HQZbFeCU7a7g";
    private static final String SUBMISSION_ID = "submission_1234";
    private static final String FILENAME = "test_file.cram";
    private static final String UPLOAD_USER = "Karel";

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired
    private FileRepository fileRepository;

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILENAME);
        fileRepository.deleteAll();
    }

    @Test
    public void whenRequestIsValid_ShouldReturnHTTPStatusOKAndDocumentShouldHaveBeenCreated() {
        PostCreateEvent postCreateEvent = new PostCreateEvent();

        ResponseEntity<Object> response = postCreateEvent.handle(tusFileInfo, eventHandlerService);

        File persistedFile = fileRepository.findByFilenameAndSubmissionId(FILENAME, SUBMISSION_ID);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));

        assertThat(fileRepository.count(), is(1L));
        assertThat(persistedFile.getFilename(), is(equalTo(FILENAME)));
        assertThat(persistedFile.getCreatedBy(), is(equalTo(UPLOAD_USER)));
    }
}
