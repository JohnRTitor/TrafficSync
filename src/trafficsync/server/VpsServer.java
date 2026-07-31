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
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// This is the core coordinator for the entire traffic system.
// It runs on a central server (VPS) and manages all the connected sites,
// routes messages between them, and coordinates the global snapshots.
// Internally it delegates connection tracking to NodeRegistry and renders
// events through the TerminalScreen. Every message that flows between two
// nodes passes through this class, making it the single routing hub.
public class VpsServer {
    // These variables hold the server's basic networking and user interface objects.
    private final int port;
    private final TerminalScreen screen;
    private TCPServer tcpServer;
    private final NodeRegistry registry = new NodeRegistry();
    // The snapshot dictionary stores the responses from each node during a global state collection.
    // We use a ConcurrentHashMap here because snapshot responses arrive on different network threads.
    // The currentSnapshotId helps us track which snapshot round is currently running so we can
    // ignore stale responses from a previous round.
    private final ConcurrentHashMap<String, String> snapshotStates = new ConcurrentHashMap<>();
    private String currentSnapshotId = null;

    // This constructor sets up the server before we start accepting actual connections.
    // We pass the port number it should listen on and the screen where it will display logs.
    public VpsServer(int port, TerminalScreen screen) {
        this.port = port;
        this.screen = screen;
    }

    // This starts the underlying TCP server so nodes can begin connecting.
    // The handleNewConnection method will be called every time a new client connects.
    public void start() throws IOException {
        tcpServer = new TCPServer(port, this::handleNewConnection);
        tcpServer.start();
        EventQueue.info("VPS Server started on port " + port);
        updateScreenStatus();
    }

    // When we want to shut down the server, this method stops the TCP listener
    // and cleanly closes all active sockets connected to the traffic nodes.
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

    // Provides access to the node registry. This is used by other parts of the
    // system that need to look up which nodes are currently connected.
    public NodeRegistry getRegistry() {
        return registry;
    }

    // Provides access to the raw snapshot state map. This allows external code
    // to inspect which nodes have reported their local state so far.
    public ConcurrentHashMap<String, String> getSnapshotStates() {
        return snapshotStates;
    }

    // Refreshes the node count shown on the terminal dashboard.
    // We call this after every registration or disconnection event.
    private void updateScreenStatus() {
        screen.setStatus("Registered Nodes", String.valueOf(registry.getNodes().size()));
    }

    // This method is triggered whenever a new socket connects to our port.
    // We wrap the raw socket in our own TCPConnection class to handle the input and output streams.
    private void handleNewConnection(Socket socket) {
        try {
            EventQueue.info("New connection from " + socket.getRemoteSocketAddress());
            TCPConnection[] connRef = new TCPConnection[1];

            // We set up the connection with two callback functions: one for receiving messages,
            // and one for dealing with the client when they disconnect.
            connRef[0] = new TCPConnection(socket, msg -> handleMessage(msg, connRef[0]), this::handleDisconnect);
            connRef[0].start();
        } catch (IOException e) {
            EventQueue.error("Failed to establish connection: " + e.getMessage());
        }
    }

    // When a node disconnects (either intentionally or due to a network failure),
    // the TCPConnection's listener thread detects the broken stream and calls this method.
    // We search through the registry to figure out which node ID belongs to this dead connection,
    // remove it from our records, and then broadcast the updated peer list so that all
    // remaining nodes know this site is no longer reachable.
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

    // This is the main router method. Whenever any message arrives at the server,
    // it comes here first. We check the message type and pass it to the right handler.
    private void handleMessage(Message msg, TCPConnection connection) {
        switch (msg.type()) {
            case REGISTER -> handleRegister(msg, connection);
            // Traffic updates and snapshot markers are just forwarded to their intended destination.
            case TRAFFIC_UPDATE, MARKER -> relayMessage(msg);
            case MANUAL_MESSAGE -> {
                // If a manual message is meant for the server itself, we print it to the screen.
                // Otherwise, we send it along to another node.
                if ("VPS".equals(msg.receiverId()) || "server".equalsIgnoreCase(msg.receiverId())) {
                    EventQueue.push(Event.Level.USER, "Message from " + msg.senderId() + ": " + msg.payload());
                } else {
                    relayMessage(msg);
                }
            }
            case SNAPSHOT_RESPONSE -> handleSnapshotResponse(msg);
            case QUERY_NODE_ID -> handleQueryNodeId(msg, connection);
            default -> EventQueue.warn("Unknown message type: " + msg.type());
        }
    }

