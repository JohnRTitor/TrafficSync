package trafficsync.snapshot;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ChandyLamportManager {
    public interface MessageSender {
        void sendMessage(MessageType type, String target, String payload, String snapshotId);
    }

    private final String ownerId;
    private final List<String> outgoingNeighbors; // Channels we send markers TO
    private final List<String> incomingNeighbors; // Logical incoming channels we expect markers FROM
    private final MessageSender sender;
    private final TerminalScreen screen;

    // We assume bidirectional links for this simulation if not strictly specified
    // otherwise
    // So incomingNeighbors = outgoingNeighbors

    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicInteger markersReceivedCount = new AtomicInteger(0);

    // Channel state: senderId -> List of messages
    private final Map<String, List<Message>> channelStates = new ConcurrentHashMap<>();
    private final Set<String> emptyChannels = ConcurrentHashMap.newKeySet();
    private final Set<String> closedChannels = ConcurrentHashMap.newKeySet();

    // Local state variables (mocked)
    private String savedLocalState = "";

    public ChandyLamportManager(String ownerId, List<String> outgoingNeighbors, List<String> incomingNeighbors,
            MessageSender sender, TerminalScreen screen) {
        this.ownerId = ownerId;
        this.outgoingNeighbors = outgoingNeighbors;
        this.incomingNeighbors = incomingNeighbors;
        this.sender = sender;
        this.screen = screen;
    }

    public void initiateSnapshot() {
        if (isRecording.compareAndSet(false, true)) {
            String snapshotId = "SNAP-" + System.currentTimeMillis();
            EventQueue.snapshot("Initiating Snapshot: " + snapshotId);

            recordLocalState();

            // Send MARKER on all outgoing channels
            for (String neighbor : outgoingNeighbors) {
                sender.sendMessage(MessageType.MARKER, neighbor, null, snapshotId);
            }

            if (incomingNeighbors.isEmpty()) {
                completeSnapshot();
            }

            EventQueue.warn("Snapshot already in progress.");
        }
    }

    public void handleMarker(Message markerMsg) {
        String senderId = markerMsg.getSenderId();
        String snapshotId = markerMsg.getSnapshotId();

        if (isRecording.compareAndSet(false, true)) {
            // Rule 1: First time seeing marker
            EventQueue.snapshot("First MARKER received from " + senderId + ". Starting recording.");
            recordLocalState();

            // Mark the channel from senderId as empty
            emptyChannels.add(senderId);
            closedChannels.add(senderId);

            // Propagate markers
            for (String neighbor : outgoingNeighbors) {
                sender.sendMessage(MessageType.MARKER, neighbor, null, snapshotId);
            }

        } else {
            // Rule 2: Subsequent marker on a channel
            EventQueue.snapshot("Subsequent MARKER received from " + senderId + ". Closing channel.");
            closedChannels.add(senderId);
        }

        checkCompletion();
    }

    public void recordIncomingMessage(Message msg) {
        if (!isRecording.get())
            return;

        String senderId = msg.getSenderId();
        // If channel is not yet closed (haven't received marker on it), record message
        if (!closedChannels.contains(senderId)) {
            channelStates.computeIfAbsent(senderId, k -> new ArrayList<>()).add(msg);
        }
    }

    private void recordLocalState() {
        savedLocalState = ownerId + "State{time=" + System.currentTimeMillis() + "}";
        channelStates.clear();
        emptyChannels.clear();
        closedChannels.clear();
        markersReceivedCount.set(0);
    }

    private void checkCompletion() {
        if (closedChannels.size() >= incomingNeighbors.size()) {
            completeSnapshot();
        } else {
            // progress is handled by Node now, we just wait.
        }
    }

    public void abortSnapshot() {
        if (isRecording.compareAndSet(true, false)) {
            channelStates.clear();
            emptyChannels.clear();
            closedChannels.clear();
            markersReceivedCount.set(0);
        }
    }

    private void completeSnapshot() {
        isRecording.set(false);

        StringBuilder report = new StringBuilder();
        report.append(savedLocalState).append(" | Channels: ");
        for (String neighbor : incomingNeighbors) {
            if (emptyChannels.contains(neighbor) || !channelStates.containsKey(neighbor)) {
                report.append("[").append(neighbor).append(": empty] ");
            } else {
                report.append("[").append(neighbor).append(": ").append(channelStates.get(neighbor).size())
                        .append(" msgs] ");
            }
        }

        String finalReport = report.toString();
        EventQueue.snapshot("Snapshot Complete: " + finalReport);

        // Report to Node
        sender.sendMessage(MessageType.LOCAL_SNAPSHOT_DONE, "NODE", finalReport, null);
    }
}
