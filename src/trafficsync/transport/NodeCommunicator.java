package trafficsync.transport;

import trafficsync.common.Message;
import trafficsync.common.MessageType;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

// This class handles the network connection from the perspective of a traffic node.
// It is the client-side counterpart to VpsServer -- while VpsServer listens for connections,
// NodeCommunicator opens a socket to the VPS and manages it. All messages sent by the node
// to other nodes or the server flow through this class. It also delegates incoming messages
// to the TrafficNode's handler via the onMessageReceived callback.
public class NodeCommunicator {
    private String nodeId;
    private final String serverHost;
    private final int serverPort;
    private final Consumer<Message> onMessageReceived;
    private final Runnable onDisconnect;

    private TCPConnection connection;

    public NodeCommunicator(
            String nodeId,
            String serverHost,
            int serverPort,
            Consumer<Message> onMessageReceived,
            Runnable onDisconnect) {
        this.nodeId = nodeId;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.onMessageReceived = onMessageReceived;
        this.onDisconnect = onDisconnect;
    }

    // After registration, the VPS assigns us a formal node ID (like NODE-1).
    // We need to update this communicator so that all future outgoing messages
    // carry the correct sender ID instead of the temporary name.
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    // This method opens a socket to the VPS server and starts the TCPConnection listener.
    // The disconnect callback from TCPConnection is adapted to a simpler Runnable here
    // because the node side does not need to know which TCPConnection object died --
    // there is only ever one connection per node.
    public void start() throws IOException {
        Socket socket = new Socket(serverHost, serverPort);
        connection = new TCPConnection(socket, onMessageReceived, conn -> {
            if (onDisconnect != null) {
                onDisconnect.run();
            }
        });
        connection.start();
    }

    // Closes the underlying TCP connection. After this, the node will not be able
    // to send or receive messages until start() is called again (i.e., a reconnect).
    public void stop() {
        if (connection != null) {
            connection.close();
        }
    }

    // These are convenience overloads so callers do not have to construct Message objects manually.
    // The first variant sends a pre-built Message directly.
    // The second builds a Message from type, receiver, and payload using the current nodeId as sender.
    // The third variant also includes a snapshotId, which is needed for Chandy-Lamport messages.
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
