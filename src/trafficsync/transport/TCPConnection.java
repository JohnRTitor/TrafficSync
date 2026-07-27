package trafficsync.transport;

import trafficsync.common.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class TCPConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Consumer<Message> onMessageReceived;
    private final Consumer<TCPConnection> onDisconnected;
    private Thread listenerThread;
    private volatile boolean running = true;
    public TCPConnection(Socket socket, Consumer<Message> onMessageReceived, Consumer<TCPConnection> onDisconnected) throws IOException {
        this.socket = socket;
        this.onMessageReceived = onMessageReceived;
        this.onDisconnected = onDisconnected;
        
        // Output stream must be initialized first in Java Object Serialization to send header
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }
    public void start() {
        listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listen() {
        try {
            while (running && !socket.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    onMessageReceived.accept((Message) obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Socket closed or error
        } finally {
            close();
        }
    }

    public synchronized void send(Message message) {
        if (running && !socket.isClosed()) {
            try {
                out.writeObject(message);
                out.flush();
                out.reset(); // prevent memory leak of cached objects
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
