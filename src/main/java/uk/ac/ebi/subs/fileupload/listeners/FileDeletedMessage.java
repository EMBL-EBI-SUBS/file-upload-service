package uk.ac.ebi.subs.fileupload.listeners;

import lombok.Data;

@Data
public class FileDeletedMessage {

    private String submissionId;
}
