package uk.ac.ebi.subs.fileupload.model.globus;

import lombok.Data;

@Data
public class GlobusShareRequest {

    private String owner;

    private String submissionId;
}
