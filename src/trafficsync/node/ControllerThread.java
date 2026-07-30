package trafficsync.node;

import trafficsync.common.Message;
import trafficsync.common.MessageType;
import trafficsync.snapshot.ChandyLamportManager;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ControllerThread extends Thread {
    // Controller details
    private final String name;
    private final TrafficNode node;
    private volatile boolean running = true;
    private final LinkedBlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
    private final List<String> outgoingNeighbors;
    private final ChandyLamportManager snapshotManager;
    // Initialize controller
    public ControllerThread(
            String name,
            TrafficNode node,
            List<String> outgoingNeighbors,
            List<String> incomingNeighbors,
            TerminalScreen screen) {
        this.name = name;
        this.node = node;
        this.outgoingNeighbors = outgoingNeighbors;
        setDaemon(true);
        // Forward snapshot messages
        ChandyLamportManager.MessageSender sender = (type, target, payload, snapshotId) -> {
            node.routeThreadMessage(new Message(type, name, target, payload, System.currentTimeMillis(), snapshotId));
        };
        // Create snapshot manager
        this.snapshotManager = new ChandyLamportManager(name, outgoingNeighbors, incomingNeighbors, sender);
    }
    // Add message to queue
    public void enqueueMessage(Message msg) {
        inbox.offer(msg);
    }
    // Stop active snapshot
    public void abortSnapshot() {
        snapshotManager.abortSnapshot();
    }
    // Return controller name
    public String getThreadName() {
        return name;
    }

    @Override
    public void run() {
        // Main processing loop
        while (running) {
            try {
                // Wait for incoming message
                long sleepTime = 5000 + (long) (Math.random() * 5000);
                Message msg = inbox.poll(sleepTime, TimeUnit.MILLISECONDS);

                if (msg != null) {
                    handleMessage(msg);
                } else {
                    // Generate traffic update
                    if (running && node.isTrafficGenerationEnabled() && !outgoingNeighbors.isEmpty()) {
                        int cars = (int) (Math.random() * 50);
                        String target = outgoingNeighbors.get((int) (Math.random() * outgoingNeighbors.size()));
                        String payload = name + " processed " + cars + " cars.";

                        node.routeThreadMessage(new Message(MessageType.TRAFFIC_UPDATE, name, target, payload));
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    // Process received message
    private void handleMessage(Message msg) {
        switch (msg.type()) {
            case START_SNAPSHOT -> snapshotManager.initiateSnapshot(msg.snapshotId());
            case MARKER -> snapshotManager.handleMarker(msg);
            case TRAFFIC_UPDATE -> {
                EventQueue.info(name + " received " + msg.type() + " from " + msg.senderId() + ": " + msg.payload());
                snapshotManager.recordIncomingMessage(msg);
            }
            default -> {}
        }
    }
    // Stop controller
    public void stopRunning() {
        running = false;
        this.interrupt();
    }
}
