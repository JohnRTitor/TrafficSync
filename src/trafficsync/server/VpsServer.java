package trafficsync.server;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;
import trafficsync.transport.TCPConnection;
import trafficsync.transport.TCPServer;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VpsServer {
    private final int port;
    private final TerminalScreen screen;
    private TCPServer tcpServer;
    private final NodeRegistry registry = new NodeRegistry();
    private final ConcurrentHashMap<String, String> snapshotStates = new ConcurrentHashMap<>();

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
            broadcastTopologyAndPeers();
            updateScreenStatus();
        }
    }

    private void handleMessage(Message msg, TCPConnection connection) {
        switch (msg.getType()) {
            case REGISTER:
                handleRegister(msg, connection);
                break;
            case TRAFFIC_UPDATE:
            case ACCIDENT_ALERT:
            case MARKER:
            case MANUAL_MESSAGE:
                relayMessage(msg);
                break;
            case SNAPSHOT_RESPONSE:
                handleSnapshotResponse(msg);
                break;
            case PING:
                connection.send(new Message(MessageType.STATUS_RESPONSE, "VPS", msg.getSenderId(), null, "PONG"));
                break;
            default:
                EventQueue.warn("Unknown message type: " + msg.getType());
        }
    }

    private void handleRegister(Message msg, TCPConnection connection) {
        String nodeId = msg.getSenderId();
        String regionId = msg.getRegionId();
        
        String payloadStr = (String) msg.getPayload();
        int nodePort = 0;
        int controllerCount = 0;
        String status = "UNKNOWN";
        
        if (payloadStr != null && !payloadStr.isEmpty()) {
            String[] parts = payloadStr.split(",");
            if (parts.length >= 1) nodePort = Integer.parseInt(parts[0]);
            if (parts.length >= 2) controllerCount = Integer.parseInt(parts[1]);
            if (parts.length >= 3) status = parts[2];
        }
        
        registry.registerNode(nodeId, regionId, nodePort, controllerCount, status, connection);
        connection.setConnectionId(nodeId);
        EventQueue.network("REGISTER successful: " + nodeId + " (Region: " + regionId + ")");
        
        connection.send(new Message(MessageType.REGISTER_ACK, "VPS", nodeId, regionId, "OK"));
        
        broadcastTopologyAndPeers();
        updateScreenStatus();
    }
    
    private void broadcastTopologyAndPeers() {
        registry.buildGlobalTopology();
        Set<String> allNodes = registry.getNodes();
        
        for (String nodeId : allNodes) {
            TCPConnection conn = registry.getConnection(nodeId);
            if (conn != null) {
                // Send PEER_LIST
                Set<String> neighbors = registry.getTopology().get(nodeId);
                String peersPayload = neighbors != null ? String.join(",", neighbors) : "";
                conn.send(new Message(MessageType.PEER_LIST, "VPS", nodeId, null, peersPayload));
                
                // Send TOPOLOGY
                String topoPayload = neighbors != null ? String.join(",", neighbors) : "";
                conn.send(new Message(MessageType.TOPOLOGY, "VPS", nodeId, null, topoPayload));
            }
        }
    }
    
    private void relayMessage(Message msg) {
        String target = msg.getReceiverId();
        TCPConnection conn = registry.getConnection(target);
        if (conn != null) {
            conn.send(msg);
            EventQueue.network("Relayed " + msg.getType() + " from " + msg.getSenderId() + " to " + target);
        } else {
            EventQueue.warn("Failed to relay message to " + target + ": Node not found");
        }
    }
    
    public void triggerGlobalSnapshot() {
        String snapshotId = "SNAP-" + System.currentTimeMillis();
        snapshotStates.clear();
        EventQueue.snapshot("Triggering global snapshot: " + snapshotId);
        screen.setTask("Snapshot", 0.0, "Waiting for node responses...");
        
        for (String nodeId : registry.getNodes()) {
            TCPConnection conn = registry.getConnection(nodeId);
            if (conn != null) {
                conn.send(new Message(MessageType.SNAPSHOT_TRIGGER, "VPS", nodeId, null, snapshotId));
            }
        }
    }

    private void handleSnapshotResponse(Message msg) {
        String state = (String) msg.getPayload();
        snapshotStates.put(msg.getSenderId(), state);
        EventQueue.snapshot("Received snapshot state from " + msg.getSenderId() + ": " + state);
        
        int totalNodes = registry.getNodes().size();
        int received = snapshotStates.size();
        
        if (received >= totalNodes) {
            screen.setTask("Snapshot", 1.0, "All nodes reported.");
            EventQueue.snapshot("Global Snapshot Complete.");
        } else {
            screen.setTask("Snapshot", (double) received / totalNodes, "Waiting for node responses... " + received + "/" + totalNodes);
        }
    }
}
