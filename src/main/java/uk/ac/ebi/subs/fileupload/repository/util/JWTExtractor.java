package uk.ac.ebi.subs.fileupload.repository.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

    public List<String> getUserDomains() {
        List<String> domains = new ArrayList<>();
        JSONArray domainJsonArray = payload.getJSONArray("Domains");
        int domainCount = domainJsonArray.length();

        for (int i = 0; i < domainCount; i++) {
            domains.add(domainJsonArray.getString(i));
        }

        return domains;
    }
}
