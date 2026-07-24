package snapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the local state of a node when a snapshot is taken.
 */
public class LocalState {

    // Node ID
    private int nodeId;

    // Number of application messages sent
    private int sentMessages;

    // Number of application messages received
    private int receivedMessages;

    // Events that happened before the snapshot
    private List<String> events;

    public LocalState(int nodeId) {

        this.nodeId = nodeId;
        this.sentMessages = 0;
        this.receivedMessages = 0;
        this.events = new ArrayList<>();
    }

    // Record a sent message
    public void incrementSentMessages() {
        sentMessages++;
    }

    // Record a received message
    public void incrementReceivedMessages() {
        receivedMessages++;
    }

    // Add an event
    public void addEvent(String event) {
        events.add(event);
    }

    // Getters

    public int getNodeId() {
        return nodeId;
    }

    public int getSentMessages() {
        return sentMessages;
    }

    public int getReceivedMessages() {
        return receivedMessages;
    }

    public List<String> getEvents() {
        return events;
    }

    @Override
    public String toString() {

        return "\n========== LOCAL STATE ==========\n" +
                "Node ID            : " + nodeId +
                "\nSent Messages      : " + sentMessages +
                "\nReceived Messages  : " + receivedMessages +
                "\nEvents             : " + events +
                "\n=================================\n";
    }
}