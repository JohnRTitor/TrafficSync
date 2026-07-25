package node;

import common.Message;
import communication.RegistryClient;
import registry.Topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class RegionNode {

    private final int listeningPort;
    private final RegistryClient registryClient;
    private int regionId;

    private final Map<Integer, TrafficControllerProcess> localProcesses = new ConcurrentHashMap<>();
    private final Map<Integer, BlockingQueue<Message>> localRouter = new ConcurrentHashMap<>();

    public RegionNode(int listeningPort) {
        this.listeningPort = listeningPort;
        this.registryClient = new RegistryClient();
    }

    public void startRegion(String localTopologyFile) {
        // Register the entire region as a single entity with the Azure Server
        if (!registryClient.register("104.214.168.255", listeningPort)) {
            throw new RuntimeException("Region Registration Failed");
        }

        this.regionId = registryClient.getNodeId();
        System.out.println("Region registered with ID: " + regionId);

        // Parse internal local topology
        Topology localTopology = new Topology(localTopologyFile);
        int numThreads = localTopology.getTotalNodes();
        System.out.println("Initializing " + numThreads + " local traffic controllers...");

        // Create threads
        for (int i = 1; i <= numThreads; i++) {
            List<Integer> neighbors = localTopology.getNeighbors(i);
            List<Integer> incomingNeighbors = localTopology.getIncomingNeighbors(i);

            TrafficControllerProcess process = new TrafficControllerProcess(
                    i,
                    this,
                    localRouter,
                    neighbors,
                    incomingNeighbors
            );
            localProcesses.put(i, process);
        }

        // Start all threads
        for (TrafficControllerProcess process : localProcesses.values()) {
            new Thread(process, "ProcessThread-" + process.getNodeId()).start();
        }

        // Start Diffusing Computation on all nodes to elect a leader
        System.out.println("Starting Diffusing Computation for leader election...");
        try {
            Thread.sleep(1000); // Wait for threads to initialize
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (TrafficControllerProcess process : localProcesses.values()) {
            process.startDiffusingComputation();
        }
    }
    
    public void stopRegion() {
        for (TrafficControllerProcess process : localProcesses.values()) {
            process.stopProcess();
        }
        registryClient.leaveServer();
        System.out.println("Region stopped.");
    }

    public Map<Integer, TrafficControllerProcess> getLocalProcesses() {
        return localProcesses;
    }

    public int getRegionId() {
        return regionId;
    }
    
    public RegistryClient getRegistryClient() {
        return registryClient;
    }
}
