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
    
    // node id -> Set of logically connected neighbor ids
    private final ConcurrentMap<String, Set<String>> topology = new ConcurrentHashMap<>();

    public void registerNode(String nodeId, String regionId, Set<String> neighbors, TCPConnection connection) {
        nodeConnections.put(nodeId, connection);
        nodeRegions.put(nodeId, regionId);
        topology.put(nodeId, neighbors);
    }

    public void removeNode(String nodeId) {
        nodeConnections.remove(nodeId);
        nodeRegions.remove(nodeId);
        topology.remove(nodeId);
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
    
    public ConcurrentMap<String, Set<String>> getTopology() {
        return topology;
    }
}
