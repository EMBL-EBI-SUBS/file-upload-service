package uk.ac.ebi.subs.fileupload.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.ValidationService;
import uk.ac.ebi.subs.fileupload.util.TUSEvent;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TUSEventController.class)
public class TUSEventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private ValidationService validationService;

    private static final String INVALID_TOKEN = "invalid.token.value";
    private static final String INVALID_SUBMISSION_UUID = "12345";
    private static final String VALID_TOKEN = "valid.token.value";
    private static final String VALID_SUBMISSION_UUID = "12345";

    @Test
    public void whenEventNameUnknownAndTokenInvalidOrSubmissionNotFound_ThenValidationFails() throws Exception {
        TUSFileInfo tusFileInfo = generateTUSFileInfo(INVALID_TOKEN, INVALID_SUBMISSION_UUID);
        String unknownEventName = "unknown event";
        String json = objectMapper.writeValueAsString(tusFileInfo);

        given(this.validationService.validateFileUploadRequest(INVALID_TOKEN, INVALID_SUBMISSION_UUID)).willReturn(false);

        this.mockMvc.perform(post("/tusevent")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .header("Hook-Name", unknownEventName)
        )
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.title").value(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_ACCEPTABLE.value()))
                .andExpect(jsonPath("$.errors[0]").value(ErrorMessages.NOT_SUPPORTED_EVENT));
    }

    @Test
    public void whenEventNameInvalidAndTokenValidAndSubmissionExists_ThenValidationFails() throws Exception {
        TUSFileInfo tusFileInfo = generateTUSFileInfo(VALID_TOKEN, VALID_SUBMISSION_UUID);
        String unknownEventName = "unknown event";
        String json = objectMapper.writeValueAsString(tusFileInfo);

        given(this.validationService.validateFileUploadRequest(VALID_TOKEN, VALID_SUBMISSION_UUID)).willReturn(true);

        this.mockMvc.perform(post("/tusevent")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .header("Hook-Name", unknownEventName)
        )
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.title").value(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_ACCEPTABLE.value()))
                .andExpect(jsonPath("$.errors[0]").value(ErrorMessages.NOT_SUPPORTED_EVENT));
    }

    @Test
    public void whenEventNameValidAndTokenInvalidOrSubmissionNotFound_ThenValidationFails() throws Exception {
        TUSFileInfo tusFileInfo = generateTUSFileInfo(INVALID_TOKEN, INVALID_SUBMISSION_UUID);
        String eventName = TUSEvent.PRE_CREATE.getEventType();
        String json = objectMapper.writeValueAsString(tusFileInfo);

        given(this.validationService.validateFileUploadRequest(INVALID_TOKEN, INVALID_SUBMISSION_UUID)).willReturn(false);

        this.mockMvc.perform(post("/tusevent")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .header("Hook-Name", eventName)
        )
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.title").value(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_ACCEPTABLE.value()))
                .andExpect(jsonPath("$.errors[0]").value("Invalid parameters"));
    }

    @Test
    public void whenEventNameValidAndTokenValidAndSubmissionExists_ThenValidationSucceed() throws Exception {
        TUSFileInfo tusFileInfo = generateTUSFileInfo(VALID_TOKEN, VALID_SUBMISSION_UUID);
        String eventName = TUSEvent.PRE_CREATE.getEventType();
        String json = objectMapper.writeValueAsString(tusFileInfo);

        given(this.validationService.validateFileUploadRequest(VALID_TOKEN, VALID_SUBMISSION_UUID)).willReturn(true);

        this.mockMvc.perform(post("/tusevent")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .header("Hook-Name", eventName)
        )
                .andDo(print())
                .andExpect(status().isOk());
    }

    private TUSFileInfo generateTUSFileInfo(String jwtToken, String sunmissionId) {
        TUSFileInfo fileInfo = new TUSFileInfo();
        fileInfo.setMetadata(
                TUSFileInfo.buildMetaData("test file name", sunmissionId, jwtToken));

        return fileInfo;
    }
}
