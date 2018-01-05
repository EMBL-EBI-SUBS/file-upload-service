package uk.ac.ebi.subs.fileupload.util;

import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;

public class TusFileInfoHelper {

    public static TUSFileInfo generateTUSFileInfo(String jwtToken, String sunmissionId) {
        TUSFileInfo fileInfo = new TUSFileInfo();
        fileInfo.setMetadata(
                TUSFileInfo.buildMetaData("test file name", sunmissionId, jwtToken));

        return fileInfo;
    }
}
