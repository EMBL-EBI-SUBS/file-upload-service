package uk.ac.ebi.subs.fileupload.security;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

/**
 *
 * See the original code concept at
 * <a href="https://github.com/EMBL-EBI-TSI/aap-client-java/blob/master/security/src/test/java/uk/ac/ebi/tsc/aap/client/security/JWTHelper.java">
 * JWTHelper class - AAP Team at EMBL-EBI</a>
 *
 * This is a slightly modified version of the above mentioned code.
 */
public class JWTHelper {

    @Value("${file-upload.security.issuer}")
    private static String issuer;


    public static String token() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        PrivateKey privateKey = keyGen.generateKeyPair().getPrivate();

        return token(privateKey, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
    }

    public static String token(PrivateKey privateKey, String alg) {
        long ttlMins = 5;
        String username = "user1";

        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        claims.setExpirationTimeMinutesInTheFuture(ttlMins);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setSubject(username);

        return build(claims, privateKey, alg);
    }

    public static String build(JwtClaims claims, PrivateKey privateKey, String alg) {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(privateKey);
        jws.setAlgorithmHeaderValue(alg);

        String token;
        try {
            token = jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new RuntimeException(e);
        }

        return token;
    }
}
