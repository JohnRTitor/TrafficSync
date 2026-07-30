package trafficsync.server;

import trafficsync.transport.TCPConnection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

// This class keeps track of all the traffic nodes that have connected to the central server.
// It helps us find a node connection when we need to send a message to a specific site.
public class NodeRegistry {

    // We use a ConcurrentHashMap because multiple network threads might try to add or remove
    // nodes at the exact same time. This map links the unique node ID to its actual TCP socket connection.
    private final ConcurrentMap<String, TCPConnection> nodeConnections = new ConcurrentHashMap<>();

    // This map links the node ID to its human-readable name, like NODE-A or NODE-B.
    private final ConcurrentMap<String, String> nodeNames = new ConcurrentHashMap<>();

    // We use an AtomicInteger to safely generate unique IDs for new nodes as they connect.
    private final AtomicInteger idCounter = new AtomicInteger(1);

    // This method creates a new unique ID for a node when it first registers with the server.
    // The AtomicInteger ensures that no two nodes get the same ID even if they connect simultaneously.
    public String generateNodeId() {
        return "NODE-" + idCounter.getAndIncrement();
    }

    // This method saves the new node in our maps so we can communicate with it later.
    public void registerNode(String nodeId, String nodeName, TCPConnection connection) {
        nodeConnections.put(nodeId, connection);
        nodeNames.put(nodeId, nodeName);
    }

    // When a node disconnects, we call this method to clean up our records.
    public void removeNode(String nodeId) {
        nodeConnections.remove(nodeId);
        nodeNames.remove(nodeId);
    }

    // This method looks up the active TCP connection for a specific node ID.
    // We use this whenever we need to route a message to that particular node.
    public TCPConnection getConnection(String nodeId) {
        return nodeConnections.get(nodeId);
    }

    public Set<String> getNodes() {
        return nodeConnections.keySet();
    }

    public java.util.Map<String, String> getNodeNames() {
        return nodeNames;
    }

    public String getNodeName(String nodeId) {
        return nodeNames.get(nodeId);
    }

    // This method tries to figure out which node the user or system wants to talk to.
    // It is very flexible because it accepts the exact ID, an ID without the prefix, or the node name.
    public String resolveNodeId(String target) {
        // If the target matches an exact node ID, we just return it.
        if (nodeConnections.containsKey(target)) return target;

        // Sometimes users forget the NODE- prefix, so we add it and check again.
        String nodeWithPrefix = "NODE-" + target;
        if (nodeConnections.containsKey(nodeWithPrefix)) return nodeWithPrefix;

        // If it was not an ID, we check if the user typed the human-readable name instead.
        // We loop through all the names to find a match.
        for (java.util.Map.Entry<String, String> entry : nodeNames.entrySet()) {
            if (entry.getValue().equals(target)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
