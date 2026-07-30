package trafficsync.cli;
import trafficsync.config.EnvReader;
import trafficsync.node.TrafficNode;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;

public class NodeApp {
    // Entry point
    public static void main(String[] args) {
        // Load configuration
        String envPath = args.length > 0 ? args[0] : ".env";
        EnvReader config = new EnvReader(envPath);
        // Read settings
        String nodeName = config.get("NODE_NAME", "NODE-A");
        String serverHost = config.get("SERVER_HOST", "127.0.0.1");
        int serverPort = config.getInt("SERVER_PORT", 9000);
        int controllers = config.getInt("CONTROLLER_COUNT", 1);
        // Command menu
        String menu = """
                      [s] Snapshot | [t] Toggle Traffic | [m <node> <msg>] Send | [i <node>] Info
                      [p] Peers | [c] Clear | [r] Reconnect | [x] Exit""";

        // Create terminal
        TerminalScreen screen = new TerminalScreen("Smart Traffic Node: " + nodeName, menu);
        // Initial status
        screen.setStatus("Server", serverHost + ":" + serverPort);
        screen.setStatus("Node ID", "PENDING");
        screen.setStatus("Controllers", String.valueOf(controllers));
        screen.setStatus("Peers", "0");
        screen.setStatus("Connection", "DISCONNECTED");
        // Create node
        TrafficNode node = new TrafficNode(nodeName, serverHost, serverPort, controllers, screen);
        // Start node
        node.start();
        // Handle commands
        screen.start(command -> {
            screen.setPromptInput(command);
            // Parse command
            String[] parts = command.split(" ", 3);
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                // Exit
                case "x" -> {
                    node.stop();
                    screen.stop();
                    System.exit(0);
                }
                // Take snapshot
                case "s" -> node.triggerLocalSnapshot();
                // Toggle traffic
                case "t" -> node.toggleTrafficGeneration();
                // Send message
                case "m" -> {
                    if (parts.length >= 3) {
                        node.sendManualMessage(parts[1], parts[2]);
                    } else {
                        EventQueue.warn("Usage: m <nodeId/nodeName/server/vps> <message>");
                    }
                }
                // Show node info
                case "i" -> {
                    if (parts.length == 1 || (parts.length >= 2 && parts[1].equalsIgnoreCase("self"))) {
                        node.querySelfNodeId();
                    } else if (parts.length >= 2) {
                        node.queryNodeId(parts[1]);
                    }
                }
                // Show peers
                case "p" -> node.printPeers();
                // Clear logs
                case "c" -> screen.clearLogs();
                // Reconnect
                case "r" -> node.reconnect();
                // Invalid command
                default -> EventQueue.warn("Unknown command: " + command);
            }
        });
    }
}