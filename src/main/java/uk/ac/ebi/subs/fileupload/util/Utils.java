package uk.ac.ebi.subs.fileupload.util;

public class Utils {

    public static String generateFolderName(String submissionUUID) {
        String separator = System.getProperty("file.separator");

        StringBuilder folderName = new StringBuilder();
        folderName.append(submissionUUID.substring(0, 1));
        folderName.append(separator);
        folderName.append(submissionUUID.substring(1, 2));
        folderName.append(separator);
        folderName.append(submissionUUID);

        return folderName.toString();
    }
}
