package communication;

import common.Message;
import handler.MessageHandler;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalCommunication implements Communication {

    private final int nodeId;
    private final MessageHandler messageHandler;
    private final Map<Integer, BlockingQueue<Message>> router;
    private final BlockingQueue<Message> incomingQueue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread receiverThread;

    public LocalCommunication(int nodeId, MessageHandler messageHandler, Map<Integer, BlockingQueue<Message>> router) {
        this.nodeId = nodeId;
        this.messageHandler = messageHandler;
        this.router = router;
        this.incomingQueue = new LinkedBlockingQueue<>();
        this.router.put(nodeId, this.incomingQueue);
    }

    @Override
    public void start() {
        running.set(true);
        receiverThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Message msg = incomingQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (msg != null && messageHandler != null) {
                        messageHandler.onMessage(msg);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "LocalComm-Node-" + nodeId);
        receiverThread.start();
        System.out.println("Local Communication started for node " + nodeId);
    }

    @Override
    public void send(Message message) {
        BlockingQueue<Message> destQueue = router.get(message.getReceiverId());
        if (destQueue != null) {
            destQueue.offer(message);
        } else {
            System.out.println("Destination Node " + message.getReceiverId() + " not found in local topology.");
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        router.remove(nodeId);
        System.out.println("Local Communication stopped for node " + nodeId);
    }
}
