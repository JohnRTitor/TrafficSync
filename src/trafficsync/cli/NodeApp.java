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
        
        String nodeId = config.get("NODE_ID", "NODE-X");
        String regionId = config.get("REGION_ID", "REGION-X");
        String serverHost = config.get("SERVER_HOST", "127.0.0.1");
        int serverPort = config.getInt("SERVER_PORT", 9000);
        int controllers = config.getInt("CONTROLLER_COUNT", 1);
        String neighbors = config.get("NEIGHBORS", "");
        
        String menu = "[q] Quit Task | [s] Snapshot | [t] Send Traffic | [c] Clear Logs | [x] Exit";
        TerminalScreen screen = new TerminalScreen("Smart Traffic Node: " + nodeId, menu);
        screen.setStatus("Server", serverHost + ":" + serverPort);
        screen.setStatus("Region", regionId);
        screen.setStatus("Controllers", String.valueOf(controllers));
        screen.setStatus("Neighbors", neighbors.isEmpty() ? "None" : neighbors);
        screen.setStatus("Connection", "DISCONNECTED");
        
        TerminalRenderer renderer = new TerminalRenderer(screen);
        renderer.start();
        
        TrafficNode node = new TrafficNode(nodeId, regionId, serverHost, serverPort, neighbors, controllers, screen);
        node.start();
        
        KeyboardInput input = new KeyboardInput(screen, command -> {
            screen.setPromptInput(command);
            switch (command.toLowerCase()) {
                case "x":
                    node.stop();
                    renderer.stop();
                    System.exit(0);
                    break;
                case "s":
                    node.triggerSnapshot();
                    break;
                case "t":
                    node.sendTrafficUpdate("Manual traffic update from " + nodeId);
                    break;
                case "c":
                    screen.clearLogs();
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
