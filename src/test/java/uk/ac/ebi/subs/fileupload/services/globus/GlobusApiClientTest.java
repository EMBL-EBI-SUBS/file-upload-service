package uk.ac.ebi.subs.fileupload.services.globus;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.config.Config;

@Ignore
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {Config.class, GlobusApiClient.class}, initializers = ConfigFileApplicationContextInitializer.class)
@TestPropertySource(locations="classpath:application.yml")
public class GlobusApiClientTest {

    @Autowired
    private GlobusApiClient globusApiClient;

    @Test
    public void test() {
        Assert.assertNotNull(globusApiClient);
    }
}
