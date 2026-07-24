package diffusing;

import common.Message;
import common.MessageType;
import communication.Communication;
import node.Node;

import java.util.Set;

/**
 * Diffusing Computation Algorithm
 */
public class DiffusingEngine {

    private Node node;
    private int nodeId;
    private Communication communication;

    private boolean visited;
    private int parent = -1;

    public DiffusingEngine(Node node) {

        this.node = node;
        this.nodeId = node.getNodeId();
        this.communication = node.getCommunication();
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

        Set<Integer> neighbors = node.getNeighbors().keySet();

        for (int receiver : neighbors) {
            Message explore = new Message(
                    MessageType.EXPLORE,
                    nodeId,
                    receiver,
                    "EXPLORE"
            );

            communication.send(explore);
        }

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