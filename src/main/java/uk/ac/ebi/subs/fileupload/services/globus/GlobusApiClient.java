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

            LOGGER.debug("Creating shared endpoint. HostPath : {}, DisplayName : {}", hostPath, displayName);

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        };

        ResponseEntity<ObjectNode> respEntity = execute(op);
        if (respEntity.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
             ObjectNode resp = respEntity.getBody();
             if (resp.has("code") && resp.get("code").asText().equals("ClientError.ActivationRequired")) {
                 LOGGER.debug("Endpoint activation required. HostPath : {}, DisplayName : {}", hostPath, displayName);
                 activateEndpoint();
                 respEntity = execute(op);
                 if (!respEntity.getStatusCode().equals(HttpStatus.CREATED)) {
                     throw new RuntimeException(buildExceptionError(respEntity));
                 }
             } else {
                 throw new RuntimeException(buildExceptionError(respEntity));
             }
        } else if (!respEntity.getStatusCode().equals(HttpStatus.CREATED)) {
            throw new RuntimeException(buildExceptionError(respEntity));
        }

        String sharedEndpointId = respEntity.getBody().get("id").asText();

        LOGGER.debug("Shared endpoint created. HostPath : {}, DisplayName : {}, SharedEndpointID : {}",
                hostPath, displayName, sharedEndpointId);

        return sharedEndpointId;
    }

    public void deleteEndpoint(String endpointId) {
        ResponseEntity<ObjectNode> resp = execute(() -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.DELETE,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "endpoint/" + endpointId));

            LOGGER.debug("Deleting shared endpoint : {}", endpointId);

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        });

        if (!resp.getStatusCode().equals(HttpStatus.OK)) {
            throw new RuntimeException(buildExceptionError(resp));
        } else {
            LOGGER.debug("Shared endpoint deleted : {}", endpointId);
        }
    }

    public void addAllAuthenticatedUsersACLToEndpoint(String endpointId) {
        ResponseEntity<ObjectNode> respEntity = execute(() -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            HashMap<String, String> req = new HashMap<>();
            req.put("DATA_TYPE", "access");
            req.put("principal_type", "all_authenticated_users");
            req.put("principal", "");
            req.put("path", "/");
            req.put("permissions", "rw");

            RequestEntity<HashMap<String, String>> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "endpoint/" + endpointId + "/access"));

            LOGGER.debug("Adding all_authenticated_users ACL to endpoint : {}", endpointId);

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        });

        if (!respEntity.getStatusCode().equals(HttpStatus.CREATED)) {
            throw new RuntimeException(buildExceptionError(respEntity));
        } else {
            LOGGER.debug("ACL all_authenticated_users added to endpoint : {}", endpointId);
        }
    }

    private String getRefreshedAccessToken() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));

        RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.POST,
                getUri(authApiUrl, AUTH_API_VERSION_2, "oauth2/token?grant_type=refresh_token&refresh_token=" + transferRefreshToken));

        LOGGER.debug("Refreshing access token.");

        ResponseEntity<ObjectNode> resp = restTemplate.exchange(requestEntity, ObjectNode.class);
        if (resp.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException(buildExceptionError(resp));
        }

        LOGGER.debug("Refreshed access token acquired.");

        return resp.getBody().get("access_token").asText();
    }

    private void activateEndpoint() {
        ObjectNode req = createActivationObject(getActivationRequirements());

        ResponseEntity<ObjectNode> respEntity = execute(() -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            RequestEntity<ObjectNode> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "endpoint/" + hostEndpoint + "/activate"));

            LOGGER.debug("Activating host endpoint : {}", hostEndpoint);

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        });

        if (!respEntity.getStatusCode().equals(HttpStatus.OK)) {
            throw new RuntimeException(buildExceptionError(respEntity));
        } else {
            LOGGER.debug("Host endpoint activated : {}", hostEndpoint);
        }
    }

    private ObjectNode getActivationRequirements() {
        ResponseEntity<ObjectNode> respEntity = execute(() -> {
            MultiValueMap<String, String> headers = buildAuthorizationHeader();

            RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET,
                    getUri(transferApiUrl, TRANSFER_API_VERSION_0_10, "endpoint/" + hostEndpoint + "/activation_requirements"));

            LOGGER.debug("Fetching activation requirements for host endpoint : {}", hostEndpoint);

            return restTemplate.exchange(requestEntity, ObjectNode.class);
        });

        if (respEntity.getStatusCodeValue() != 200) {
            throw new RuntimeException(buildExceptionError(respEntity));
        }

        LOGGER.debug("Activation requirements fetched for host endpoint : {}", hostEndpoint);

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

            LOGGER.debug("Authentication failed. Token may have been expired. Response : {}", resp.getBody().toString());

            transferAccessToken = getRefreshedAccessToken();
            resp = operation.get();
        }

        return resp;
    }

    @PostConstruct
    private void init() {
        LOGGER.debug("Acquiring access token at startup.");
        try {
            transferAccessToken = getRefreshedAccessToken();
        } catch (Exception ex) {
            LOGGER.error("Error getting access token.", ex);
        }
    }
}
