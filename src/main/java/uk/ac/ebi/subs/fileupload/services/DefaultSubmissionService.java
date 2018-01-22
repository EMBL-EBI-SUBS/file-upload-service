package uk.ac.ebi.subs.fileupload.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.subs.data.status.SubmissionStatus;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.fileupload.errors.SubmissionNotFoundException;
import uk.ac.ebi.subs.repository.model.Submission;

import java.util.List;

@Service
public class DefaultSubmissionService implements SubmissionService {

    private RestTemplate restTemplate;

    @Value("${subs-api.host}")
    private String serviceHost;
    @Value("${subs-api.submissionStatusURI}")
    private String submissionStatusURI;
    @Value("${subs-api.submissionURI}")
    private String submissionURI;

    public DefaultSubmissionService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public String getSubmissionStatus(String submissionUuid) throws SubmissionNotFoundException {
        ResponseEntity<SubmissionStatus> submissionStatusResponse;
        try {
            submissionStatusResponse = restTemplate.getForEntity(
                    String.format(submissionStatusURI, serviceHost, submissionUuid), SubmissionStatus.class);
        } catch (HttpClientErrorException httpClientException) {
            if (httpClientException.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new SubmissionNotFoundException(submissionUuid);
            } else {
                throw httpClientException;
            }
        }

        return submissionStatusResponse.getBody().getStatus();
    }

    @Override
    public boolean isModifiable(String submissionUuid) throws SubmissionNotFoundException {

        return getSubmissionStatus(submissionUuid).equals(SubmissionStatusEnum.Draft.name());
    }

    @Override
    public String getTeamNameBySubmissionId(String submissionId) throws SubmissionNotFoundException {
        ResponseEntity<Submission> submissionResponse;
        try {
            submissionResponse = restTemplate.getForEntity(
                    String.format(submissionURI, serviceHost, submissionId), Submission.class);
        } catch (HttpClientErrorException httpClientException) {
            if (httpClientException.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new SubmissionNotFoundException(submissionId);
            } else {
                throw httpClientException;
            }
        }

        return submissionResponse.getBody().getTeam().getName();
    }

    @Override
    public boolean isUserAllowedToModifyGivenSubmission(String submissionId, List<String> teamNames) {
        String teamNameBySubmissionId = getTeamNameBySubmissionId(submissionId);

        return teamNames.contains(teamNameBySubmissionId);
    }
}
