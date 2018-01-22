package uk.ac.ebi.subs.fileupload.services;

import uk.ac.ebi.subs.fileupload.errors.SubmissionNotFoundException;

import java.util.List;

/**
 * This class is responsible for provide information about {@code Submission}s.
 * Created by karoly on 18/12/2017.
 */
public interface SubmissionService {

    String getSubmissionStatus(String submissionId) throws SubmissionNotFoundException;

    boolean isModifiable(String submissionId) throws SubmissionNotFoundException;

    String getTeamNameBySubmissionId(String submissionId) throws SubmissionNotFoundException;

    boolean isUserAllowedToModifyGivenSubmission(String submissionId, List<String> teamNames);
}
