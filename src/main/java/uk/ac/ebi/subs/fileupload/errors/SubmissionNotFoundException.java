package uk.ac.ebi.subs.fileupload.errors;

public class SubmissionNotFoundException extends RuntimeException {

    private static final String SUBMISSION_NOT_EXISTS_MESSAGE = "Submission not found with id: %s";

    public SubmissionNotFoundException(String submissionUuid) {
        super(String.format(SUBMISSION_NOT_EXISTS_MESSAGE, submissionUuid));
    }
}
