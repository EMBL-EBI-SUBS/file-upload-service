package uk.ac.ebi.subs.fileupload.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;

/**
 * This class contains the properties of the file being uploaded.
 */
@Data
public class TUSFileInfo {

    @JsonIgnore
    private long id;

    @JsonProperty(value = "ID")
    private String tusId;

    @JsonProperty(value = "Size")
    private long size;

    @JsonProperty(value = "Offset")
    private long offsetValue;

    @JsonProperty(value = "MetaData")
    @Valid
    @NotNull
    private MetaData metadata;

    @JsonProperty(value = "IsPartial")
    private boolean isPartial;

    @JsonProperty(value = "IsFinal")
    private boolean isFinal;

    @JsonProperty(value = "PartialUploads")
    private String[] partialUploads;

    public TUSFileInfo() {
    }

    @Data
    public static class MetaData {

        @JsonIgnore
        private long id;

        @NotNull
        @JsonProperty(value = "name")
        private String filename;

        @NotNull
        private String submissionID;

        @NotNull
        private String jwtToken;

        public MetaData() {
        }

        @Override
        public String toString() {
            return "metadata{" +
                    "filename='" + filename + '\'' +
                    ", submissionID='" + submissionID + '\'' +
                    '}';
        }
    }

    public static MetaData buildMetaData(String fileName, String submissionID, String jwtToken) {
        MetaData metadata = new MetaData();
        metadata.setFilename(fileName);
        metadata.setSubmissionID(submissionID);
        metadata.setJwtToken(jwtToken);

        return metadata;
    }

    @Override
    public String toString() {
        return "TUSFileInfo{" +
                "id=" + id +
                ", tusId='" + tusId + '\'' +
                ", size=" + size +
                ", offsetValue=" + offsetValue +
                ", metadata=" + metadata +
                ", isPartial=" + isPartial +
                ", isFinal=" + isFinal +
                ", partialUploads=" + Arrays.toString(partialUploads) +
                '}';
    }
}
