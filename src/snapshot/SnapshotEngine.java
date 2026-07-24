package snapshot;

import common.Message;
import common.MessageType;
import communication.Communication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the Chandy-Lamport Snapshot Algorithm.
 */
public class SnapshotEngine {

    private int nodeId;
    private Communication communication;

    private LocalState localState;

    private boolean snapshotStarted;

    private Set<Integer> pendingChannels;

    private Map<Integer, ChannelState> channelStates;

    public SnapshotEngine(int nodeId, Communication communication) {

        this.nodeId = nodeId;
        this.communication = communication;

        this.localState = new LocalState(nodeId);

        this.snapshotStarted = false;

        this.pendingChannels = new HashSet<>();

        this.channelStates = new HashMap<>();
    }

    /**
     * Coordinator starts snapshot.
     */
    public void startSnapshot() {

        if (snapshotStarted) {
            return;
        }

        snapshotStarted = true;

        System.out.println("\n================================");
        System.out.println("Snapshot Started at Node " + nodeId);
        System.out.println("================================");

        localState.addEvent("Snapshot Started");

        initializeIncomingChannels();

        sendMarker();
    }

    /**
     * Ring topology:
     * Node1 <- Node3
     * Node2 <- Node1
     * Node3 <- Node2
     */
    private void initializeIncomingChannels() {

        if (nodeId == 1) {

            pendingChannels.add(3);
            channelStates.put(3, new ChannelState(3));

        } else if (nodeId == 2) {

            pendingChannels.add(1);
            channelStates.put(1, new ChannelState(1));

        } else {

            pendingChannels.add(2);
            channelStates.put(2, new ChannelState(2));
        }
    }

    /**
     * Send MARKER.
     */
    private void sendMarker() {

        int receiver;

        if (nodeId == 1)
            receiver = 2;
        else if (nodeId == 2)
            receiver = 3;
        else
            receiver = 1;

        Message marker = new Message(
                MessageType.MARKER,
                nodeId,
                receiver,
                "MARKER"
        );

        communication.send(marker);
    }

    /**
     * Receive MARKER.
     */
    public void receiveMarker(Message message) {

        System.out.println("Node "
                + nodeId
                + " received MARKER from Node "
                + message.getSenderId());

        if (!snapshotStarted) {

            snapshotStarted = true;

            localState.addEvent(
                    "Snapshot started by MARKER from Node "
                            + message.getSenderId());

            initializeIncomingChannels();

            pendingChannels.remove(message.getSenderId());

            sendMarker();

        } else {

            pendingChannels.remove(message.getSenderId());

        }

        if (pendingChannels.isEmpty()) {

            finishSnapshot();

        }

    }

    /**
     * Record normal application messages.
     */
    public void recordTraffic(Message message) {

        localState.incrementReceivedMessages();

        localState.addEvent(
                "Received TRAFFIC from Node "
                        + message.getSenderId());

        if (snapshotStarted &&
                pendingChannels.contains(message.getSenderId())) {

            ChannelState channel =
                    channelStates.get(message.getSenderId());

            if (channel != null) {

                channel.recordMessage(message);

            }

        }

    }

    /**
     * Snapshot completed.
     */
    private void finishSnapshot() {

        System.out.println("\nSnapshot Completed at Node " + nodeId);

        SnapshotReport report =
                new SnapshotReport(localState);

        for (ChannelState channel : channelStates.values()) {

            report.addChannelState(channel);

        }

        report.printReport();

    }

    public LocalState getLocalState() {

        return localState;

    }

}