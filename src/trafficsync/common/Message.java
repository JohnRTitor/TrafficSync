package trafficsync.common;

import java.io.Serializable;

// This class represents a single packet of information sent over the network.
// It implements Serializable so Java can automatically convert it into bytes for us.
public class Message implements Serializable {
    // We need this version ID so Java does not complain if we recompile the project.
    private static final long serialVersionUID = 1L;

    // These are the core details every message needs.
    // They are marked final so the message cannot be changed once it is created.
    private final MessageType type;
    private final String senderId;
    private final String receiverId;
    private final Object payload;
    private final long timestamp;
    private final String snapshotId; 

    // This constructor is used when a message is part of a Chandy-Lamport snapshot.
    // We need the snapshot ID to know which global snapshot this belongs to.
    public Message(MessageType type, String senderId, String receiverId, Object payload, long timestamp, String snapshotId) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.payload = payload;
        this.timestamp = timestamp;
        this.snapshotId = snapshotId;
    }
    
    public Message(MessageType type, String senderId, String receiverId, Object payload) {
        this(type, senderId, receiverId, payload, System.currentTimeMillis(), null);
    }

    // The getters allow other parts of the program to read the message data safely.
    public MessageType getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public String getSnapshotId() { return snapshotId; }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", payload=" + payload +
                ", snapshotId=" + snapshotId +
                '}';
    }
}
