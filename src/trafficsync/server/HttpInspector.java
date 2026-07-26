package trafficsync.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpInspector {
    private final int port;
    private final VpsServer vpsServer;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread thread;

    public HttpInspector(int port, VpsServer vpsServer) {
        this.port = port;
        this.vpsServer = vpsServer;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        thread = new Thread(this::listen);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private void listen() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                handleRequest(socket);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void handleRequest(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null) return;

            String[] parts = line.split(" ");
            if (parts.length >= 2 && parts[0].equals("GET")) {
                String path = parts[1];
                String responseBody = route(path);

                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Content-Length: " + responseBody.length());
                out.println("Connection: close");
                out.println();
                out.println(responseBody);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private String route(String path) {
        NodeRegistry registry = vpsServer.getRegistry();
        switch (path) {
            case "/ping":
                return "{\"status\": \"ok\"}";
            case "/nodes":
                return "{\"nodes\": " + toJsonArray(registry.getNodes()) + "}";
            case "/topology":
                return "{\"topology\": " + registry.getTopology().toString().replace("=", ":") + "}";
            case "/regions":
                return "{\"regions\": " + registry.getNodeRegions().toString().replace("=", ":") + "}";
            case "/snapshot/status":
                return "{\"snapshots\": " + vpsServer.getSnapshotStates().toString().replace("=", ":") + "}";
            default:
                return "{\"error\": \"Not found\"}";
        }
    }
    
    private String toJsonArray(Iterable<String> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String item : items) {
            if (!first) sb.append(", ");
            sb.append("\"").append(item).append("\"");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
