package uk.ac.ebi.subs.fileupload.eventhandlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.services.ValidationService;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PreCreateEventTest {

    private TUSFileInfo tusFileInfo;
    private static final String JWT_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1MTYzNjk4NTEsImV4cCI6MTU0NzkwNTg1MSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIm5hbWUiOiJLYXJlbCIsIkVtYWlsIjoia2FyZWxAZXhhbXBsZS5jb20iLCJEb21haW5zIjpbInRlYW1fYSIsInRlYW1fYiJdfQ.uGvNNVZZjb3CNc0zX5zj_QPz2pOAGZ7HQZbFeCU7a7g";
    private static final String SUBMISSION_UUID = "submission_1234";
    private static final String FILENAME = "test_file.cram";

    @MockBean
    private ValidationService validationService;

    @Autowired
    private EventHandlerService eventHandlerService;

    @Before
    public void setup() {
        tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_UUID, FILENAME);
    }

    @Test
    public void whenRequestIsInvalid_ShouldReturnHTTPStatusNotAcceptable() {
        given(this.validationService.validateFileUploadRequest(JWT_TOKEN, SUBMISSION_UUID)).willReturn(false);

        PreCreateEvent preCreateEvent = new PreCreateEvent();

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.NOT_ACCEPTABLE)));
    }

    @Test
    public void whenRequestIsValid_ShouldReturnHTTPStatusOK() {
        given(this.validationService.validateFileUploadRequest(JWT_TOKEN, SUBMISSION_UUID)).willReturn(true);

        PreCreateEvent preCreateEvent = new PreCreateEvent();

        ResponseEntity<Object> response = preCreateEvent.handle(tusFileInfo, eventHandlerService);

        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.OK)));
    }
}
