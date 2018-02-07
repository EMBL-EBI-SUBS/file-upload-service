package uk.ac.ebi.subs.fileupload.services;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.fileupload.errors.InvalidTokenException;

import javax.annotation.PostConstruct;

/**
 * Verify token validity.
 *
 * See the original code concept at
 * <a href="https://github.com/EMBL-EBI-TSI/aap-client-java/blob/master/security/src/main/java/uk/ac/ebi/tsc/aap/client/security/TokenHandler.java">
 * TokenHandler class - AAP Team at EMBL-EBI</a>
 *
 * This is a slightly modified version of the above mentioned code.
 */
@Service
public class DefaultTokenHandlerService implements TokenHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTokenHandlerService.class);

    public JwtConsumer jwtConsumer;

    @Value("${file-upload.security.certificatePath}")
    private String certificatePath;

    @PostConstruct
    public void initPropertyDependentFields() throws Exception {
        LOGGER.trace("initPropertyDependentFields- certificatePath***** {}", certificatePath);
        jwtConsumer = configureJwtConsumer(certificatePath);
    }

    @Override
    public void setJwtConsumer(JwtConsumer jwtConsumer) {
        this.jwtConsumer = jwtConsumer;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            jwtConsumer.processToClaims(token);
        } catch (InvalidJwtException e ) {
            throw new InvalidTokenException("Cannot parse token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse token: "+e.getMessage(), e);
        }

        return true;
    }
}