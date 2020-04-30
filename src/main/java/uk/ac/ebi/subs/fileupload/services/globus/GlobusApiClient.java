package uk.ac.ebi.subs.fileupload.services.globus;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.function.Supplier;

@Service
public class GlobusApiClient {

    private static final String AUTH_API_VERSION_2 = "v2";
    private static final String TRANSFER_API_VERSION_0_10 = "v0.10";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobusApiClient.class);

    @Value("${file-upload.globus.url.auth}")
    private String authApiUrl;

    @Value("${file-upload.globus.url.transfer}")
    private String transferApiUrl;

    @Value("${file-upload.globus.client.id}")
    private String clientId;
    @Value("${file-upload.globus.client.secret}")
    private String clientSecret;

    @Value("${file-upload.globus.transferRefreshToken}")
    private String transferRefreshToken;

    @Value("${file-upload.globus.hostEndpoint.id}")
    private String hostEndpoint;

    @Value("${file-upload.globus.hostEndpoint.activation.username}")
    private String endpointActivationUsername;
    @Value("${file-upload.globus.hostEndpoint.activation.password}")
    private String endpointActivationPassword;

    private String transferAccessToken;

    @Autowired
    private RestTemplate restTemplate;

    public String createShare(String hostPath, String displayName, String description) {
        Supplier<ResponseEntity<ObjectNode>> op = () -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            HashMap<String, String> req = new HashMap<>();
            req.put("DATA_TYPE", "shared_endpoint");
            req.put("display_name", displayName);
            req.put("host_endpoint", hostEndpoint);
            req.put("host_path", hostPath);
            req.put("description", description);

            RequestEntity<HashMap<String, String>> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "shared_endpoint"));

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        };

        ResponseEntity<ObjectNode> respEntity = execute(op);
        if (respEntity.getStatusCodeValue() == 400) {
             ObjectNode resp = respEntity.getBody();
             if (resp.has("code") && resp.get("code").equals("ClientError.ActivationRequired")) {
                 activateEndpoint();
                 respEntity = execute(op);
                 if (!respEntity.getStatusCode().equals(HttpStatus.CREATED)) {
                     throw new RuntimeException(buildExceptionError(respEntity));
                 }
             } else {
                 throw new RuntimeException(buildExceptionError(respEntity));
             }
        }

        return respEntity.getBody().get("id").asText();
    }

    public void deleteEndpoint(String endpointId) {
        ResponseEntity<ObjectNode> resp = execute(() -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.DELETE,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "endpoint/" + endpointId));

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        });

        if (!resp.getStatusCode().equals(HttpStatus.OK)) {
            throw new RuntimeException(buildExceptionError(resp));
        }
    }

    private String getRefreshedAccessToken() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));

        RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.POST,
                getUri(authApiUrl, AUTH_API_VERSION_2, "oauth2/token?grant_type=refresh_token&refresh_token=" + transferRefreshToken));

        ResponseEntity<ObjectNode> resp = restTemplate.exchange(requestEntity, ObjectNode.class);
        if (resp.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(buildExceptionError(resp));
        }

        return resp.getBody().get("access_token").asText();
    }

    private void activateEndpoint() {
        ObjectNode req = createActivationObject(getActivationRequirements());

        ResponseEntity<ObjectNode> respEntity = execute(() -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            RequestEntity<ObjectNode> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "endpoint/" + hostEndpoint + "/activate"));

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        });

        if (!respEntity.getStatusCode().equals(HttpStatus.OK)) {
            throw new RuntimeException(buildExceptionError(respEntity));
        }
    }

    private ObjectNode getActivationRequirements() {
        ResponseEntity<ObjectNode> respEntity = execute(() -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "endpoint/" + hostEndpoint + "/activation_requirements"));

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        });

        if (respEntity.getStatusCodeValue() != 200) {
            throw new RuntimeException(buildExceptionError(respEntity));
        }

        return respEntity.getBody();
    }

    private ObjectNode createActivationObject(ObjectNode activationRequirements) {
        ObjectNode res = activationRequirements.objectNode();
        res.put("DATA_TYPE", "activation_requirements");

        ArrayNode data = res.putArray("DATA");

        activationRequirements.withArray("DATA").forEach(jsonNode -> {
            String name = jsonNode.get("name").asText();
            if (name.equals("hostname") || name.equals("server_dn")) {
                data.add(jsonNode);
            } else if (name.equals("username")) {
                ((ObjectNode)jsonNode).put("value", endpointActivationUsername);
                data.add(jsonNode);
            } else if (name.equals("passphrase")) {
                ((ObjectNode)jsonNode).put("value", endpointActivationPassword);
                data.add(jsonNode);
            }
        });

        return res;
    }

    private MultiValueMap<String, String> buildAuthorizationHeader() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Bearer " + transferAccessToken);

        return headers;
    }

    private URI getUri(String baseUrl, String apiVersion, String path) {
        try {
            URL url = new URL(new URL(baseUrl), apiVersion + "/");
            url = new URL(url, url.getPath() + path);

            return url.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildExceptionError(ResponseEntity<ObjectNode> responseEntity) {
        if (responseEntity.getBody() != null) {
            return String.format("Status : %s, Response : %s", responseEntity.getStatusCode(), responseEntity.getBody().toString());
        } else {
            return String.format("Status : %s", responseEntity.getStatusCode());
        }
    }

    /**
     * Execute the given operation. If the operation fails due to expired token then the token is refreshed and the
     * operation is executed again.
     *
     * @param operation
     * @return
     */
    private ResponseEntity<ObjectNode> execute(Supplier<ResponseEntity<ObjectNode>> operation) {
        ResponseEntity<ObjectNode> resp = operation.get();
        if (resp.getStatusCodeValue() == 401
                && resp.getBody() != null
                && resp.getBody().get("code").asText().equals("AuthenticationFailed")) {

            transferAccessToken = getRefreshedAccessToken();
            resp = operation.get();
        }

        return resp;
    }

    @PostConstruct
    private void init() {
        try {
            transferAccessToken = getRefreshedAccessToken();
        } catch (Exception ex) {
            LOGGER.error("Error getting access token.", ex);
        }
    }
}
