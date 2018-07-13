package uk.ac.ebi.subs.fileupload.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
public class ValidationServiceTest {

    private static final String FILE_NAME = "test_file.cram";
    private static final String SUBMISSION_ID = "12ab34cd56ef";
    private static final String JWT_TOKEN = "dummy.jwt.token";

    private TUSFileInfo tusFileInfo;

    @MockBean
    private SubmissionService submissionService;
    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @MockBean
    private ValidationResultRepository validationResultRepository;
    @MockBean
    private FileRepository fileRepository;


    private ValidationService validationService;

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, FILE_NAME);

        validationService = new ValidationService(submissionService, rabbitMessagingTemplate,
                validationResultRepository, fileRepository);
    }

    @Test
    public void whenFilenameNotSentWithMetadata_ThenUploadFailsWithUnprocessableEntityStatus() {
        tusFileInfo.getMetadata().setFilename(null);

        ResponseEntity<Object> response = validationService.validateMetadata(tusFileInfo.getMetadata());

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));
    }

    @Test
    public void whenSubmissionIdNotSentWithMetadata_ThenUploadFailsWithUnprocessableEntityStatus() {
        tusFileInfo.getMetadata().setSubmissionID(null);

        ResponseEntity<Object> response = validationService.validateMetadata(tusFileInfo.getMetadata());

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));
    }

    @Test
    public void whenJWTTokenNotSentWithMetadata_ThenUploadFailsWithUnprocessableEntityStatus() {
        tusFileInfo.getMetadata().setJwtToken(null);

        ResponseEntity<Object> response = validationService.validateMetadata(tusFileInfo.getMetadata());

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)));
    }
}

