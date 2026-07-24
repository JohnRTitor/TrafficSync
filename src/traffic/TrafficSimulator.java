package traffic;

import common.Message;
import common.MessageType;
import communication.Communication;

/**
 * Continuously generates application traffic.
 */
public class TrafficSimulator extends Thread {

    private int nodeId;
    private Communication communication;
    private boolean running = true;

    public TrafficSimulator(int nodeId, Communication communication) {

        this.nodeId = nodeId;
        this.communication = communication;
    }

    @Override
    public void run() {

        while (running) {

            try {

                Thread.sleep(3000);

            } catch (InterruptedException e) {

                e.printStackTrace();
            }

            int receiver;

            if (nodeId == 1)
                receiver = 2;
            else if (nodeId == 2)
                receiver = 3;
            else
                receiver = 1;

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