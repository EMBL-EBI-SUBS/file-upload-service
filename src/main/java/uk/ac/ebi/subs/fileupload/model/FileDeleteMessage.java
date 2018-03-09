package uk.ac.ebi.subs.fileupload.model;

public class FileDeleteMessage {

    private String targetFilePath;

    public String getTargetFilePath() {
        return targetFilePath;
    }

    public void setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }
}
