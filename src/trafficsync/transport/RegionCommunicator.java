package trafficsync.transport;

import trafficsync.common.Message;
import trafficsync.common.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

public class RegionCommunicator {
    private final String nodeId;
    private final String regionId;
    private final String serverHost;
    private final int serverPort;
    private final Consumer<Message> onMessageReceived;
    private final Runnable onDisconnect;
    
    private TCPConnection connection;

    public RegionCommunicator(String nodeId, String regionId, String serverHost, int serverPort, 
                              Consumer<Message> onMessageReceived, Runnable onDisconnect) {
        this.nodeId = nodeId;
        this.regionId = regionId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.onMessageReceived = onMessageReceived;
        this.onDisconnect = onDisconnect;
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
        Message msg = new Message(type, nodeId, receiverId, regionId, payload);
        sendMessage(msg);
    }

    public void sendMessage(MessageType type, String receiverId, Object payload, String snapshotId) {
        Message msg = new Message(type, nodeId, receiverId, regionId, payload, System.currentTimeMillis(), snapshotId);
        sendMessage(msg);
    }
}
