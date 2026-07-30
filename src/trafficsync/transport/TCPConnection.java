package trafficsync.transport;

import trafficsync.common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

// This class represents a single active socket connection between a client and a server.
// It uses Java's built-in ObjectInputStream and ObjectOutputStream so we can send our Message objects directly.
public class TCPConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Consumer<Message> onMessageReceived;
    private final Consumer<TCPConnection> onDisconnected;
    private Thread listenerThread;
    private volatile boolean running = true;
    // We pass in two callbacks: one to run when a message arrives, and one for when the connection dies.
    public TCPConnection(Socket socket, Consumer<Message> onMessageReceived, Consumer<TCPConnection> onDisconnected)
            throws IOException {
        this.socket = socket;
        this.onMessageReceived = onMessageReceived;
        this.onDisconnected = onDisconnected;

        // It is very important to create the ObjectOutputStream first. If both sides create the input stream
        // first, they will sit forever waiting for a header and the program will freeze.
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    // This spins up a background thread that will constantly listen for incoming data.
    public void start() {
        listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // This is the loop that runs in the background thread. It stays blocked on readObject()
    // until a new message arrives over the network.
    private void listen() {
        try {
            while (running && !socket.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    onMessageReceived.accept((Message) obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // When the socket is closed or the connection drops, it throws an exception here.
            // We just let it fall through to the finally block to clean up.
        } finally {
            close();
        }
    }

    // This method writes a message out to the socket. It is synchronized because
    // multiple threads in our application might try to send a message to the same place at the same time.
    public synchronized void send(Message message) {
        if (running && !socket.isClosed()) {
            try {
                out.writeObject(message);
                out.flush();
                // Java caches sent objects by default. If we don't call reset(), it will use up all our memory
                // if we send thousands of traffic updates over time.
                out.reset();
            } catch (IOException e) {
                close();
            }
        }
    }

    public void close() {
        if (!running) return;
        running = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignored
        }
        if (onDisconnected != null) {
            onDisconnected.accept(this);
        }
    }
}
