package traffic;

import common.Message;
import common.MessageType;
import communication.Communication;
import node.TrafficControllerProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Continuously generates application traffic.
 */
public class TrafficSimulator extends Thread {

    private TrafficControllerProcess node;
    private int nodeId;
    private Communication communication;
    private boolean running = true;
    private final Random random = new Random();

    public TrafficSimulator(TrafficControllerProcess node) {

        this.node = node;
        this.nodeId = node.getNodeId();
        this.communication = node.getCommunication();
    }

    @Override
    public void run() {

        while (running) {

            try {

                Thread.sleep(3000);

            } catch (InterruptedException e) {

                e.printStackTrace();
            }

            List<Integer> neighbors = new ArrayList<>(node.getNeighbors().keySet());
            if (neighbors.isEmpty()) continue;
            
            int receiver = neighbors.get(random.nextInt(neighbors.size()));

            Message message = new Message(
                    MessageType.TRAFFIC,
                    nodeId,
                    receiver,
                    "Application Message");

            communication.send(message);

        }

    }

    public void stopTraffic() {

        running = false;

    }

}