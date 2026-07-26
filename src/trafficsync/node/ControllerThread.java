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
    private final String name;
    private final TrafficNode node;
    private volatile boolean running = true;
    private final LinkedBlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
    private final List<String> outgoingNeighbors;
    private final ChandyLamportManager snapshotManager;

    public ControllerThread(String name, TrafficNode node, List<String> outgoingNeighbors, List<String> incomingNeighbors, TerminalScreen screen) {
        this.name = name;
        this.node = node;
        this.outgoingNeighbors = outgoingNeighbors;
        setDaemon(true);
        
        ChandyLamportManager.MessageSender sender = (type, target, payload, snapshotId) -> {
            node.routeThreadMessage(new Message(type, name, target, null, payload, System.currentTimeMillis(), snapshotId));
        };
        
        this.snapshotManager = new ChandyLamportManager(name, outgoingNeighbors, incomingNeighbors, sender, screen);
    }

    public void enqueueMessage(Message msg) {
        inbox.offer(msg);
    }

    public void abortSnapshot() {
        snapshotManager.abortSnapshot();
    }

    public String getThreadName() {
        return name;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Wait for message with timeout to simulate periodic work
                long sleepTime = 5000 + (long)(Math.random() * 5000);
                Message msg = inbox.poll(sleepTime, TimeUnit.MILLISECONDS);
                
                if (msg != null) {
                    handleMessage(msg);
                } else {
                    // Periodic work (if we timed out)
                    if (running && node.isTrafficGenerationEnabled() && !outgoingNeighbors.isEmpty()) {
                        int cars = (int)(Math.random() * 50);
                        String target = outgoingNeighbors.get((int)(Math.random() * outgoingNeighbors.size()));
                        String payload = name + " processed " + cars + " cars.";
                        node.routeThreadMessage(new Message(MessageType.TRAFFIC_UPDATE, name, target, null, payload));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case START_SNAPSHOT:
                snapshotManager.initiateSnapshot(msg.getSnapshotId());
                break;
            case MARKER:
                snapshotManager.handleMarker(msg);
                break;
            case TRAFFIC_UPDATE:
            case ACCIDENT_ALERT:
                EventQueue.info(name + " received " + msg.getType() + " from " + msg.getSenderId() + ": " + msg.getPayload());
                snapshotManager.recordIncomingMessage(msg);
                break;
            default:
                break;
        }
    }

    public void stopRunning() {
        running = false;
        this.interrupt();
    }
}
