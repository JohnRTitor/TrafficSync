package trafficsync.snapshot;
import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.terminal.EventQueue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChandyLamportManager {
    // Sends snapshot messages
    public interface MessageSender {
        void sendMessage(MessageType type, String target, String payload, String snapshotId);
    }
    // Snapshot details
    private final String ownerId;
    private final List<String> outgoingNeighbors;
    private final List<String> incomingNeighbors;
    private final MessageSender sender;
    // Snapshot state
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    // Channel records
    private final Map<String, List<Message>> channelStates = new ConcurrentHashMap<>();
    private final Set<String> emptyChannels = ConcurrentHashMap.newKeySet();
    private final Set<String> closedChannels = ConcurrentHashMap.newKeySet();
    // Local snapshot data
    private String savedLocalState = "";
    private volatile String currentSnapshotId = null;
    // Initialize manager
    public ChandyLamportManager(String ownerId, List<String> outgoingNeighbors,
                                List<String> incomingNeighbors, MessageSender sender) {
        this.ownerId = ownerId;
        this.outgoingNeighbors = outgoingNeighbors;
        this.incomingNeighbors = incomingNeighbors;
        this.sender = sender;
    }
    // Start snapshot
    public void initiateSnapshot(String snapshotId) {
        if (isRecording.compareAndSet(false, true)) {
            this.currentSnapshotId = snapshotId;
            EventQueue.snapshot("Initiating Snapshot: " + snapshotId);
            // Save local state
            recordLocalState();
            // Send markers
            for (String neighbor : outgoingNeighbors) {
                sender.sendMessage(MessageType.MARKER, neighbor, null, snapshotId);
            }
            // Finish if no inputs
            if (incomingNeighbors.isEmpty()) {
                completeSnapshot();
            }
        } else {
            EventQueue.warn("Snapshot already in progress.");
        }
    }
    // Process marker
    public void handleMarker(Message markerMsg) {
        String senderId = markerMsg.getSenderId();
        String snapshotId = markerMsg.getSnapshotId();
        if (isRecording.compareAndSet(false, true)) {
            this.currentSnapshotId = snapshotId;
            // First marker
            EventQueue.snapshot("First MARKER received from " + senderId + ". Starting recording.");
            recordLocalState();
            // Close sender channel
            emptyChannels.add(senderId);
            closedChannels.add(senderId);
            // Forward markers
            for (String neighbor : outgoingNeighbors) {
                sender.sendMessage(MessageType.MARKER, neighbor, null, snapshotId);
            }

        } else {
            // Close channel
            EventQueue.snapshot("Subsequent MARKER received from " + senderId + ". Closing channel.");
            closedChannels.add(senderId);
        }
        // Check completion
        checkCompletion();
    }

    // Record incoming message
    public void recordIncomingMessage(Message msg) {
        if (!isRecording.get())
            return;

        String senderId = msg.getSenderId();
        // Save in-transit message
        if (!closedChannels.contains(senderId)) {
            channelStates.computeIfAbsent(senderId, k -> new ArrayList<>()).add(msg);
        }
    }
    // Save local state
    private void recordLocalState() {
        savedLocalState = String.format("STATE[%s, ts=%d]", ownerId, System.currentTimeMillis());
        channelStates.clear();
        emptyChannels.clear();
        closedChannels.clear();
    }
    // Check completion
    private void checkCompletion() {
        if (closedChannels.size() >= incomingNeighbors.size()) {
            completeSnapshot();
        }
    }
    // Cancel snapshot
    public void abortSnapshot() {
        if (isRecording.compareAndSet(true, false)) {
            channelStates.clear();
            emptyChannels.clear();
            closedChannels.clear();
            savedLocalState = "";
        }
    }
    // Finish snapshot
    private void completeSnapshot() {
        isRecording.set(false);
        StringBuilder report = new StringBuilder();
        report.append(savedLocalState).append(" | Channels: ");
        // Build report
        for (String neighbor : incomingNeighbors) {
            if (emptyChannels.contains(neighbor) || !channelStates.containsKey(neighbor)) {
                report.append("[").append(neighbor).append(": empty] ");
            } else {
                report.append("[")
                      .append(neighbor)
                      .append(": ")
                      .append(channelStates.get(neighbor).size())
                      .append(" msgs] ");
            }
        }
        String finalReport = report.toString();
        EventQueue.snapshot("Snapshot Complete: " + finalReport);
        // Notify node
        sender.sendMessage(
                MessageType.LOCAL_SNAPSHOT_DONE,
                "NODE",
                finalReport,
                currentSnapshotId
        );
    }
}