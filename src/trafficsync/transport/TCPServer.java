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
    // We use volatile here because the main thread might call stop() while the acceptor thread
    // is checking this flag. Without volatile, the acceptor thread could see a stale cached value.
    private volatile boolean running = false;

    public TCPServer(int port, Consumer<Socket> onClientConnected) {
        this.port = port;
        this.onClientConnected = onClientConnected;
    }

    // This starts the server socket and creates a new background thread to wait for connections.
    // The thread is marked as a daemon so it does not prevent the JVM from shutting down
    // when the main thread exits -- we do not want a leftover listener keeping the process alive.
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

    // Gracefully shuts down the server by flipping the running flag and closing the socket.
    // Closing the socket while accept() is blocked causes it to throw an IOException,
    // which breaks the acceptLoop and lets the thread terminate naturally.
    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // We ignore this because we are already shutting down.
            }
        }
    }
}
