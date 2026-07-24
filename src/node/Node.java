package node;

import communication.Communication;
import communication.RegistryClient;
import communication.SocketCommunication;
import diffusing.DiffusingEngine;
import handler.MessageHandler;
import snapshot.SnapshotEngine;
import traffic.TrafficSimulator;

import java.util.Map;

/**
 * Represents one node in the distributed system.
 */
public class Node {

    private int nodeId;
    private int port;

    private Communication communication;
    private MessageHandler messageHandler;
    private SnapshotEngine snapshotEngine;
    private DiffusingEngine diffusingEngine;
    private TrafficSimulator trafficSimulator;
    
    private final RegistryClient registryClient = new RegistryClient();

    public Node(int listeningPort) {

        if (!registryClient.register("localhost", listeningPort)) {
            throw new RuntimeException("Registration Failed");
        }

        this.nodeId = registryClient.getNodeId();
        this.port = registryClient.getPort();

        // Create Message Handler
        messageHandler = new MessageHandler();

        // Create Communication Layer
        communication = new SocketCommunication(port, messageHandler, registryClient);

        // Create Snapshot Engine
        snapshotEngine = new SnapshotEngine(nodeId, communication);

        // Create Diffusing Engine
        diffusingEngine = new DiffusingEngine(nodeId, communication);

        // Create Traffic Simulator
        trafficSimulator = new TrafficSimulator(nodeId, communication);

        // Connect engines with MessageHandler
        messageHandler.setSnapshotEngine(snapshotEngine);
        messageHandler.setDiffusingEngine(diffusingEngine);
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getPort() {
        return port;
    }

    public Communication getCommunication() {
        return communication;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public SnapshotEngine getSnapshotEngine() {
        return snapshotEngine;
    }

    public DiffusingEngine getDiffusingEngine() {
        return diffusingEngine;
    }

    public TrafficSimulator getTrafficSimulator() {
        return trafficSimulator;
    }
    
    public Map<Integer, Integer> getNeighbors() {
        return registryClient.getNeighbors();
    }
    
    public void startCommunication() {
        communication.start();
    }
    
    public void stopCommunication() {
        communication.stop();
    }

    /**
     * Start traffic generation.
     */
    public void startTraffic() {
        trafficSimulator.start();
    }

    /**
     * Stop traffic generation.
     */
    public void stopTraffic() {
        trafficSimulator.stopTraffic();
    }

    /**
     * Start diffusing computation.
     */
    public void startDiffusingComputation() {
        diffusingEngine.startDiffusing();
    }
}