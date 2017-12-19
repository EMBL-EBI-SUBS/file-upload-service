package uk.ac.ebi.subs.fileupload.services;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.subs.fileupload.errors.InvalidTokenException;
import uk.ac.ebi.subs.fileupload.security.JWTHelper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;

import static org.mockito.Matchers.contains;

/**
 * @author Amelie Cornelis  <ameliec@ebi.ac.uk>
 * @since 21/07/2016.
 *
 * @author Karoly Erdos     <karoly@ebi.ac.uk>
 */
public class TokenHandlerServiceTest {

    private TokenHandlerService subject;

    static final private String username = "user1";
    private PrivateKey signingKey;
    private PublicKey verifyingKey;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        KeyPair testKeyPair = keyGen.generateKeyPair();
        signingKey = testKeyPair.getPrivate();
        verifyingKey = testKeyPair.getPublic();
        subject = new DefaultTokenHandlerService();
        subject.setJwtConsumer(subject.configureJwtConsumer(verifyingKey));
    }

    @Test
    public void whenTokenIsValid_thenValidationSucceed() {
        JwtClaims claims = minClaims();
        claims.setClaim("name", "Alice Wonderland");
        String validToken = JWTHelper.build(claims, signingKey, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

        subject.validateToken(validToken);
    }

    @Test
    public void whenTokenIsTampered_thenValidationShouldThrowException() throws Exception {
        this.thrown.expect(InvalidTokenException.class);
        this.thrown.expectMessage(contains("Cannot parse token"));

        String validToken = JWTHelper.token();
        String encodedPayload = validToken.split("\\.")[1];
        String decodedPayload = new String(Base64.getDecoder().decode(encodedPayload));
        String tamperedPayload = decodedPayload.replace(username, "bob");
        String tamperedEncodedPayload = new String(Base64.getEncoder().encode(tamperedPayload.getBytes()));
        String tamperedToken = validToken.replace(encodedPayload, tamperedEncodedPayload);

        subject.validateToken(tamperedToken);
    }

    @Test
    public void whenTokenSignedWithDifferentPrivateKey_thenValidationShouldThrowException() throws Exception {
        this.thrown.expect(InvalidTokenException.class);
        this.thrown.expectMessage(contains("Cannot parse token"));

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        PrivateKey privateKey = keyGen.generateKeyPair().getPrivate();
        String tokenFromUntrusted = JWTHelper.token(privateKey, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

        subject.validateToken(tokenFromUntrusted);
    }

    @Test
    public void whenTokenSignedWithDifferentAlgorithm_thenValidationShouldThrowException() throws Exception {
        this.thrown.expect(InvalidTokenException.class);
        this.thrown.expectMessage(contains("Cannot parse token"));

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        PrivateKey privateKey = keyGen.generateKeyPair().getPrivate();
        String tokenFromUntrusted = JWTHelper.token(privateKey, AlgorithmIdentifiers.RSA_USING_SHA256);

        subject.validateToken(tokenFromUntrusted);
    }

    @Test
    public void whenTokemExpired_thenValidationShouldThrowException() {
        this.thrown.expect(InvalidTokenException.class);
        this.thrown.expectMessage(contains("Cannot parse token"));

        String expiredToken = expiredToken();
        subject.validateToken(expiredToken);
    }

    private String expiredToken() {
        // past must be earlier than skew allowed by TokenHandler jwtConsumer
        long past = - (60 * 1000);
        JwtClaims claims = minClaims(past);
        return JWTHelper.build(claims, signingKey, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
    }

    private JwtClaims minClaims() {
        return minClaims(2 * 1000);
    }

    private JwtClaims minClaims(long expiresIn) {
        long now = new Date().getTime();
        NumericDate expiry = NumericDate.fromMilliseconds(now + expiresIn);
        JwtClaims claims = new JwtClaims();
        claims.setSubject(username);
        claims.setExpirationTime( expiry);
        return claims;
    }
}
