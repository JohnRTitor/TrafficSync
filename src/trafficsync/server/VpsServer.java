package trafficsync.server;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.terminal.Event;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.Task;
import trafficsync.terminal.TerminalScreen;
import trafficsync.transport.TCPConnection;
import trafficsync.transport.TCPServer;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class VpsServer {
    private final int port;
    private final TerminalScreen screen;
    private TCPServer tcpServer;
    private final NodeRegistry registry = new NodeRegistry();
    private final ConcurrentHashMap<String, String> snapshotStates = new ConcurrentHashMap<>();
    private String currentSnapshotId = null;

    public VpsServer(int port, TerminalScreen screen) {
        this.port = port;
        this.screen = screen;
    }

    public void start() throws IOException {
        tcpServer = new TCPServer(port, this::handleNewConnection);
        tcpServer.start();
        EventQueue.info("VPS Server started on port " + port);
        updateScreenStatus();
    }

    public void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
        }
        for (String nodeId : registry.getNodes()) {
            TCPConnection conn = registry.getConnection(nodeId);
            if (conn != null) conn.close();
        }
        EventQueue.info("VPS Server stopped.");
    }

    public NodeRegistry getRegistry() {
        return registry;
    }
    
    public ConcurrentHashMap<String, String> getSnapshotStates() {
        return snapshotStates;
    }

    private void updateScreenStatus() {
        screen.setStatus("Registered Nodes", String.valueOf(registry.getNodes().size()));
    }

    private void handleNewConnection(Socket socket) {
        try {
            EventQueue.info("New connection from " + socket.getRemoteSocketAddress());
            TCPConnection[] connRef = new TCPConnection[1];
            connRef[0] = new TCPConnection(
                socket,
                msg -> handleMessage(msg, connRef[0]),
                this::handleDisconnect
            );
            connRef[0].start();
        } catch (IOException e) {
            EventQueue.error("Failed to establish connection: " + e.getMessage());
        }
    }

    private void handleDisconnect(TCPConnection connection) {
        String toRemove = null;
        for (String nodeId : registry.getNodes()) {
            if (registry.getConnection(nodeId) == connection) {
                toRemove = nodeId;
                break;
            }
        }
        if (toRemove != null) {
            registry.removeNode(toRemove);
            EventQueue.warn("Node disconnected: " + toRemove);
            broadcastPeers();
            updateScreenStatus();
        }
    }

    private void handleMessage(Message msg, TCPConnection connection) {
        switch (msg.getType()) {
            case REGISTER -> handleRegister(msg, connection);
            case TRAFFIC_UPDATE, MARKER -> relayMessage(msg);
            case MANUAL_MESSAGE -> {
                if ("VPS".equals(msg.getReceiverId()) || "server".equalsIgnoreCase(msg.getReceiverId())) {
                    EventQueue.push(Event.Level.USER, "Message from " + msg.getSenderId() + ": " + msg.getPayload());
                } else {
                    relayMessage(msg);
                }
            }
            case SNAPSHOT_RESPONSE -> handleSnapshotResponse(msg);
            case QUERY_NODE_ID -> handleQueryNodeId(msg, connection);
            default -> EventQueue.warn("Unknown message type: " + msg.getType());
        }
    }

    private void handleRegister(Message msg, TCPConnection connection) {
        String payloadStr = (String) msg.getPayload();
        String[] parts = payloadStr.split(",");
        String nodeName = parts[0];
        
        String assignedNodeId = registry.generateNodeId();
        
        registry.registerNode(assignedNodeId, nodeName, connection);
        EventQueue.network("REGISTER successful: " + assignedNodeId + " (Name: " + nodeName + ")");
        
        connection.send(new Message(MessageType.REGISTER_ACK, "VPS", assignedNodeId, assignedNodeId));
        
        broadcastPeers();
        updateScreenStatus();
    }
    
    private void handleQueryNodeId(Message msg, TCPConnection connection) {
        String targetName = (String) msg.getPayload();
        String resolvedNodeId = registry.resolveNodeId(targetName);
        
        String responsePayload;
        if (resolvedNodeId != null) {
            responsePayload = targetName + " has Node ID: " + resolvedNodeId;
        } else {
            responsePayload = "Node not found: " + targetName;
        }
        
        connection.send(new Message(MessageType.QUERY_NODE_ID_RESPONSE, "VPS", msg.getSenderId(), responsePayload));
    }
    
    private void broadcastPeers() {
        Set<String> allNodes = registry.getNodes();
        
        for (String nodeId : allNodes) {
            TCPConnection conn = registry.getConnection(nodeId);
            if (conn != null) {
                Set<String> neighbors = new HashSet<>(allNodes);
                neighbors.remove(nodeId);
                java.util.List<String> peerInfo = new java.util.ArrayList<>();
                for (String neighbor : neighbors) {
                    peerInfo.add(neighbor + "(" + registry.getNodeName(neighbor) + ")");
                }
                String peersPayload = String.join(",", peerInfo);
                conn.send(new Message(MessageType.PEER_LIST, "VPS", nodeId, peersPayload));
            }
        }
    }
    
    private void relayMessage(Message msg) {
        String target = msg.getReceiverId();
        String resolvedNodeId = registry.resolveNodeId(target);
        if (resolvedNodeId != null) {
            TCPConnection conn = registry.getConnection(resolvedNodeId);
            if (conn != null) {
                conn.send(msg);
                EventQueue.network("Relayed " + msg.getType() + " from " + msg.getSenderId() + " to " + resolvedNodeId);
            }
        } else {
            EventQueue.warn("Failed to relay message to " + target + ": Node not found");
        }
    }

    public void sendMessageToNode(String target, String message) {
        String resolvedNodeId = registry.resolveNodeId(target);
        if (resolvedNodeId != null) {
            TCPConnection conn = registry.getConnection(resolvedNodeId);
            if (conn != null) {
                conn.send(new Message(MessageType.MANUAL_MESSAGE, "VPS", resolvedNodeId, message));
                EventQueue.info("Sent message to " + resolvedNodeId + ": " + message);
            }
        } else {
            EventQueue.warn("Node not found: " + target);
        }
    }

    public void broadcastMessage(String message) {
        Set<String> nodes = registry.getNodes();
        if (nodes.isEmpty()) {
            EventQueue.warn("No nodes connected to broadcast to.");
            return;
        }
        for (String nodeId : nodes) {
            TCPConnection conn = registry.getConnection(nodeId);
            if (conn != null) {
                conn.send(new Message(MessageType.MANUAL_MESSAGE, "VPS", nodeId, message));
            }
        }
        EventQueue.info("Broadcasted message: " + message);
    }
    
    public void triggerGlobalSnapshot() {
        currentSnapshotId = "SNAP-" + System.currentTimeMillis();
        snapshotStates.clear();
        EventQueue.snapshot("Triggering global snapshot: " + currentSnapshotId);
        
        String snapId = currentSnapshotId;
        Runnable onCancel = () -> {
            screen.removeTask(snapId);
            snapshotStates.clear();
            currentSnapshotId = null;
        };
        screen.addTask(new Task(snapId, "Global Snapshot", 0.0, "Waiting for node responses...", onCancel));
        
        for (String nodeId : registry.getNodes()) {
            TCPConnection conn = registry.getConnection(nodeId);
            if (conn != null) {
                conn.send(new Message(MessageType.SNAPSHOT_TRIGGER, "VPS", nodeId, null, System.currentTimeMillis(), snapId));
            }
        }
    }

    private void handleSnapshotResponse(Message msg) {
        String state = (String) msg.getPayload();
        
        synchronized (this) {
            snapshotStates.put(msg.getSenderId(), state);
            EventQueue.snapshot("Received snapshot state from " + msg.getSenderId() + ".");
            
            if (currentSnapshotId == null) return;
            
            int totalNodes = registry.getNodes().size();
            int received = snapshotStates.size();
            
            if (received >= totalNodes) {
                EventQueue.snapshot("Global Snapshot Complete.");
                screen.removeTask(currentSnapshotId);
                
                saveAggregatedSnapshot(currentSnapshotId, snapshotStates);
                
                currentSnapshotId = null;
            } else {
                screen.updateTask(currentSnapshotId, (double) received / totalNodes, "Waiting for node responses... " + received + "/" + totalNodes);
            }
        }
    }

    private void saveAggregatedSnapshot(String snapshotId, ConcurrentHashMap<String, String> states) {
        File dir = new File("snapshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File file = new File(dir, snapshotId + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Global Snapshot ID: " + snapshotId);
            writer.println("==================================================");
            for (Map.Entry<String, String> entry : states.entrySet()) {
                writer.println("Node: " + entry.getKey());
                writer.println("State: " + entry.getValue());
                writer.println("--------------------------------------------------");
            }
            EventQueue.snapshot("Aggregated snapshot saved to " + file.getPath());
        } catch (IOException e) {
            EventQueue.error("Failed to save aggregated snapshot: " + e.getMessage());
        }
    }
}
