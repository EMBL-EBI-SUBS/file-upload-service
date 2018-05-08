package uk.ac.ebi.subs.fileupload.errors;

public class FileDeletionException extends RuntimeException {

    private static final String FILE_DELETION_ERROR_HAS_HAPPENED = "Something went wrong when deleting the file: [%s]";

    public FileDeletionException(String message) {
        super(message);
    }

    public static FileDeletionException byTargetPath(String ...messageParams) {
        return new FileDeletionException(
                String.format(FILE_DELETION_ERROR_HAS_HAPPENED, messageParams[0]));
    }
}
