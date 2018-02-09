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
    protected ResponseEntity<Object> handleInvalidToken(RuntimeException ex) {
        String exceptionMessage = ex.getMessage();
        ResponseEntity<Object> errorResponse = ErrorResponse.assemble(HttpStatus.UNAUTHORIZED, exceptionMessage);
        LOGGER.info("InvalidTokenException occurred:: " + exceptionMessage);
        return errorResponse;
    }

    @ExceptionHandler(value = SubmissionNotFoundException.class)
    protected ResponseEntity<Object> handleSubmissionNotFound(RuntimeException ex) {
        String exceptionMessage = ex.getMessage();
        ResponseEntity<Object> errorResponse = ErrorResponse.assemble(HttpStatus.NOT_FOUND, exceptionMessage);
        LOGGER.info("SubmissionNotFoundException occurred:: " + exceptionMessage);
        return errorResponse;
    }
}
