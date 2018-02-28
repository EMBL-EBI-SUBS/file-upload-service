package uk.ac.ebi.subs.fileupload.model;

import lombok.ToString;

/**
 * This class represents a RabbitMQ JSON message.
 * It only contains 1 key-value pair that contains the file's generated ID by TUS.
 */
@ToString
public class ChecksumGenerationMessage {

    private String generatedTusId;

    public String getGeneratedTusId() {
        return generatedTusId;
    }

    public void setGeneratedTusId(String generatedTusId) {
        this.generatedTusId = generatedTusId;
    }
}
