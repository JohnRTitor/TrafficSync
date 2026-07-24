package handler;

import common.Message;
import common.MessageType;
import diffusing.DiffusingEngine;
import snapshot.SnapshotEngine;

/**
 * Receives all incoming messages and forwards them
 * to the appropriate algorithm.
 */
public class MessageHandler {

    private SnapshotEngine snapshotEngine;
    private DiffusingEngine diffusingEngine;

    public MessageHandler() {

    }

    /**
     * Connect Snapshot Engine
     */
    public void setSnapshotEngine(SnapshotEngine snapshotEngine) {
        this.snapshotEngine = snapshotEngine;
    }

    /**
     * Connect Diffusing Engine
     */
    public void setDiffusingEngine(DiffusingEngine diffusingEngine) {
        this.diffusingEngine = diffusingEngine;
    }

    /**
     * Called whenever a message arrives.
     */
    public void onMessage(Message message) {

        System.out.println("\n==============================");
        System.out.println("Message Received");
        System.out.println("==============================");
        System.out.println("Type      : " + message.getType());
        System.out.println("Sender    : " + message.getSenderId());
        System.out.println("Receiver  : " + message.getReceiverId());
        System.out.println("Payload   : " + message.getPayload());

        switch (message.getType()) {

            case TRAFFIC:

                if (snapshotEngine != null) {
                    snapshotEngine.recordTraffic(message);
                }

                break;

            case MARKER:

                if (snapshotEngine != null) {
                    snapshotEngine.receiveMarker(message);
                }

                break;

            case EXPLORE:

                if (diffusingEngine != null) {
                    diffusingEngine.receiveExplore(message);
                }

                break;

            case ECHO:

                if (diffusingEngine != null) {
                    diffusingEngine.receiveEcho(message);
                }

                break;

            case SNAPSHOT_REPORT:

                System.out.println("Snapshot Report Received.");

                break;

            default:

                System.out.println("Unknown Message Type.");

        }

    }

}