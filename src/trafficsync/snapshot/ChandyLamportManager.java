package trafficsync.snapshot;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;
import trafficsync.transport.TCPConnection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ChandyLamportManager {
    private final String nodeId;
    private final List<String> outgoingNeighbors; // Channels we send markers TO
    private final List<String> incomingNeighbors; // Logical incoming channels we expect markers FROM
    private final TCPConnection vpsConnection;
    private final TerminalScreen screen;

    // We assume bidirectional links for this simulation if not strictly specified otherwise
    // So incomingNeighbors = outgoingNeighbors
    
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicInteger markersReceivedCount = new AtomicInteger(0);
    
    // Channel state: senderId -> List of messages
    private final Map<String, List<Message>> channelStates = new ConcurrentHashMap<>();
    private final Set<String> emptyChannels = ConcurrentHashMap.newKeySet();
    private final Set<String> closedChannels = ConcurrentHashMap.newKeySet();
    
    // Local state variables (mocked)
    private String savedLocalState = "";

    public ChandyLamportManager(String nodeId, List<String> neighbors, TCPConnection vpsConnection, TerminalScreen screen) {
        this.nodeId = nodeId;
        this.outgoingNeighbors = neighbors;
        this.incomingNeighbors = neighbors; // For simplicity, symmetric graph
        this.vpsConnection = vpsConnection;
        this.screen = screen;
    }

    public void initiateSnapshot() {
        if (isRecording.compareAndSet(false, true)) {
            String snapshotId = "SNAP-" + System.currentTimeMillis();
            EventQueue.snapshot("Initiating Snapshot: " + snapshotId);
            screen.setTask("Snapshot", 0.1, "Recording local state...");
            
            recordLocalState();
            
            // Send MARKER on all outgoing channels
            for (String neighbor : outgoingNeighbors) {
                Message marker = new Message(MessageType.MARKER, nodeId, neighbor, null, null, System.currentTimeMillis(), snapshotId);
                vpsConnection.send(marker);
            }
            
            if (incomingNeighbors.isEmpty()) {
                completeSnapshot();
            } else {
                screen.setTask("Snapshot", 0.5, "Waiting for markers on channels...");
            }
        } else {
            EventQueue.warn("Snapshot already in progress.");
        }
    }

    public void handleMarker(Message markerMsg) {
        String senderId = markerMsg.getSenderId();
        String snapshotId = markerMsg.getSnapshotId();
        
        if (isRecording.compareAndSet(false, true)) {
            // Rule 1: First time seeing marker
            EventQueue.snapshot("First MARKER received from " + senderId + ". Starting recording.");
            screen.setTask("Snapshot", 0.5, "Recording in progress...");
            recordLocalState();
            
            // Mark the channel from senderId as empty
            emptyChannels.add(senderId);
            closedChannels.add(senderId);
            
            // Propagate markers
            for (String neighbor : outgoingNeighbors) {
                Message m = new Message(MessageType.MARKER, nodeId, neighbor, null, null, System.currentTimeMillis(), snapshotId);
                vpsConnection.send(m);
            }
            
        } else {
            // Rule 2: Subsequent marker on a channel
            EventQueue.snapshot("Subsequent MARKER received from " + senderId + ". Closing channel.");
            closedChannels.add(senderId);
        }
        
        checkCompletion();
    }

    public void recordIncomingMessage(Message msg) {
        if (!isRecording.get()) return;
        
        String senderId = msg.getSenderId();
        // If channel is not yet closed (haven't received marker on it), record message
        if (!closedChannels.contains(senderId)) {
            channelStates.computeIfAbsent(senderId, k -> new ArrayList<>()).add(msg);
        }
    }

    private void recordLocalState() {
        savedLocalState = "NodeState{time=" + System.currentTimeMillis() + "}";
        channelStates.clear();
        emptyChannels.clear();
        closedChannels.clear();
        markersReceivedCount.set(0);
    }

    private void checkCompletion() {
        if (closedChannels.size() >= incomingNeighbors.size()) {
            completeSnapshot();
        } else {
            double progress = 0.5 + (0.5 * ((double) closedChannels.size() / incomingNeighbors.size()));
            screen.setTask("Snapshot", progress, "Waiting for markers... " + closedChannels.size() + "/" + incomingNeighbors.size());
        }
    }

    private void completeSnapshot() {
        isRecording.set(false);
        screen.setTask("Snapshot", 1.0, "Completed");
        
        StringBuilder report = new StringBuilder();
        report.append(savedLocalState).append(" | Channels: ");
        for (String neighbor : incomingNeighbors) {
            if (emptyChannels.contains(neighbor) || !channelStates.containsKey(neighbor)) {
                report.append("[").append(neighbor).append(": empty] ");
            } else {
                report.append("[").append(neighbor).append(": ").append(channelStates.get(neighbor).size()).append(" msgs] ");
            }
        }
        
        String finalReport = report.toString();
        EventQueue.snapshot("Snapshot Complete: " + finalReport);
        
        // Report to VPS
        Message response = new Message(MessageType.SNAPSHOT_RESPONSE, nodeId, "VPS", null, finalReport);
        vpsConnection.send(response);
        
        // Reset task after a delay (could be done in a separate thread, but this is fine for UI)
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            screen.setTask("Idle", -1, "");
        }).start();
    }
}
