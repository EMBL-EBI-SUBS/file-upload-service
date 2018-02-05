package uk.ac.ebi.subs.fileupload.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * This is a utility class to assemble a HTTP Error response with the given HTTP status code and error message.
 */
public final class ErrorResponse {

    public static ResponseEntity<Object> assemble(HttpStatus httpStatus, String errorMessage) {
        FileApiError fileApiError = new FileApiError(httpStatus, errorMessage);
        return new ResponseEntity<>(fileApiError, httpStatus);
    }
}
