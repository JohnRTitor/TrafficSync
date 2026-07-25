package node;

import communication.Communication;
import communication.LocalCommunication;
import diffusing.DiffusingEngine;
import handler.MessageHandler;
import snapshot.SnapshotEngine;
import traffic.TrafficSimulator;
import common.Message;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

public class TrafficControllerProcess implements Runnable {

    private final int localNodeId;
    private final RegionNode regionNode;
    
    private final Communication communication;
    private final MessageHandler messageHandler;
    private final SnapshotEngine snapshotEngine;
    private final DiffusingEngine diffusingEngine;
    private final TrafficSimulator trafficSimulator;
    
    private final Map<Integer, Integer> neighbors = new HashMap<>(); // localNodeId -> dummy port
    private final Set<Integer> incomingNeighbors = new HashSet<>();
    
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TrafficControllerProcess(int localNodeId, RegionNode regionNode, Map<Integer, BlockingQueue<Message>> router, List<Integer> neighborIds, List<Integer> incomingIds) {
        this.localNodeId = localNodeId;
        this.regionNode = regionNode;

        for (int nid : neighborIds) {
            neighbors.put(nid, 0); // Port not needed for local comm
        }
        this.incomingNeighbors.addAll(incomingIds);

        this.messageHandler = new MessageHandler();
        this.communication = new LocalCommunication(localNodeId, messageHandler, router);
        
        // Passing 'this' to engines, need to ensure they use localNodeId
        this.snapshotEngine = new SnapshotEngine(this);
        this.diffusingEngine = new DiffusingEngine(this);
        this.trafficSimulator = new TrafficSimulator(this);
        
        this.messageHandler.setSnapshotEngine(snapshotEngine);
        this.messageHandler.setDiffusingEngine(diffusingEngine);
    }
    
    @Override
    public void run() {
        running.set(true);
        communication.start();
        System.out.println("TrafficControllerProcess " + localNodeId + " running.");
        
        // Wait until stopped
        while (running.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        communication.stop();
        trafficSimulator.stopTraffic();
    }

    public void stopProcess() {
        running.set(false);
    }

    public int getNodeId() {
        return localNodeId;
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
        return neighbors;
    }

    public Set<Integer> getIncomingNeighbors() {
        return incomingNeighbors;
    }
    
    public RegionNode getRegionNode() {
        return regionNode;
    }
    
    public void startTraffic() {
        trafficSimulator.start();
    }

    public void stopTraffic() {
        trafficSimulator.stopTraffic();
    }

    public void startDiffusingComputation() {
        diffusingEngine.startDiffusing();
    }
}
