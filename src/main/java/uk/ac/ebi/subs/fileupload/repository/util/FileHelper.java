package uk.ac.ebi.subs.fileupload.repository.util;

import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;

/**
 * This class contains static helper method(s) related to the {@link File} entity.
 */
public class FileHelper {

    public static File convertTUSFileInfoToFile(TUSFileInfo tusFileInfo) {
        File file = new File();
        file.setId(tusFileInfo.getId());
        file.setTusId(tusFileInfo.getTusId());
        file.setSubmissionId(tusFileInfo.getMetadata().getSubmissionID());
        file.setFilename(tusFileInfo.getMetadata().getFilename());
        file.setTotalSize(tusFileInfo.getSize());
        file.setUploadedSize(tusFileInfo.getOffsetValue());
        file.setUser(tusFileInfo.getMetadata().getJwtToken());

        // TODO: get the user from the JWT token

        return file;
    }
}
