package uk.ac.ebi.subs.fileupload.errors;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

/**
 * The payload of an File API error response.
 */
public class FileApiError {

    /**
     * A brief title for the error status.
     */
    private String title;

    /**
     * The HTTP status code for the current request.
     */
    private int status;

    /**
     * Error details specific to this request (optional).
     */
    private List<String> errors;

    public FileApiError() {}

    public FileApiError(HttpStatus httpStatus) {
        this.title = httpStatus.getReasonPhrase();
        this.status = httpStatus.value();
    }

    public FileApiError(HttpStatus httpStatus, List<String> errors) {
        this.title = httpStatus.getReasonPhrase();
        this.status = httpStatus.value();
        this.errors = errors;
    }

    public FileApiError(HttpStatus httpStatus, String error) {
        this.title = httpStatus.getReasonPhrase();
        this.status = httpStatus.value();
        this.errors = Arrays.asList(error);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void setError(String error) {
        this.errors = Arrays.asList(error);
    }
}
