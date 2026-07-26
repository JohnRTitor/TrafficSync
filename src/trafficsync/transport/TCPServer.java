package trafficsync.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

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

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        acceptorThread = new Thread(this::acceptLoop);
        acceptorThread.setDaemon(true);
        acceptorThread.start();
    }

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
