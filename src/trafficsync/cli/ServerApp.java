package trafficsync.cli;

import trafficsync.config.EnvReader;
import trafficsync.server.VpsServer;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;

// This is the main class that starts the central server for our traffic network.
// It hosts the VPS coordinator and displays a terminal interface so we can monitor all sites.
public class ServerApp {

    // The main method starts the server. It reads the port number from a file,
    // builds the user interface, starts accepting connections, and then listens
    // for commands typed by the user.
    public static void main(String[] args) {
        // Read the configuration file to find out which port the server should listen on.
        // If no file is provided, we use the default .env file.
        String envPath = args.length > 0 ? args[0] : ".env";
        EnvReader config = new EnvReader(envPath);

        int tcpPort = config.getInt("SERVER_PORT", 9000);

        String menu = """
            [s] Snapshot All | [m <node> <msg>] Send Msg | [b <msg>] Broadcast
            [c] Clear Logs | [x] Exit\
            """;

        // We set up the visual terminal screen before we actually start the server logic.
        // This makes sure the user sees the dashboard immediately even if the server takes time to load.
        TerminalScreen screen = new TerminalScreen("VPS Coordinator Server", menu);
        screen.setStatus("Server IP", "0.0.0.0");
        screen.setStatus("TCP Port", String.valueOf(tcpPort));

        // Create the actual server object that will handle incoming node connections.
        VpsServer server = new VpsServer(tcpPort, screen);

        try {
            // Attempt to open the server port and start listening for traffic nodes.
            server.start();
        } catch (java.io.IOException e) {
            // If the port is already in use or another network error happens, we show an error message.
            EventQueue.error("Server failed to start: " + e.getMessage());
        }

        // We set up a callback function that handles any commands the user types into the terminal.
        // This allows us to interact with the server while it is running in the background.
        screen.start(command -> {
            screen.setPromptInput(command);
            String[] parts = command.split(" ", 3);
            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                case "x" -> {
                    server.stop();
                    screen.stop();
                    System.exit(0);
                }
                case "c" -> screen.clearLogs();
                case "s" -> server.triggerGlobalSnapshot();
                case "m" -> {
                    if (parts.length >= 3) {
                        server.sendMessageToNode(parts[1], parts[2]);
                    } else {
                        EventQueue.warn("Usage: m <nodeId> <message>");
                    }
                }
                case "b" -> {
                    String bMsg = command.substring(1).trim();
                    if (!bMsg.isEmpty()) {
                        server.broadcastMessage(bMsg);
                    } else {
                        EventQueue.warn("Usage: b <message>");
                    }
                }
                default -> EventQueue.warn("Unknown command: " + command);
            }
        });
    }
}
