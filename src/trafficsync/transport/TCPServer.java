package trafficsync.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

// This class is a simple wrapper around the standard Java ServerSocket.
// It listens on a specific port in a background thread and triggers a callback
// whenever a new client connects.
public class TCPServer {
    private final int port;
    private final Consumer<Socket> onClientConnected;
    private ServerSocket serverSocket;
    private Thread acceptorThread;
    private volatile boolean running = false;

    public TCPServer(int port, Consumer<Socket> onClientConnected) {
        this.port = port;
        this.onClientConnected = onClientConnected;
    }

    // This starts the server socket and creates a new background thread to wait for connections.
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        acceptorThread = new Thread(this::acceptLoop);
        acceptorThread.setDaemon(true);
        acceptorThread.start();
    }

    // This is the loop that runs in the background. It will pause at serverSocket.accept()
    // until a node tries to connect. Once connected, it passes the new socket to our callback function.
    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                onClientConnected.accept(clientSocket);
            } catch (IOException e) {
                if (running) {
                    System.err.println("TCPServer accept error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
