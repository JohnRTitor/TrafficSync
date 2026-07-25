package snapshot;

import common.Message;
import common.MessageType;
import communication.Communication;
import communication.Client;
import communication.RegistryClient;
import aggregator.SnapshotSerializer;
import node.TrafficControllerProcess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements the Chandy-Lamport Snapshot Algorithm.
 */
public class SnapshotEngine {

    private TrafficControllerProcess node;
    private int nodeId;
    private Communication communication;

    private LocalState localState;

    private boolean snapshotStarted;

    private Set<Integer> pendingChannels;

    private Map<Integer, ChannelState> channelStates;

    public SnapshotEngine(TrafficControllerProcess node) {

        this.node = node;
        this.nodeId = node.getNodeId();
        this.communication = node.getCommunication();

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

    private void initializeIncomingChannels() {
        for (int incomingId : node.getIncomingNeighbors()) {
            pendingChannels.add(incomingId);
            channelStates.put(incomingId, new ChannelState(incomingId));
        }
    }

    /**
     * Send MARKER.
     */
    private void sendMarker() {
        for (int receiver : node.getNeighbors().keySet()) {
            Message marker = new Message(
                    MessageType.MARKER,
                    nodeId,
                    receiver,
                    "MARKER"
            );
            communication.send(marker);
        }
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

    private void finishSnapshot() {

        System.out.println("\nSnapshot Completed at Node " + nodeId);

        SnapshotReport report =
                new SnapshotReport(localState);

        for (ChannelState channel : channelStates.values()) {

            report.addChannelState(channel);

        }

        report.printReport();

        System.out.println("Sending Snapshot Report to Aggregator...");
        
        try {
            RegistryClient registryClient = node.getRegionNode().getRegistryClient();
            String aggHost = registryClient.getAggregatorHost();
            int aggPort = registryClient.getAggregatorPort();
            int regionId = node.getRegionNode().getRegionId();
            
            if (aggHost != null && !aggHost.isEmpty() && aggPort > 0) {
                String payload = SnapshotSerializer.serialize(report);
                Message msg = new Message(MessageType.SNAPSHOT_REPORT, regionId, 0, payload);
                Client client = new Client();
                client.send(aggHost, aggPort, msg);
            } else {
                System.out.println("Aggregator not found in Registry. Report not sent.");
            }
        } catch (Exception e) {
            System.err.println("Failed to send Snapshot Report: " + e.getMessage());
        }

    }

    public LocalState getLocalState() {

        return localState;

    }

}