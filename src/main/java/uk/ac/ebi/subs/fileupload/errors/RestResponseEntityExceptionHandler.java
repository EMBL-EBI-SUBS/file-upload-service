package uk.ac.ebi.subs.fileupload.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * This class contains the exception handlers for file upload requests.
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = InvalidTokenException.class)
    protected ResponseEntity<Object> handleInvalidToken(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        LOGGER.info("InvalidTokenException occurred:: " + bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                getContentTypeHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(value = SubmissionNotFoundException.class)
    protected ResponseEntity<Object> handleSubmissionNotFound(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        LOGGER.info("SubmissionNotFoundException occurred:: " + bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                getContentTypeHeaders(), HttpStatus.NOT_FOUND, request);
    }

    private HttpHeaders getContentTypeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaTypes.HAL_JSON);
        return headers;
    }
}
