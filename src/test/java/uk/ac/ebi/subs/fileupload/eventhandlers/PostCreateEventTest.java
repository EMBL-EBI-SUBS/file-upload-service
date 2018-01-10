package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostCreateEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "xxxxx.yyyyy.zzzz";
    private static final String SUBMISSION_UUID = "submission_1234";

    @Autowired
    private EventHandlerService eventHandlerService;

    @MockBean
    private FileRepository fileRepository;

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_UUID);
    }

    @Test
    public void whenRequestIsValid_ShouldReturnHTTPStatusOKAndDocumentShouldHaveBeenCreated() {
        PostCreateEvent postCreateEvent = new PostCreateEvent();

        ResponseEntity<Object> response = postCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));

        verify(fileRepository, times(1)).insert(any(File.class));
    }
}