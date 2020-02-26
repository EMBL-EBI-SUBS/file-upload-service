package uk.ac.ebi.subs.fileupload.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.subs.fileupload.services.ValidationService;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusApiClient;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusService;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
@ComponentScan("uk.ac.ebi.subs.fileupload")
public class HealthEndpointCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ValidationService validationService;

    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @MockBean
    private FileRepository fileRepository;

    @MockBean
    private GlobusApiClient globusApiClient;

    @MockBean
    private GlobusService globusService;

    @Test
    public void emulateCorsPreflight() throws IOException, Exception {

        this.mockMvc.perform(
                options("/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "GET")
        )
                .andDo(print())
                .andExpect(status().is2xxSuccessful());

    }
}
