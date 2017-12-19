package uk.ac.ebi.subs.fileupload.security;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.fileupload.errors.InvalidTokenException;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Verify token validity.
 *
 * @author Amelie Cornelis  <ameliec@ebi.ac.uk>
 * @since 18/07/2016.
 *
 * @author Karoly Erdos     <karoly@ebi.ac.uk>
 */
@Component
public class TokenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenHandler.class);

    public JwtConsumer jwtConsumer;
    @Value("${jwt.certificate}")
    private String certificatePath;

    @PostConstruct
    public void initPropertyDependentFields() throws Exception {
        LOGGER.trace("initPropertyDependentFields- certificatePath***** {}", certificatePath);
        setJwtConsumer(certificatePath);
    }

    void setJwtConsumer(String path) throws Exception {
        InputStream inputStream = new DefaultResourceLoader().getResource(path).getInputStream();
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
        final PublicKey verifyingKey = certificate.getPublicKey();
        setJwtConsumer(verifyingKey);
    }

    void setJwtConsumer(PublicKey verifyingKey) {
        jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setVerificationKey(verifyingKey)
                .setRelaxVerificationKeyValidation()
                .build();
    }

    public void validateToken(String token) {
        try {
            jwtConsumer.processToClaims(token);
        } catch (InvalidJwtException e ) {
            throw new InvalidTokenException("Cannot parse token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse token: "+e.getMessage(), e);
        }
    }

}