    // When a node registers, we generate a formal ID for it and save it in our registry.
    // Then we send an acknowledgement back so the node knows it was successful.
    private void handleRegister(Message msg, TCPConnection connection) {
        String payloadStr = (String) msg.payload();
        String[] parts = payloadStr.split(",");
        String nodeName = parts[0];

        String assignedNodeId = registry.generateNodeId();

        registry.registerNode(assignedNodeId, nodeName, connection);
        EventQueue.network("REGISTER successful: " + assignedNodeId + " (Name: " + nodeName + ")");

        connection.send(new Message(MessageType.REGISTER_ACK, "VPS", assignedNodeId, assignedNodeId));

        // After a new node joins, we need to update everyone's address book.
        broadcastPeers();
        updateScreenStatus();
    }

    // A node might want to find the ID of another node by its name. This method replies
    // with the ID if we have it in the registry.
    private void handleQueryNodeId(Message msg, TCPConnection connection) {
        String targetName = (String) msg.payload();
        String resolvedNodeId = registry.resolveNodeId(targetName);

        String responsePayload;
        if (resolvedNodeId != null) {
            responsePayload = targetName + " has Node ID: " + resolvedNodeId;
        } else {
            responsePayload = "Node not found: " + targetName;
        }

        connection.send(new Message(MessageType.QUERY_NODE_ID_RESPONSE, "VPS", msg.senderId(), responsePayload));
    }

    // This loop sends a list of all active nodes to every connected client.
    // We send everyone the list EXCEPT themselves, so they know who they can talk to.
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

    // This is a simple postman function. It looks up the receiver's ID in the registry
    // and sends the message object over their socket connection.
    private void relayMessage(Message msg) {
        String target = msg.receiverId();
        String resolvedNodeId = registry.resolveNodeId(target);
        if (resolvedNodeId != null) {
            TCPConnection conn = registry.getConnection(resolvedNodeId);
            if (conn != null) {
                conn.send(msg);
                EventQueue.network("Relayed " + msg.type() + " from " + msg.senderId() + " to " + resolvedNodeId);
            }
        } else {
            EventQueue.warn("Failed to relay message to " + target + ": Node not found");
        }
    }

    // Sends a manual message from the VPS terminal to a specific node.
    // The target can be a node ID, a name, or an ID without the NODE- prefix
    // because resolveNodeId handles all three formats.
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

    // Sends the same message to every currently connected node.
    // This is triggered when the user types 'b <message>' in the terminal.
    // If no nodes are connected, we warn the user instead of silently doing nothing.
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

    // This method is called when the user hits 's' in the terminal.
    // It creates a new snapshot session and tells every node to start recording their state.
    public void triggerGlobalSnapshot() {
        currentSnapshotId = "SNAP-" + System.currentTimeMillis();
        snapshotStates.clear();
        EventQueue.snapshot("Triggering global snapshot: " + currentSnapshotId);

        // We set up a visual task in the terminal to show progress as nodes report back.
        String snapId = currentSnapshotId;
        Runnable onCancel = () -> {
            screen.removeTask(snapId);
            snapshotStates.clear();
            currentSnapshotId = null;
        };
        screen.addTask(new Task(snapId, "Global Snapshot", 0.0, "Waiting for node responses...", onCancel));

        // Broadcast the snapshot trigger to every single connected site.
        for (String nodeId : registry.getNodes()) {
            TCPConnection conn = registry.getConnection(nodeId);
            if (conn != null) {
                conn.send(new Message(
                        MessageType.SNAPSHOT_TRIGGER, "VPS", nodeId, null, System.currentTimeMillis(), snapId));
            }
        }
    }

    // When a node finishes its local snapshot, it sends the result here.
    // We use a synchronized block because multiple nodes might report back at the exact same time.
    private void handleSnapshotResponse(Message msg) {
        String state = (String) msg.payload();

        synchronized (this) {
            snapshotStates.put(msg.senderId(), state);
            EventQueue.snapshot("Received snapshot state from " + msg.senderId() + ".");

            if (currentSnapshotId == null) return;

            int totalNodes = registry.getNodes().size();
            int received = snapshotStates.size();

            // Once we have received a state from every registered node, the snapshot is complete.
            if (received >= totalNodes) {
                EventQueue.snapshot("Global Snapshot Complete.");
                screen.removeTask(currentSnapshotId);

                // Write the final result out to a text file for record keeping.
                saveAggregatedSnapshot(currentSnapshotId, snapshotStates);

                currentSnapshotId = null;
            } else {
                // Otherwise we just update the progress bar on the screen.
                screen.updateTask(
                        currentSnapshotId,
                        (double) received / totalNodes,
                        "Waiting for node responses... " + received + "/" + totalNodes);
            }
        }
    }

    // Writes the final aggregated snapshot out to a text file so we have a persistent record.
    // Each snapshot gets its own file named after the snapshot ID (e.g. SNAP-1717012345678.txt).
    // The snapshots/ directory is created automatically if it does not exist yet.
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
