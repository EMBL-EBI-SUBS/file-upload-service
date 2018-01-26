package uk.ac.ebi.subs.fileupload.repository.util;

import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;

/**
 * This class contains static helper method(s) related to the {@link File} entity.
 */
public class FileHelper {

    public static File convertTUSFileInfoToFile(TUSFileInfo tusFileInfo) {
        File file = new File();
        file.setGeneratedTusId(tusFileInfo.getTusId());
        file.setSubmissionId(tusFileInfo.getMetadata().getSubmissionID());
        file.setFilename(tusFileInfo.getMetadata().getFilename());
        file.setTotalSize(tusFileInfo.getSize());
        file.setUploadedSize(tusFileInfo.getOffsetValue());

        return file;
    }
}
