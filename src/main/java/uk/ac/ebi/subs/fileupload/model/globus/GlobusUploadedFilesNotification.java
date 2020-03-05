package uk.ac.ebi.subs.fileupload.model.globus;

import lombok.Data;

import java.util.List;

@Data
public class GlobusUploadedFilesNotification {

    private String owner;

    private String submissionId;

    private List<String> files;
}
