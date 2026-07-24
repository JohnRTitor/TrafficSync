package snapshot;

import common.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of an incoming communication channel.
 * Stores messages that were in transit during the snapshot.
 */
public class ChannelState {

    // Node from which this channel originates
    private int sourceNodeId;

    // Messages recorded on this channel
    private List<Message> messages;

    public ChannelState(int sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
        this.messages = new ArrayList<>();
    }

    /**
     * Record an in-transit message.
     */
    public void recordMessage(Message message) {
        messages.add(message);
    }

    public int getSourceNodeId() {
        return sourceNodeId;
    }

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {

        return "\nChannel From Node " + sourceNodeId +
                "\nRecorded Messages : " + messages + "\n";
    }
}