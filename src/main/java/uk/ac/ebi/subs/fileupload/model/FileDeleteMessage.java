package uk.ac.ebi.subs.fileupload.model;

import lombok.Data;

@Data
public class FileDeleteMessage {
    private String targetFilePath;
    private String submissionId;
}
