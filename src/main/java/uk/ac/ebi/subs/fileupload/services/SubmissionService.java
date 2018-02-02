package uk.ac.ebi.subs.fileupload.services;

import uk.ac.ebi.subs.fileupload.errors.SubmissionNotFoundException;

import java.util.List;

/**
 * This class is responsible for provide information about {@code Submission}s.
 * Created by karoly on 18/12/2017.
 */
public interface SubmissionService {

    String getSubmissionStatus(String submissionId, String jwtToken) throws SubmissionNotFoundException;

    boolean isModifiable(String submissionId, String jwtToken) throws SubmissionNotFoundException;

    String getTeamNameBySubmissionId(String submissionId, String jwtToken) throws SubmissionNotFoundException;

    boolean isUserAllowedToModifyGivenSubmission(String submissionId, List<String> teamNames, String jwtToken);
}
