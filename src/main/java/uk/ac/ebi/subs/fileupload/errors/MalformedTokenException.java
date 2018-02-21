package uk.ac.ebi.subs.fileupload.errors;

public class MalformedTokenException extends RuntimeException {

    public MalformedTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
