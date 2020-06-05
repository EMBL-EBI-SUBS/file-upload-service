package uk.ac.ebi.subs.fileupload.util;

public class Utils {

    public static String generateFolderName(String submissionUUID) {

        return String.join(System.getProperty("file.separator"),
                submissionUUID.substring(0, 1),
                submissionUUID.substring(1, 2),
                submissionUUID);
    }
}
