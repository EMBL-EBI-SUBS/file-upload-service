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
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

/**
 * This class is responsible for provide information about {@code Submission}s.
 * Created by karoly on 18/12/2017.
 */
@Service
public class SubmissionService {

    private RestTemplate restTemplate;

    @Value("${file-upload.subs-api.host}")
    private String serviceHost;
    @Value("${file-upload.subs-api.submissionStatusURI}")
    private String submissionStatusURI;
    @Value("${file-upload.subs-api.submissionURI}")
    private String submissionURI;

    public SubmissionService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

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

    public boolean isModifiable(String submissionUuid, String jwtToken) throws SubmissionNotFoundException {

        return getSubmissionStatus(submissionUuid, jwtToken).equals(SubmissionStatusEnum.Draft.name());
    }

    private HttpEntity<?> createRequestEntity(String jwtToken) {
        MultiValueMap<String,String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Bearer " + jwtToken);
        headers.add("Content-Type", MediaTypes.HAL_JSON_VALUE);
        headers.add("Accept", MediaTypes.HAL_JSON_VALUE);

        return new HttpEntity<>(headers);
    }
}
