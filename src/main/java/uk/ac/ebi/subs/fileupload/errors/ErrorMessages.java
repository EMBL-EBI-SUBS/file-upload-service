package uk.ac.ebi.subs.fileupload.errors;

/**
 * This class holds error messages related to file upload events.
 */
public class ErrorMessages {

    public static final String NOT_SUPPORTED_EVENT = "Not supported event.";
    public static final String UNAUTHORIZED_REQUEST = "The user is not authorized to send this request.";


    public static final String FILE_DOCUMENT_NOT_FOUND = "File document with tusId: %s is not found in the database.";
    public static final String FILE_CREATION_ERROR = "File could not be created.";
    public static final String
            DUPLICATED_FILE_ERROR = "File with name: %s has been already uploaded to the given submission (uuid = %s).";

    public static final String SUBMISSION_NOT_MODIFIABLE = "The submission with id: %s is not modifiable";

    public static final String SUBMISSION_ID_MANDATORY = "It is mandatory to send the submission ID in the metadata of the request.";
    public static final String JWT_TOKEN_MANDATORY = "It is mandatory to send the authorization token (JWT) in the metadata of the request.";
    public static final String FILENAME_MANDATORY = "It is mandatory to send the filename in the metadata of the request.";

    public static final String INVALID_JWT_TOKEN = "The authorization token (JWT) is invalid";
    public static final String NOT_ENOUGH_DISKSPACE = "The file storage has not enough usable disk space to store the file: %s";
}
