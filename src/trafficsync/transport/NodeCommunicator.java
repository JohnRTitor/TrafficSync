package trafficsync.transport;

import trafficsync.common.Message;
import trafficsync.common.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

// This class handles the network connection from the perspective of a traffic node.
// It connects to the central VPS server and forwards any received messages to our local logic.
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

    // This method opens a socket to the VPS server and starts the TCPConnection listener.
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

    // These are helper methods so we don't have to construct Message objects manually every time.
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
