package trafficsync.node;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.snapshot.ChandyLamportManager;
import trafficsync.terminal.Event;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;
import trafficsync.transport.TCPConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrafficNode {
    private final String nodeId;
    private final String regionId;
    private final String serverHost;
    private final int serverPort;
    private final List<String> neighbors;
    private final int controllerCount;
    private final TerminalScreen screen;

    private TCPConnection vpsConnection;
    private ChandyLamportManager snapshotManager;
    private final List<ControllerThread> controllers = new ArrayList<>();
    
    private volatile boolean registered = false;

    public TrafficNode(String nodeId, String regionId, String serverHost, int serverPort, 
                       String neighborsStr, int controllerCount, TerminalScreen screen) {
        this.nodeId = nodeId;
        this.regionId = regionId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.controllerCount = controllerCount;
        this.screen = screen;
        
        this.neighbors = new ArrayList<>();
        if (neighborsStr != null && !neighborsStr.trim().isEmpty()) {
            this.neighbors.addAll(Arrays.asList(neighborsStr.split(",")));
        }
    }

    public void start() {
        try {
            Socket socket = new Socket(serverHost, serverPort);
            vpsConnection = new TCPConnection(socket, this::handleMessage, this::handleDisconnect);
            vpsConnection.start();
            
            snapshotManager = new ChandyLamportManager(nodeId, neighbors, vpsConnection, screen);

            // Register with VPS
            String payload = String.join(",", neighbors);
            Message regMsg = new Message(MessageType.REGISTER, nodeId, "VPS", regionId, payload);
            vpsConnection.send(regMsg);
            
            EventQueue.info("Connected to VPS. Sent REGISTER.");
            
        } catch (IOException e) {
            EventQueue.error("Failed to connect to VPS at " + serverHost + ":" + serverPort + " - " + e.getMessage());
        }
    }

    public void stop() {
        for (ControllerThread ct : controllers) {
            ct.stopRunning();
        }
        if (vpsConnection != null) {
            vpsConnection.close();
        }
        EventQueue.info("Node stopped.");
    }
    
    public void triggerSnapshot() {
        if (!registered) {
            EventQueue.warn("Cannot trigger snapshot: Node not registered with VPS.");
            return;
        }
        snapshotManager.initiateSnapshot();
    }
    
    public void sendTrafficUpdate(String payload) {
        if (!registered || neighbors.isEmpty()) return;
        String target = neighbors.get((int) (Math.random() * neighbors.size()));
        Message msg = new Message(MessageType.TRAFFIC_UPDATE, nodeId, target, regionId, payload);
        vpsConnection.send(msg);
        EventQueue.network("Sent TRAFFIC to " + target);
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case REGISTER_ACK:
                registered = true;
                EventQueue.network("Registration Confirmed.");
                screen.setStatus("Connection", "CONNECTED");
                startControllers();
                break;
            case TRAFFIC_UPDATE:
            case ACCIDENT_ALERT:
                EventQueue.network("Received " + msg.getType() + " from " + msg.getSenderId());
                snapshotManager.recordIncomingMessage(msg);
                break;
            case MARKER:
                snapshotManager.handleMarker(msg);
                break;
            case STATUS_RESPONSE:
                EventQueue.info("Received PONG from VPS");
                break;
            default:
                EventQueue.warn("Unhandled message type: " + msg.getType());
        }
    }

    private void handleDisconnect(TCPConnection connection) {
        registered = false;
        screen.setStatus("Connection", "DISCONNECTED");
        EventQueue.error("Lost connection to VPS.");
        for (ControllerThread ct : controllers) {
            ct.stopRunning();
        }
    }

    private void startControllers() {
        for (int i = 0; i < controllerCount; i++) {
            ControllerThread ct = new ControllerThread("Controller-" + i, this);
            controllers.add(ct);
            ct.start();
        }
        EventQueue.info("Started " + controllerCount + " controller threads.");
    }
}
