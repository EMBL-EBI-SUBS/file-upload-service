package uk.ac.ebi.subs.fileupload.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.ValidationService;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusApiClient;
import uk.ac.ebi.subs.fileupload.services.globus.GlobusService;
import uk.ac.ebi.subs.fileupload.util.TUSEventType;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.validator.repository.ValidationResultRepository;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
@ComponentScan("uk.ac.ebi.subs.fileupload")
public class BadJSONRequestTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    private ValidationService validationService;

    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @MockBean
    private MappingMongoConverter mappingMongoConverter;

    @MockBean
    private ValidationResultRepository validationResultRepository;

    @MockBean
    private FileRepository fileRepository;

    @MockBean
    private SubmissionRepository submissionRepository;

    @MockBean
    private GlobusService globusService;
    @MockBean
    private GlobusApiClient globusApiClient;

    private static final String VALID_TOKEN = "valid.token.value";
    private static final String VALID_SUBMISSION_UUID = "12345";
    private static final String FILENAME = "test_file.cram";

    @Test
    public void whenNotCompatibleJSONBodySent_ThenValidationReturnsWithUnprocessableEntityStatus() throws Exception {
        TUSFileInfo tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(VALID_TOKEN, VALID_SUBMISSION_UUID, FILENAME);
        String eventName = TUSEventType.PRE_CREATE.getEventType();
        String json = objectMapper.writeValueAsString(tusFileInfo);
        json = json.replace("name", "filename");

        doReturn(new ResponseEntity<>(HttpStatus.OK)).when(this.validationService).validateFileUploadRequest(VALID_TOKEN, VALID_SUBMISSION_UUID);

        this.mockMvc.perform(post("/tusevent")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .header("Hook-Name", eventName)
        )
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.UNPROCESSABLE_ENTITY.value()))
                .andExpect(jsonPath("$.errors[0]").value(ErrorMessages.FILENAME_MANDATORY));;
    }

}
