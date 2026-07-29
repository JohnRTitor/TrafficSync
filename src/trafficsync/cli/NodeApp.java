package trafficsync.cli;

import trafficsync.config.EnvReader;
import trafficsync.node.TrafficNode;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.KeyboardInput;
import trafficsync.terminal.TerminalRenderer;
import trafficsync.terminal.TerminalScreen;

public class NodeApp {
    public static void main(String[] args) {
        String envPath = args.length > 0 ? args[0] : ".env";
        EnvReader config = new EnvReader(envPath);
        
        String nodeName = config.get("NODE_NAME", "NODE-A");
        String serverHost = config.get("SERVER_HOST", "127.0.0.1");
        int serverPort = config.getInt("SERVER_PORT", 9000);
        int controllers = config.getInt("CONTROLLER_COUNT", 1);
        
        String menu = "[s] Local Snapshot | [t] Toggle Traffic | [m <id> <msg>] Send Manual Msg | [i <name>] Query ID | [p] List Connected Sites | [c] Clear Logs | [r] Reconnect | [x] Exit";
        TerminalScreen screen = new TerminalScreen("Smart Traffic Node: " + nodeName, menu);
        screen.setStatus("Server", serverHost + ":" + serverPort);
        screen.setStatus("Node ID", "PENDING");
        screen.setStatus("Controllers", String.valueOf(controllers));
        screen.setStatus("Peers", "0");
        screen.setStatus("Connection", "DISCONNECTED");
        
        TerminalRenderer renderer = new TerminalRenderer(screen);
        renderer.start();
        
        TrafficNode node = new TrafficNode(nodeName, serverHost, serverPort, controllers, screen);
        node.start();
        
        KeyboardInput input = new KeyboardInput(screen, command -> {
            screen.setPromptInput(command);
            String[] parts = command.split(" ", 3);
            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                case "x":
                    node.stop();
                    renderer.stop();
                    System.exit(0);
                    break;

                case "s":
                    node.triggerLocalSnapshot();
                    break;
                case "t":
                    node.toggleTrafficGeneration();
                    break;
                case "m":
                    if (parts.length >= 3) {
                        node.sendManualMessage(parts[1], parts[2]);
                    } else {
                        EventQueue.warn("Usage: m <nodeId/nodeName/server/vps> <message>");
                    }
                    break;
                case "i":
                    if (parts.length == 1 || (parts.length >= 2 && parts[1].equalsIgnoreCase("self"))) {
                        node.querySelfNodeId();
                    } else if (parts.length >= 2) {
                        node.queryNodeId(parts[1]);
                    }
                    break;
                case "p":
                    node.printPeers();
                    break;
                case "c":
                    screen.clearLogs();
                    break;
                case "r":
                    node.reconnect();
                    break;
                default:
                    EventQueue.warn("Unknown command: " + command);
            }
        });
        input.start();
        
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
