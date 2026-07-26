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
    private final int nodePort;
    private final List<String> neighbors;
    private final List<String> peers = new ArrayList<>();
    private final int controllerCount;
    private final TerminalScreen screen;

    private TCPConnection vpsConnection;
    private ChandyLamportManager snapshotManager;
    private final List<ControllerThread> controllers = new ArrayList<>();
    
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
        
        this.neighbors = new ArrayList<>();
    }

    public void start() {
        try {
            Socket socket = new Socket(serverHost, serverPort);
            vpsConnection = new TCPConnection(socket, this::handleMessage, this::handleDisconnect);
            vpsConnection.start();
            
            snapshotManager = new ChandyLamportManager(nodeId, neighbors, vpsConnection, screen);

            // Register with VPS
            String payload = nodePort + "," + controllerCount + ",ACTIVE";
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
            case TOPOLOGY:
                updateTopology((String) msg.getPayload());
                break;
            case PEER_LIST:
                updatePeers((String) msg.getPayload());
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
    
    private void updateTopology(String payload) {
        neighbors.clear();
        if (payload != null && !payload.isEmpty()) {
            neighbors.addAll(Arrays.asList(payload.split(",")));
        }
        screen.setStatus("Neighbors", neighbors.isEmpty() ? "None" : String.join(",", neighbors));
        EventQueue.info("Topology updated: " + neighbors);
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
        EventQueue.info("Logical Neighbors: " + String.join(", ", neighbors));
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
