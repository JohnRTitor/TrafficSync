package trafficsync.server;

import trafficsync.transport.TCPConnection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NodeRegistry {
    // node id -> Connection
    private final ConcurrentMap<String, TCPConnection> nodeConnections = new ConcurrentHashMap<>();
    
    // node id -> Node Name
    private final ConcurrentMap<String, String> nodeNames = new ConcurrentHashMap<>();
    
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public String generateNodeId() {
        return "NODE-" + idCounter.getAndIncrement();
    }

    public void registerNode(String nodeId, String nodeName, TCPConnection connection) {
        nodeConnections.put(nodeId, connection);
        nodeNames.put(nodeId, nodeName);
    }

    public void removeNode(String nodeId) {
        nodeConnections.remove(nodeId);
        nodeNames.remove(nodeId);
    }

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
    
    public String resolveNodeId(String target) {
        if (nodeConnections.containsKey(target)) return target;
        
        String nodeWithPrefix = "NODE-" + target;
        if (nodeConnections.containsKey(nodeWithPrefix)) return nodeWithPrefix;
        
        for (java.util.Map.Entry<String, String> entry : nodeNames.entrySet()) {
            if (entry.getValue().equals(target)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
