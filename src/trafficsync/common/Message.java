package trafficsync.common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final String senderId;
    private final String receiverId;
    private final String regionId;
    private final Object payload;
    private final long timestamp;
    private final String snapshotId; // Optional

    public Message(MessageType type, String senderId, String receiverId, String regionId, Object payload, long timestamp, String snapshotId) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.regionId = regionId;
        this.payload = payload;
        this.timestamp = timestamp;
        this.snapshotId = snapshotId;
    }
    
    public Message(MessageType type, String senderId, String receiverId, String regionId, Object payload) {
        this(type, senderId, receiverId, regionId, payload, System.currentTimeMillis(), null);
    }

    public MessageType getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getRegionId() { return regionId; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public String getSnapshotId() { return snapshotId; }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", regionId='" + regionId + '\'' +
                ", payload=" + payload +
                ", snapshotId=" + snapshotId +
                '}';
    }
}
