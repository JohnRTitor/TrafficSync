package aggregator;

import common.Message;
import handler.MessageHandler;

public class SnapshotReceiver extends MessageHandler {

    private SnapshotCollector collector;

    public SnapshotReceiver(SnapshotCollector collector) {
        this.collector = collector;
    }

    @Override
    public void onMessage(Message message) {
        System.out.println("[Aggregator] Message Received from Site " + message.getSenderId());
        
        switch (message.getType()) {
            case SNAPSHOT_REPORT:
                collector.receiveSnapshot(message.getSenderId(), message.getPayload());
                break;
            default:
                System.out.println("[Aggregator] Ignored non-snapshot message: " + message.getType());
        }
    }
}
