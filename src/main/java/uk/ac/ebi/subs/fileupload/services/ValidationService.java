package uk.ac.ebi.subs.fileupload.services;

public interface ValidationService {

    /**
     * Check the validity of the file upload request.
     * First validate if the JWT token is valid, then check if the given submission exists and modifiable.
     *
     * @param jwt the JWT security token from the original TUS client's file upload request
     * @param submissionUuid the UUID of the submission the file would belong to
     * @return true if both of the above is valid, otherwise false
     */
    boolean validateFileUploadRequest(String jwt, String submissionUuid);
}
