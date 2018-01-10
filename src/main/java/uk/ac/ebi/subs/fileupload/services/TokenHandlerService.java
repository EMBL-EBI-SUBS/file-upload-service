package uk.ac.ebi.subs.fileupload.services;

import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This class responsible for validating the JWT token.
 */
public interface TokenHandlerService {

    boolean validateToken(String token);

    default JwtConsumer configureJwtConsumer(String path) throws Exception {
        InputStream inputStream = new DefaultResourceLoader().getResource(path).getInputStream();
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
        final PublicKey verifyingKey = certificate.getPublicKey();

        return configureJwtConsumer(verifyingKey);
    }

    default JwtConsumer configureJwtConsumer(PublicKey verifyingKey) {
        return new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setVerificationKey(verifyingKey)
                .setRelaxVerificationKeyValidation()
                .build();
    }

    void setJwtConsumer(JwtConsumer jwtConsumer);
}
