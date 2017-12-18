package uk.ac.ebi.subs.fileupload.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.subs.data.status.SubmissionStatus;
import uk.ac.ebi.subs.fileupload.errors.SubmissionNotExistsException;

@Service
public class DefaultSubmissionService implements SubmissionService {

    private RestTemplate restTemplate;

    @Value("${subs-api.host}")
    private String serviceHost;
    @Value("${subs-api.submissionStatusURI}")
    private String submissionStatusURI;

    public DefaultSubmissionService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public String getSubmissionStatus(String submissionUuid) throws SubmissionNotExistsException {
        ResponseEntity<SubmissionStatus> submissionStatusResponse;
        try {
            submissionStatusResponse = restTemplate.getForEntity(
                    String.format(submissionStatusURI, serviceHost, submissionUuid), SubmissionStatus.class);
        } catch (HttpClientErrorException httpClientException) {
            if (httpClientException.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new SubmissionNotExistsException();
            } else {
                throw httpClientException;
            }
        }

        return submissionStatusResponse.getBody().getStatus();
    }
}
