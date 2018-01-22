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
import java.util.Arrays;
import java.util.List;

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
    @Value("${subs-api.submissionURI}")
    private String submissionURI;

    private static final String TEST_JWT_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1MTYzNjk4NTEsImV4cCI6MTU0NzkwNTg1MSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIm5hbWUiOiJLYXJlbCIsIkVtYWlsIjoia2FyZWxAZXhhbXBsZS5jb20iLCJEb21haW5zIjpbInRlYW1fYSIsInRlYW1fYiJdfQ.uGvNNVZZjb3CNc0zX5zj_QPz2pOAGZ7HQZbFeCU7a7g";

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private MockRestServiceServer server;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    // Tests for getSubmissionStatus method //

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

        submissionService.getSubmissionStatus(invalidSubmissionUuid, TEST_JWT_TOKEN);
    }

    @Test
    public void getSubmissionStatusByValidIdWithDraftStatusShouldReturnTheSubmissionStatus() throws IOException {
        String validDraftSubmissionUuid = "33334444-aaaa-bbbb-cccc-123456789012";

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submissionservice/draftSubmissionStatusByValidSubmissionId.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionStatusURI, serviceHost, validDraftSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
        );

        String submissionStatus = submissionService.getSubmissionStatus(validDraftSubmissionUuid, TEST_JWT_TOKEN);

        assertThat(submissionStatus, is(equalTo("Draft")));
    }

    @Test
    public void getSubmissionStatusByValidIdWithSubmittedStatusShouldReturnTheSubmissionStatus() throws Exception {
        String validSubmittedSubmissionUuid = "55556666-aaaa-bbbb-cccc-123456789012";

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submissionservice/submittedSubmissionStatusByValidSubmissionId.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionStatusURI, serviceHost, validSubmittedSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
                );

        String submissionStatus = submissionService.getSubmissionStatus(validSubmittedSubmissionUuid, TEST_JWT_TOKEN);

        assertThat(submissionStatus, is(equalTo("Submitted")));
    }

    @Test
    public void whenSubmissionStatusIsSubmittedThenSubmissionIsNotModifiable() throws Exception {
        String submittedSubmissionUuid = "55556666-aaaa-bbbb-cccc-123456789012";

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submissionservice/submittedSubmissionStatusByValidSubmissionId.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionStatusURI, serviceHost, submittedSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
                );

        assertThat(submissionService.isModifiable(submittedSubmissionUuid, TEST_JWT_TOKEN), is(false));
    }

    @Test
    public void whenSubmissionStatusIsDraftThenSubmissionIsModifiable() throws Exception {
        String draftSubmissionUuid = "33334444-aaaa-bbbb-cccc-123456789012";

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submissionservice/draftSubmissionStatusByValidSubmissionId.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionStatusURI, serviceHost, draftSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
                );

        assertThat(submissionService.isModifiable(draftSubmissionUuid, TEST_JWT_TOKEN), is(true));
    }


    // TEST for getTeamNameBySubmissionId method//

    @Test
    public void getSubmissionByInvalidIdShouldReturnSubmissionNotExistsError() {
        String invalidSubmissionUuid = "11112222-aaaa-bbbb-cccc-123456789012";
        String SUBMISSION_NOT_EXISTS_MESSAGE = "Submission not found with id: %s";

        this.server.expect(
                requestTo(String.format(submissionURI, serviceHost, invalidSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                );

        this.thrown.expect(SubmissionNotFoundException.class);
        this.thrown.expectMessage(String.format(SUBMISSION_NOT_EXISTS_MESSAGE, invalidSubmissionUuid));

        submissionService.getTeamNameBySubmissionId(invalidSubmissionUuid, TEST_JWT_TOKEN);
    }

    @Test
    public void getSubmissionByValidIdShouldReturnTheTeamNameTheSubmissionBelongsTo() throws IOException {
        String validDraftSubmissionUuid = "33334444-aaaa-bbbb-cccc-123456789012";

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submissionservice/submissionWithMatchingTeam.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionURI, serviceHost, validDraftSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
                );

        String teamName = submissionService.getTeamNameBySubmissionId(validDraftSubmissionUuid, TEST_JWT_TOKEN);

        assertThat(teamName, is(equalTo("BelaTeam")));
    }

    @Test
    public void whenUserDoesNotBelongToSameTeamAsTheSubmission_ThenUserNotAllowedToModifyIt() throws Exception {
        String validDraftSubmissionUuid = "33334444-aaaa-bbbb-cccc-123456789012";
        List<String> teamsOfUser = Arrays.asList("team_a", "team_b", "BelaTeam");

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submissionservice/submissionWithNotMatchingTeam.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionURI, serviceHost, validDraftSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
                );

        assertThat(submissionService.isUserAllowedToModifyGivenSubmission(validDraftSubmissionUuid, teamsOfUser, TEST_JWT_TOKEN), is(false));
    }

    @Test
    public void whenUserBelongsToSameTeamAsTheSubmission_ThenUserAllowedToModifyIt() throws Exception {
        String validDraftSubmissionUuid = "33334444-aaaa-bbbb-cccc-123456789012";
        List<String> teamsOfUser = Arrays.asList("team_a", "team_b", "BelaTeam");

        File submissionJson = new File(getClass().getClassLoader()
                .getResource("submissionservice/submissionWithMatchingTeam.json").getFile());
        String content = new String(Files.readAllBytes(submissionJson.toPath()));

        this.server.expect(
                requestTo(String.format(submissionURI, serviceHost, validDraftSubmissionUuid)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(content, MediaType.APPLICATION_JSON)
                );

        assertThat(submissionService.isUserAllowedToModifyGivenSubmission(validDraftSubmissionUuid, teamsOfUser, TEST_JWT_TOKEN), is(true));
    }
}
