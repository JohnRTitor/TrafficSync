package trafficsync.server;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.terminal.Event;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;
import trafficsync.transport.TCPConnection;
import trafficsync.transport.TCPServer;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
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
        
        String neighborsStr = (String) msg.getPayload();
        Set<String> neighbors = new HashSet<>();
        if (neighborsStr != null && !neighborsStr.isEmpty()) {
            neighbors.addAll(Arrays.asList(neighborsStr.split(",")));
        }
        
        registry.registerNode(nodeId, regionId, neighbors, connection);
        connection.setConnectionId(nodeId);
        EventQueue.network("REGISTER successful: " + nodeId + " (Region: " + regionId + ")");
        
        connection.send(new Message(MessageType.REGISTER_ACK, "VPS", nodeId, regionId, "OK"));
        updateScreenStatus();
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
    
    private void handleSnapshotResponse(Message msg) {
        String state = (String) msg.getPayload();
        snapshotStates.put(msg.getSenderId(), state);
        EventQueue.snapshot("Received snapshot state from " + msg.getSenderId());
    }
}
