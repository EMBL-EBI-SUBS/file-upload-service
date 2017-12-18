package uk.ac.ebi.subs.fileupload.errors;

public class SubmissionNotExistsException extends RuntimeException {

    private static final String SUBMISSION_NOT_EXISTS_MESSAGE = "The submission you tried to query is not exists";

    public SubmissionNotExistsException() {
        super(SUBMISSION_NOT_EXISTS_MESSAGE);
    }
}
