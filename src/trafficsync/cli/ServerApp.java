package trafficsync.cli;

import trafficsync.config.EnvReader;
import trafficsync.server.VpsServer;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;

// This is the main entry point for the VPS (central coordinator) side of our traffic network.
// It ties together three major components: the EnvReader for configuration, the VpsServer
// for handling all node connections and message routing, and the TerminalScreen for the
// interactive dashboard. The flow is: read config -> build UI -> start TCP listener -> handle user commands.
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

        // At this point the server is running and accepting connections in the background.
        // We now hand control to the terminal UI, which blocks on this thread and waits for user input.
        // The lambda below acts as a command dispatcher -- every keystroke-enter pair from the user
        // arrives here as a string, and we figure out which server action to invoke.
        screen.start(command -> {
            screen.setPromptInput(command);
            String[] parts = command.split(" ", 3);
            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                // Graceful shutdown: stop accepting new connections, close all sockets, then kill the UI.
                case "x" -> {
                    server.stop();
                    screen.stop();
                    System.exit(0);
                }
                case "c" -> screen.clearLogs();
                // The 's' command kicks off the Chandy-Lamport global snapshot across all connected sites.
                // The VpsServer sends SNAPSHOT_TRIGGER to every node and waits for their responses.
                case "s" -> server.triggerGlobalSnapshot();
                case "m" -> {
                    if (parts.length >= 3) {
                        server.sendMessageToNode(parts[1], parts[2]);
                    } else {
                        EventQueue.warn("Usage: m <nodeId> <message>");
                    }
                }
                // Broadcast sends the same message to every connected node at once.
                // We strip the 'b' prefix and use the rest of the input as the message body.
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
