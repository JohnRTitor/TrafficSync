package trafficsync.transport;

import trafficsync.common.Message;
import trafficsync.common.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class NodeCommunicator {
    private String nodeId;
    private final String serverHost;
    private final int serverPort;
    private final Consumer<Message> onMessageReceived;
    private final Runnable onDisconnect;
    
    private TCPConnection connection;

    public NodeCommunicator(String nodeId, String serverHost, int serverPort, 
                              Consumer<Message> onMessageReceived, Runnable onDisconnect) {
        this.nodeId = nodeId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.onMessageReceived = onMessageReceived;
        this.onDisconnect = onDisconnect;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void start() throws IOException {
        Socket socket = new Socket(serverHost, serverPort);
        connection = new TCPConnection(socket, onMessageReceived, conn -> {
            if (onDisconnect != null) {
                onDisconnect.run();
            }
        });
        connection.start();
    }

    public void stop() {
        if (connection != null) {
            connection.close();
        }
    }

    public void sendMessage(Message message) {
        if (connection != null) {
            connection.send(message);
        }
    }

    public void sendMessage(MessageType type, String receiverId, Object payload) {
        Message msg = new Message(type, nodeId, receiverId, payload);
        sendMessage(msg);
    }

    public void sendMessage(MessageType type, String receiverId, Object payload, String snapshotId) {
        Message msg = new Message(type, nodeId, receiverId, payload, System.currentTimeMillis(), snapshotId);
        sendMessage(msg);
    }
}
