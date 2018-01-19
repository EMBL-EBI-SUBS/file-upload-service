package uk.ac.ebi.subs.fileupload.repository.util;

import org.json.JSONObject;

import java.util.Base64;

/**
 * This is a helper class to extract values from JWT token's payload.
 */
public class JWTExtractor {

    private String jwtToken;

    private JSONObject payload;

    private static final String USERNAME_KEY = "name";

    public JWTExtractor(String jwtToken) {
        this.jwtToken = jwtToken;

        String payloadBase64 = jwtToken.split("\\.")[1];

        payload = new JSONObject(new String(Base64.getDecoder().decode(payloadBase64)));
    }

    public String getUsername() {
        return payload.getString(USERNAME_KEY);
    }
}
