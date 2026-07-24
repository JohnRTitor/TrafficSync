package communication;

import common.Message;
import handler.MessageHandler;

public class SocketCommunication implements Communication {

    private final Server server;
    private final Client client;
    private final int port;
    private final RegistryClient registryClient;

    /**
     * Create communication object for this node.
     *
     * @param listeningPort Port on which this node will receive messages.
     * @param messageHandler Handler for received messages.
     * @param registryClient Registry client to look up peers.
     */
    public SocketCommunication(int listeningPort, MessageHandler messageHandler, RegistryClient registryClient) {

        this.port = listeningPort;
        this.registryClient = registryClient;

        server = new Server(listeningPort, messageHandler);
        client = new Client();
    }

    /**
     * Start the communication server.
     */
    @Override
    public void start() {

        System.out.println();
        System.out.println("=================================");
        System.out.println(" Starting Communication Server");
        System.out.println("=================================");
        System.out.println("Server is listening on Port : " + port);
        System.out.println("Waiting for incoming messages...");
        System.out.println("=================================\n");

        server.start();
    }

    /**
     * Send a message to another node.
     */
    @Override
    public void send(Message message) {
        // Refresh peers before sending
        registryClient.loadPeers();
        
        Integer receiverPort = registryClient.getNeighbors().get(message.getReceiverId());
        
        if (receiverPort != null && receiverPort > 0) {
            client.send("localhost", receiverPort, message);
        } else {
            System.out.println("Destination Node " + message.getReceiverId() + " is not registered or not a neighbor.");
        }
    }

    /**
     * Stop the communication service.
     */
    @Override
    public void stop() {

        server.interrupt();

        System.out.println();
        System.out.println("=================================");
        System.out.println(" Communication Stopped");
        System.out.println("=================================");
    }
}