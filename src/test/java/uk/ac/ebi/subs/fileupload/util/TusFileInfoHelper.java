package uk.ac.ebi.subs.fileupload.util;

import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;

public class TusFileInfoHelper {

    public static TUSFileInfo generateTUSFileInfo(String jwtToken, String sunmissionId, String filename) {
        TUSFileInfo fileInfo = new TUSFileInfo();
        fileInfo.setId(1L);
        fileInfo.setMetadata(
                TUSFileInfo.buildMetaData(filename, sunmissionId, jwtToken));

        return fileInfo;
    }
}
