package trafficsync.server;

import trafficsync.transport.TCPConnection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NodeRegistry {
    // node id -> Connection
    private final ConcurrentMap<String, TCPConnection> nodeConnections = new ConcurrentHashMap<>();
    
    // node id -> Region ID
    private final ConcurrentMap<String, String> nodeRegions = new ConcurrentHashMap<>();
    

    public void registerNode(String nodeId, String regionId, TCPConnection connection) {
        nodeConnections.put(nodeId, connection);
        nodeRegions.put(nodeId, regionId);
    }

    public void removeNode(String nodeId) {
        nodeConnections.remove(nodeId);
        nodeRegions.remove(nodeId);
    }

    public TCPConnection getConnection(String nodeId) {
        return nodeConnections.get(nodeId);
    }

    public Set<String> getNodes() {
        return nodeConnections.keySet();
    }
    
    public ConcurrentMap<String, String> getNodeRegions() {
        return nodeRegions;
    }
    
}
