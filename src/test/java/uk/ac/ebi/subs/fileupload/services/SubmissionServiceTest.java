package uk.ac.ebi.subs.fileupload.services;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import uk.ac.ebi.subs.fileupload.errors.SubmissionNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest( { SubmissionService.class } )
public class SubmissionServiceTest {

    @Value("${subs-api.host}")
    private String serviceHost;
    @Value("${subs-api.submissionStatusURI}")
    private String submissionStatusURI;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private MockRestServiceServer server;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getSubmissionStatusByInvalidIdShouldReturnSubmissionNotExistsError() {
        String invalidSubmissionUuid = "11112222-aaaa-bbbb-cccc-123456789012";
        String SUBMISSION_NOT_EXISTS_MESSAGE = "Submission not found with id: %s";

        this.server.expect(
                requestTo(String.format(submissionStatusURI, serviceHost, invalidSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
        );

        this.thrown.expect(SubmissionNotFoundException.class);
        this.thrown.expectMessage(String.format(SUBMISSION_NOT_EXISTS_MESSAGE, invalidSubmissionUuid));

        submissionService.getSubmissionStatus(invalidSubmissionUuid);
    }

    @Test
    public void getSubmissionStatusByValidIdWithDraftStatusShouldReturnTheSubmissionStatus() throws IOException {
        String validDraftSubmissionUuid = "33334444-aaaa-bbbb-cccc-123456789012";

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("draftSubmissionStatusByValidSubmissionId.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionStatusURI, serviceHost, validDraftSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
        );

        String submissionStatus = submissionService.getSubmissionStatus(validDraftSubmissionUuid);

        assertThat(submissionStatus, is(equalTo("Draft")));
    }

    @Test
    public void getSubmissionStatusByValidIdWithSubmittedStatusShouldReturnTheSubmissionStatus() throws IOException {
        String validSubmittedSubmissionUuid = "55556666-aaaa-bbbb-cccc-123456789012";

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submittedSubmissionStatusByValidSubmissionId.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionStatusURI, serviceHost, validSubmittedSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
                );

        String submissionStatus = submissionService.getSubmissionStatus(validSubmittedSubmissionUuid);

        assertThat(submissionStatus, is(equalTo("Submitted")));
    }
}
