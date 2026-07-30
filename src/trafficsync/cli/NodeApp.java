package trafficsync.cli;

import trafficsync.config.EnvReader;
import trafficsync.node.TrafficNode;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;

public class NodeApp {
    public static void main(String[] args) {
        String envPath = args.length > 0 ? args[0] : ".env";
        EnvReader config = new EnvReader(envPath);
        
        String nodeName = config.get("NODE_NAME", "NODE-A");
        String serverHost = config.get("SERVER_HOST", "127.0.0.1");
        int serverPort = config.getInt("SERVER_PORT", 9000);
        int controllers = config.getInt("CONTROLLER_COUNT", 1);
        
        String menu = """
                      [s] Snapshot | [t] Toggle Traffic | [m <node> <msg>] Send | [i <node>] Info
                      [p] Peers | [c] Clear | [r] Reconnect | [x] Exit""";
        TerminalScreen screen = new TerminalScreen("Smart Traffic Node: " + nodeName, menu);
        screen.setStatus("Server", serverHost + ":" + serverPort);
        screen.setStatus("Node ID", "PENDING");
        screen.setStatus("Controllers", String.valueOf(controllers));
        screen.setStatus("Peers", "0");
        screen.setStatus("Connection", "DISCONNECTED");
        
        TrafficNode node = new TrafficNode(nodeName, serverHost, serverPort, controllers, screen);
        node.start();
        
        screen.start(command -> {
            screen.setPromptInput(command);
            String[] parts = command.split(" ", 3);
            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                case "x" -> {
                    node.stop();
                    screen.stop();
                    System.exit(0);
                }
                case "s" -> node.triggerLocalSnapshot();
                case "t" -> node.toggleTrafficGeneration();
                case "m" -> {
                    if (parts.length >= 3) {
                        node.sendManualMessage(parts[1], parts[2]);
                    } else {
                        EventQueue.warn("Usage: m <nodeId/nodeName/server/vps> <message>");
                    }
                }
                case "i" -> {
                    if (parts.length == 1 || (parts.length >= 2 && parts[1].equalsIgnoreCase("self"))) {
                        node.querySelfNodeId();
                    } else if (parts.length >= 2) {
                        node.queryNodeId(parts[1]);
                    }
                }
                case "p" -> node.printPeers();
                case "c" -> screen.clearLogs();
                case "r" -> node.reconnect();
                default -> EventQueue.warn("Unknown command: " + command);
            }
        });
    }
}
