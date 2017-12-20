package uk.ac.ebi.subs.fileupload.services;

import uk.ac.ebi.subs.fileupload.errors.SubmissionNotFoundException;

/**
 * This class is responsible for provide information about {@code Submission}s.
 * Created by karoly on 18/12/2017.
 */
public interface SubmissionService {

    String getSubmissionStatus(String submissionUuid) throws SubmissionNotFoundException;

    boolean isModifiable(String submissionUuid) throws SubmissionNotFoundException;
}
