package trafficsync.node;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.terminal.Event;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;
import trafficsync.terminal.TerminalScreen.Task;
import java.util.concurrent.atomic.AtomicBoolean;
import trafficsync.transport.RegionCommunicator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrafficNode {
    private final String nodeId;
    private final String regionId;
    private final String serverHost;
    private final int serverPort;
    private final int nodePort;
    private final List<String> peers = new ArrayList<>();
    private final int controllerCount;
    private final TerminalScreen screen;

    private RegionCommunicator communicator;
    private final List<ControllerThread> controllers = new ArrayList<>();
    
    private final Map<String, List<String>> threadTopology = new HashMap<>();
    private final Map<String, List<String>> incomingThreadTopology = new HashMap<>();
    
    private volatile boolean snapshotInProgress = false;
    private String currentSnapshotId = null;
    private final Queue<Message> pendingSnapshotTriggers = new LinkedList<>();
    private final Map<String, String> localSnapshotStates = new ConcurrentHashMap<>();
    
    private volatile boolean trafficGenerationEnabled = false;
    private volatile boolean registered = false;

    public TrafficNode(String nodeId, String regionId, String serverHost, int serverPort, 
                       int nodePort, int controllerCount, TerminalScreen screen) {
        this.nodeId = nodeId;
        this.regionId = regionId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.nodePort = nodePort;
        this.controllerCount = controllerCount;
        this.screen = screen;
    }

    public void start() {
        try {
            EventQueue.info("Connecting to VPS at " + serverHost + ":" + serverPort + "...");
            communicator = new RegionCommunicator(nodeId, regionId, serverHost, serverPort, this::handleMessage, this::handleDisconnect);
            communicator.start();
            
            // Register with VPS
            String payload = nodePort + "," + controllerCount + ",ACTIVE";
            communicator.sendMessage(MessageType.REGISTER, "VPS", payload);
            
            EventQueue.info("Connected to VPS. Sent REGISTER.");
            
        } catch (IOException e) {
            EventQueue.error("Failed to connect to VPS at " + serverHost + ":" + serverPort + " - " + e.getMessage());
        }
    }

    public void stop() {
        for (ControllerThread ct : controllers) {
            ct.stopRunning();
        }
        if (communicator != null) {
            communicator.stop();
        }
        EventQueue.info("Node stopped.");
    }
    
    public void triggerLocalSnapshot() {
        if (!registered) {
            EventQueue.warn("Cannot trigger snapshot: Node not registered with VPS.");
            return;
        }
        String localSnapshotId = "LOCAL-" + System.currentTimeMillis();
        handleSnapshotTrigger(new Message(MessageType.SNAPSHOT_TRIGGER, "LOCAL", nodeId, null, null, System.currentTimeMillis(), localSnapshotId));
    }
    
    public void toggleTrafficGeneration() {
        trafficGenerationEnabled = !trafficGenerationEnabled;
        EventQueue.info("Traffic generation is now " + (trafficGenerationEnabled ? "ON" : "OFF"));
    }
    
    public boolean isTrafficGenerationEnabled() {
        return trafficGenerationEnabled;
    }

    public void sendManualMessage(String target, String text) {
        if (!registered) {
            EventQueue.warn("Cannot send manual message: Node not registered with VPS.");
            return;
        }
        
        String resolvedTarget = target;
        if (!peers.contains(target)) {
            for (String peer : peers) {
                if (peer.equals("NODE-" + target) || peer.endsWith("-" + target)) {
                    resolvedTarget = peer;
                    break;
                }
            }
        }
        
        communicator.sendMessage(MessageType.MANUAL_MESSAGE, resolvedTarget, text);
        EventQueue.network("Sent MANUAL_MESSAGE to " + resolvedTarget);
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case REGISTER_ACK:
                registered = true;
                EventQueue.network("Registration Confirmed.");
                screen.setStatus("Connection", "CONNECTED");
                startControllers();
                break;
            case PEER_LIST:
                updatePeers((String) msg.getPayload());
                break;
            case SNAPSHOT_TRIGGER:
                handleSnapshotTrigger(msg);
                break;
            case TOPOLOGY:
                // We no longer rely on external topology for Chandy-Lamport
                break;
            case STATUS_RESPONSE:
                EventQueue.info("Received PONG from VPS");
                break;
            case MANUAL_MESSAGE:
                EventQueue.push(Event.Level.USER, "Message from " + msg.getSenderId() + ": " + msg.getPayload());
                break;
            default:
                EventQueue.warn("Unhandled message type: " + msg.getType());
        }
    }
    
    private void handleSnapshotTrigger(Message msg) {
        if (snapshotInProgress) {
            pendingSnapshotTriggers.add(msg);
            EventQueue.info("Snapshot already in progress. Scheduling trigger for later.");
            return;
        }
        
        snapshotInProgress = true;
        currentSnapshotId = msg.getSnapshotId();
        localSnapshotStates.clear();
        
        String snapId = currentSnapshotId;
        Runnable onCancel = () -> {
            snapshotInProgress = false;
            currentSnapshotId = null;
            localSnapshotStates.clear();
            for (ControllerThread ct : controllers) {
                ct.abortSnapshot();
            }
            if (!pendingSnapshotTriggers.isEmpty()) {
                handleSnapshotTrigger(pendingSnapshotTriggers.poll());
            }
        };
        screen.addTask(new Task(snapId, "Local Snapshot", -1, "Waiting for markers...", onCancel));
        
        if (msg.getSenderId().equals("LOCAL")) {
            EventQueue.snapshot("Initiating Local Snapshot (ID: " + currentSnapshotId + ")");
        } else {
            EventQueue.snapshot("Received SNAPSHOT_TRIGGER from VPS (ID: " + currentSnapshotId + ")");
        }
        
        List<String> validInitiators = new ArrayList<>();
        for (String node : threadTopology.keySet()) {
            if (canReachAll(node)) {
                validInitiators.add(node);
            }
        }
        
        if (!validInitiators.isEmpty()) {
            String initiator = validInitiators.get((int)(Math.random() * validInitiators.size()));
            EventQueue.snapshot("Selected initiator " + initiator + " for local snapshot.");
            routeThreadMessage(new Message(MessageType.START_SNAPSHOT, "NODE", initiator, regionId, null, System.currentTimeMillis(), currentSnapshotId));
        } else {
            EventQueue.error("No valid initiator found in local topology!");
            // Free the lock since we failed to start
            snapshotInProgress = false;
            currentSnapshotId = null;
        }
    }

    private boolean canReachAll(String startNode) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startNode);
        visited.add(startNode);
        
        while (!queue.isEmpty()) {
            String curr = queue.poll();
            for (String neighbor : threadTopology.get(curr)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return visited.size() == controllerCount;
    }

    public void routeThreadMessage(Message msg) {
        if (msg.getType() == MessageType.LOCAL_SNAPSHOT_DONE) {
            handleLocalSnapshotDone(msg);
            return;
        }

        String target = msg.getReceiverId();
        for (ControllerThread ct : controllers) {
            if (ct.getThreadName().equals(target)) {
                ct.enqueueMessage(msg);
                break;
            }
        }
    }
    
    private synchronized void handleLocalSnapshotDone(Message msg) {
        if (currentSnapshotId == null || !currentSnapshotId.equals(msg.getSnapshotId())) {
            // Ignore stale snapshot messages
            return;
        }
        
        localSnapshotStates.put(msg.getSenderId(), (String)msg.getPayload());
        if (localSnapshotStates.size() == controllerCount) {
            StringBuilder sb = new StringBuilder();
            sb.append("Region-").append(regionId).append(" [");
            for (Map.Entry<String, String> entry : localSnapshotStates.entrySet()) {
                sb.append("{").append(entry.getKey()).append(": ").append(entry.getValue()).append("}");
            }
            sb.append("]");
            
            if (currentSnapshotId.startsWith("LOCAL-")) {
                EventQueue.snapshot("Local Snapshot Result: " + sb.toString());
            } else {
                communicator.sendMessage(MessageType.SNAPSHOT_RESPONSE, "VPS", sb.toString());
                EventQueue.snapshot("Sent combined regional snapshot to VPS");
            }
            
            screen.removeTask(currentSnapshotId);
            
            // Clean up and check queue
            snapshotInProgress = false;
            currentSnapshotId = null;
            
            if (!pendingSnapshotTriggers.isEmpty()) {
                Message nextTrigger = pendingSnapshotTriggers.poll();
                handleSnapshotTrigger(nextTrigger);
            }
        }
    }
    
    private void updatePeers(String payload) {
        peers.clear();
        if (payload != null && !payload.isEmpty()) {
            peers.addAll(Arrays.asList(payload.split(",")));
        }
        screen.setStatus("Peers", String.valueOf(peers.size()));
        EventQueue.info("Peer list updated: " + peers);
    }
    
    public void printPeers() {
        EventQueue.info("All Known Sites: " + String.join(", ", peers));
    }
    
    public void printNeighbors() {
        // Obsolete command now
    }

    private void handleDisconnect() {
        registered = false;
        screen.setStatus("Connection", "DISCONNECTED");
        EventQueue.error("Lost connection to VPS.");
        for (ControllerThread ct : controllers) {
            ct.stopRunning();
        }
    }

    private void buildThreadTopology() {
        for (int i = 0; i < controllerCount; i++) {
            String name = "Controller-" + i;
            threadTopology.put(name, new ArrayList<>());
            incomingThreadTopology.put(name, new ArrayList<>());
        }

        if (controllerCount == 0) return;

        List<String> nodes = new ArrayList<>(threadTopology.keySet());
        Collections.shuffle(nodes);
        String root = nodes.get(0);
        
        List<String> connected = new ArrayList<>();
        connected.add(root);
        
        // Build spanning tree
        for (int i = 1; i < nodes.size(); i++) {
            String target = nodes.get(i);
            String source = connected.get((int) (Math.random() * connected.size()));
            threadTopology.get(source).add(target);
            incomingThreadTopology.get(target).add(source);
            connected.add(target);
        }
        
        // Add random edges
        int extraEdges = controllerCount; 
        for (int i = 0; i < extraEdges; i++) {
            String from = nodes.get((int)(Math.random() * nodes.size()));
            String to = nodes.get((int)(Math.random() * nodes.size()));
            if (!from.equals(to) && !threadTopology.get(from).contains(to)) {
                threadTopology.get(from).add(to);
                incomingThreadTopology.get(to).add(from);
            }
        }
    }

    private void startControllers() {
        buildThreadTopology();
        for (int i = 0; i < controllerCount; i++) {
            String name = "Controller-" + i;
            ControllerThread ct = new ControllerThread(name, this, threadTopology.get(name), incomingThreadTopology.get(name), screen);
            controllers.add(ct);
            ct.start();
        }
        EventQueue.info("Started " + controllerCount + " controller threads with local topology.");
    }
}
