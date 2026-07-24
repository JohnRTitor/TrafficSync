package diffusing;

import common.Message;
import common.MessageType;
import communication.Communication;

/**
 * Diffusing Computation Algorithm
 */
public class DiffusingEngine {

    private int nodeId;
    private Communication communication;

    private boolean visited;
    private int parent = -1;

    public DiffusingEngine(int nodeId, Communication communication) {

        this.nodeId = nodeId;
        this.communication = communication;
        this.visited = false;
    }

    /**
     * Coordinator starts the computation.
     */
    public void startDiffusing() {

        visited = true;

        System.out.println("\n=================================");
        System.out.println("Node " + nodeId + " started Diffusing Computation");
        System.out.println("=================================");

        sendExplore();

    }

    /**
     * Receive EXPLORE.
     */
    public void receiveExplore(Message message) {

        System.out.println("Node "
                + nodeId
                + " received EXPLORE from Node "
                + message.getSenderId());

        if (!visited) {

            visited = true;

            parent = message.getSenderId();

            sendExplore();

        } else {

            sendEcho(message.getSenderId());

        }

    }

    /**
     * Receive ECHO.
     */
    public void receiveEcho(Message message) {

        System.out.println("Node "
                + nodeId
                + " received ECHO from Node "
                + message.getSenderId());

        if (parent != -1) {

            System.out.println("Returning ECHO to parent Node " + parent);

            sendEcho(parent);

        } else {

            System.out.println("Diffusing Computation Completed.");

        }

    }

    /**
     * Send EXPLORE message.
     */
    private void sendExplore() {

        int receiver;

        switch (nodeId) {

            case 1:
                receiver = 2;
                break;

            case 2:
                receiver = 3;
                break;

            default:
                receiver = 1;

        }

        Message explore = new Message(
                MessageType.EXPLORE,
                nodeId,
                receiver,
                "EXPLORE"
        );

        communication.send(explore);

    }

    /**
     * Send ECHO message.
     */
    private void sendEcho(int receiver) {

        Message echo = new Message(
                MessageType.ECHO,
                nodeId,
                receiver,
                "ECHO"
        );

        communication.send(echo);

    }

}