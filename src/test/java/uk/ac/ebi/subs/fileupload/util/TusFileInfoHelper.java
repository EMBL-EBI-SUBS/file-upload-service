package uk.ac.ebi.subs.fileupload.util;

import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;

import java.util.UUID;

public class TusFileInfoHelper {

    public static TUSFileInfo generateTUSFileInfo(String jwtToken, String sunmissionId, String filename) {
        TUSFileInfo fileInfo = new TUSFileInfo();
        fileInfo.setId(1L);
        fileInfo.setTusId(UUID.randomUUID().toString());
        fileInfo.setMetadata(
                TUSFileInfo.buildMetaData(filename, sunmissionId, jwtToken));

        return fileInfo;
    }
}
