package uk.ac.ebi.subs.fileupload.errors;

public class FileNotFoundException extends RuntimeException {

    public static final String FILE_NOT_FOUND_MESSAGE = "The following file is not found: %s";

    public FileNotFoundException(String message) {
        super(message);
    }
}
