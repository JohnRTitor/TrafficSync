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
    
    // node id -> Node Port
    private final ConcurrentMap<String, Integer> nodePorts = new ConcurrentHashMap<>();
    
    // node id -> Controller Count
    private final ConcurrentMap<String, Integer> controllerCounts = new ConcurrentHashMap<>();
    
    // node id -> Status
    private final ConcurrentMap<String, String> nodeStatuses = new ConcurrentHashMap<>();
    
    // node id -> Set of logically connected neighbor ids
    private final ConcurrentMap<String, Set<String>> topology = new ConcurrentHashMap<>();

    public void registerNode(String nodeId, String regionId, int nodePort, int controllerCount, String status, TCPConnection connection) {
        nodeConnections.put(nodeId, connection);
        nodeRegions.put(nodeId, regionId);
        nodePorts.put(nodeId, nodePort);
        controllerCounts.put(nodeId, controllerCount);
        nodeStatuses.put(nodeId, status);
    }

    public void removeNode(String nodeId) {
        nodeConnections.remove(nodeId);
        nodeRegions.remove(nodeId);
        nodePorts.remove(nodeId);
        controllerCounts.remove(nodeId);
        nodeStatuses.remove(nodeId);
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
    
    public void buildGlobalTopology() {
        Set<String> nodes = getNodes();
        for (String node : nodes) {
            Set<String> neighbors = ConcurrentHashMap.newKeySet();
            for (String other : nodes) {
                if (!node.equals(other)) {
                    neighbors.add(other);
                }
            }
            topology.put(node, neighbors);
        }
    }
}
