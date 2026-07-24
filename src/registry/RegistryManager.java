package registry;

import java.util.*;

public class RegistryManager {

    private final Map<Integer, NodeInfo> registeredNodes;
    private final Topology topology;

    private int nextNodeId;

    public RegistryManager(Topology topology) {

        this.topology = topology;
        this.registeredNodes = new LinkedHashMap<>();
        this.nextNodeId = 1;
    }

    // Checks whether a node is already registered
    public synchronized boolean isRegistered(String host, int port) {

        for (NodeInfo node : registeredNodes.values()) {

            if (node.getHost().equals(host)
                    && node.getPort() == port) {

                return true;
            }

        }

        return false;
    }

    // Registers a node (or returns the existing one if already registered)
    public synchronized NodeInfo registerNode(String host, int port) {

        // Duplicate registration
        for (NodeInfo node : registeredNodes.values()) {

            if (node.getHost().equals(host)
                    && node.getPort() == port) {

                return node;

            }

        }

        // Maximum number of nodes reached
        if (nextNodeId > topology.getTotalNodes()) {

            return null;

        }

        NodeInfo node = new NodeInfo(nextNodeId, host, port);

        registeredNodes.put(nextNodeId, node);

        nextNodeId++;

        return node;
    }
    
    public synchronized void removeNode(int nodeId) {
        registeredNodes.remove(nodeId);
    }

    public synchronized List<Integer> getNeighbors(int nodeId) {

        return topology.getNeighbors(nodeId);

    }

    public synchronized List<Integer> getIncomingNeighbors(int nodeId) {

        return topology.getIncomingNeighbors(nodeId);

    }

    public synchronized Collection<NodeInfo> getRegisteredNodes() {

        return registeredNodes.values();

    }

    public synchronized NodeInfo getNode(int nodeId) {

        return registeredNodes.get(nodeId);

    }

    public synchronized int getRegisteredCount() {

        return registeredNodes.size();

    }

    public synchronized boolean allNodesRegistered() {

        return registeredNodes.size() ==
                topology.getTotalNodes();

    }

    public void printRegisteredNodes() {

        System.out.println();
        System.out.println("===== Registered Nodes =====");

        if (registeredNodes.isEmpty()) {

            System.out.println("No nodes registered.");

        } else {

            for (NodeInfo node : registeredNodes.values()) {

                System.out.println(
                        "Node "
                                + node.getId()
                                + " -> "
                                + node.getHost()
                                + ":"
                                + node.getPort());

            }

        }

        System.out.println("============================");

    }

}
