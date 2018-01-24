package uk.ac.ebi.subs.fileupload.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.subs.data.status.SubmissionStatusEnum;
import uk.ac.ebi.subs.fileupload.errors.SubmissionNotFoundException;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

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
    public String getSubmissionStatus(String submissionUuid, String jwtToken) throws SubmissionNotFoundException {
        ResponseEntity<SubmissionStatus> submissionStatusResponse;
        try {
            submissionStatusResponse = restTemplate.exchange(
                    String.format(submissionStatusURI, serviceHost, submissionUuid),
                    HttpMethod.GET,
                    createRequestEntity(jwtToken),
                    SubmissionStatus.class);
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
    public boolean isModifiable(String submissionUuid, String jwtToken) throws SubmissionNotFoundException {

        return getSubmissionStatus(submissionUuid, jwtToken).equals(SubmissionStatusEnum.Draft.name());
    }

    @Override
    public String getTeamNameBySubmissionId(String submissionId, String jwtToken) throws SubmissionNotFoundException {
        ResponseEntity<Submission> submissionResponse;
        try {
            submissionResponse = restTemplate.exchange(
                    String.format(submissionURI, serviceHost, submissionId),
                    HttpMethod.GET,
                    createRequestEntity(jwtToken),
                    Submission.class);
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
    public boolean isUserAllowedToModifyGivenSubmission(String submissionId, List<String> teamNames, String jwtToken) {
        String teamNameBySubmissionId = getTeamNameBySubmissionId(submissionId, jwtToken);

        return teamNames.contains(teamNameBySubmissionId);
    }

    private HttpEntity<?> createRequestEntity(String jwtToken) {
        MultiValueMap<String,String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Bearer " + jwtToken);
        headers.add("Content-Type", MediaTypes.HAL_JSON_VALUE);
        headers.add("Accept", MediaTypes.HAL_JSON_VALUE);

        return new HttpEntity<>(headers);
    }
}
