package trafficsync.common;

import java.io.Serializable;

// These are the core details every message needs. In a Record, they are
// automatically marked final, so the message cannot be changed once it is created.
// It represents a single packet of information sent over the network.
// Implements Serializable so Java can automatically convert it into bytes
public record Message(
        MessageType type, String senderId, String receiverId, Object payload, long timestamp, String snapshotId)
        implements Serializable {

    // Default constructor for all parameters is auto-created in a record
    // Below is a Constructor overload
    // for non snapshot messages (without snapshot ID and passed timestamp)
    public Message(MessageType type, String senderId, String receiverId, Object payload) {
        this(type, senderId, receiverId, payload, System.currentTimeMillis(), null);
    }

    @Override
    public String toString() {
        return "Message{" + "type=" + type
                + ", senderId='" + senderId + '\''
                + ", receiverId='" + receiverId + '\''
                + ", payload=" + payload
                + ", timestamp=" + timestamp
                + (snapshotId != null ? ", snapshotId='" + snapshotId + '\'' : "")
                + '}';
    }
}
