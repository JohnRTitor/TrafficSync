package common;

import java.io.Serializable;

/**
 * Represents a message exchanged between nodes.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;
    private int senderId;
    private int receiverId;
    private String payload;

    // Constructor
    public Message(MessageType type, int senderId, int receiverId, String payload) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.payload = payload;
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String getPayload() {
        return payload;
    }

    // Setters
    public void setType(MessageType type) {
        this.type = type;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", payload='" + payload + '\'' +
                '}';
    }
}