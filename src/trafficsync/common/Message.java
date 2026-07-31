package trafficsync.common;

import java.io.Serializable;

// This is the fundamental data unit exchanged over the network in our system.
// Every interaction -- registration, traffic updates, snapshot markers, manual messages --
// is wrapped in a Message record. The senderId and receiverId fields let the VpsServer
// route it to the correct destination, while the type field tells the receiver how to
// interpret the payload. The snapshotId field is only used during Chandy-Lamport operations.
// Since this is a Java record, all fields are automatically final and immutable, which means
// a message cannot be tampered with after creation. It implements Serializable so Java can
// convert it into bytes for transmission over ObjectOutputStream.
public record Message(
        MessageType type, String senderId, String receiverId, Object payload, long timestamp, String snapshotId)
        implements Serializable {

    // Default constructor for all parameters is auto-created in a record
    // Below is a Constructor overload
    // for non snapshot messages (without snapshot ID and passed timestamp)
    public Message(MessageType type, String senderId, String receiverId, Object payload) {
        this(type, senderId, receiverId, payload, System.currentTimeMillis(), null);
    }

    // Custom toString so log messages and debug output show a clean summary of the message.
    // The snapshotId is only included when it is present to keep non-snapshot messages shorter.
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
