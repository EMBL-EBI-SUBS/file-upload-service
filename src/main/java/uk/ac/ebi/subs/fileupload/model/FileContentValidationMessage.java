package uk.ac.ebi.subs.fileupload.model;

import lombok.Data;

@Data
public class FileContentValidationMessage {

    private String fileUUID;
    private String fileType;
    private String filePath;
    private String validationResultUUID;

}
