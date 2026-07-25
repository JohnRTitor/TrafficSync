package diffusing;

import common.Message;
import common.MessageType;
import communication.Communication;
import node.TrafficControllerProcess;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DiffusingEngine {

    private final TrafficControllerProcess node;
    private final int nodeId;
    private final Communication communication;
    
    private final int totalNodesInRegion;

    private class DiffusingInstance {
        int initiatorId;
        int parent = -1;
        boolean visited = false;
        int expectedEchos = 0;
        int nodesReached = 1; // self
    }

    private final Map<Integer, DiffusingInstance> instances = new HashMap<>();

    public DiffusingEngine(TrafficControllerProcess node) {
        this.node = node;
        this.nodeId = node.getNodeId();
        this.communication = node.getCommunication();
        this.totalNodesInRegion = node.getRegionNode().getLocalProcesses().size();
    }

    public void startDiffusing() {
        System.out.println("\n=================================");
        System.out.println("Node " + nodeId + " starting Diffusing Computation for Leadership");
        System.out.println("=================================");

        DiffusingInstance instance = new DiffusingInstance();
        instance.initiatorId = nodeId;
        instance.visited = true;
        instances.put(nodeId, instance);

        sendExplore(instance);
    }

    public void receiveExplore(Message message) {
        int initiatorId = parseInitiator(message.getPayload());
        
        System.out.println("Node " + nodeId + " received EXPLORE from Node " + message.getSenderId() + " for Initiator " + initiatorId);

        DiffusingInstance instance = instances.computeIfAbsent(initiatorId, k -> new DiffusingInstance());
        instance.initiatorId = initiatorId;

        if (!instance.visited) {
            instance.visited = true;
            instance.parent = message.getSenderId();
            sendExplore(instance);
        } else {
            sendEcho(message.getSenderId(), initiatorId, 0); // 0 extra nodes reached
        }
    }

    public void receiveEcho(Message message) {
        int initiatorId = parseInitiator(message.getPayload());
        int count = parseCount(message.getPayload());
        
        System.out.println("Node " + nodeId + " received ECHO from Node " + message.getSenderId() + " for Initiator " + initiatorId + " with count " + count);

        DiffusingInstance instance = instances.get(initiatorId);
        if (instance != null) {
            instance.nodesReached += count;
            instance.expectedEchos--;

            if (instance.expectedEchos == 0) {
                if (instance.parent != -1) {
                    System.out.println("Node " + nodeId + " returning ECHO to parent Node " + instance.parent + " for Initiator " + initiatorId);
                    sendEcho(instance.parent, initiatorId, instance.nodesReached);
                } else {
                    System.out.println("Diffusing Computation Completed for Initiator " + initiatorId);
                    System.out.println("Nodes reached: " + instance.nodesReached + " / " + totalNodesInRegion);
                    if (instance.nodesReached == totalNodesInRegion) {
                        System.out.println(">>> Node " + nodeId + " IS THE ELECTED LEADER <<<");
                        // Leader elected! Start snapshot.
                        node.getSnapshotEngine().startSnapshot();
                    }
                }
            }
        }
    }

    private void sendExplore(DiffusingInstance instance) {
        Set<Integer> neighbors = node.getNeighbors().keySet();
        instance.expectedEchos = neighbors.size();

        if (instance.expectedEchos == 0) {
            if (instance.parent != -1) {
                sendEcho(instance.parent, instance.initiatorId, instance.nodesReached);
            } else {
                System.out.println("Node " + nodeId + " has no neighbors. Cannot be leader for region size > 1.");
            }
            return;
        }

        for (int receiver : neighbors) {
            Message explore = new Message(
                    MessageType.EXPLORE,
                    nodeId,
                    receiver,
                    instance.initiatorId + ":0"
            );
            communication.send(explore);
        }
    }

    private void sendEcho(int receiver, int initiatorId, int nodesReached) {
        Message echo = new Message(
                MessageType.ECHO,
                nodeId,
                receiver,
                initiatorId + ":" + nodesReached
        );
        communication.send(echo);
    }
    
    private int parseInitiator(String payload) {
        return Integer.parseInt(payload.split(":")[0]);
    }
    
    private int parseCount(String payload) {
        return Integer.parseInt(payload.split(":")[1]);
    }
}