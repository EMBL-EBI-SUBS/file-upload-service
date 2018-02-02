package uk.ac.ebi.subs.fileupload.errors;

/**
 * This class holds error messages related to file upload events.
 */
public class ErrorMessages {

    public static final String INVALID_PARAMETERS = "Invalid parameters.";
    public static final String NOT_SUPPORTED_EVENT = "Not supported event.";
    public static final String FILE_DOCUMENT_NOT_FOUND = "File document is not found in the database.";
    public static final String FILE_CREATION_ERROR = "File could not be created.";
    public static final String
            DUPLICATED_FILE_ERROR = "File with name: %s has been already uploaded to the given submission (uuid = %s).";
}
