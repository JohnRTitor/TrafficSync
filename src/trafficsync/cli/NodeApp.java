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
        int nodePort = config.getInt("NODE_PORT", 5000);
        
        String menu = "[q] Quit Task | [s] Local Snapshot | [t] Send Traffic | [m <id> <msg>] Send Manual Msg | [p] List Sites | [n] Show Neighbors | [c] Clear Logs | [x] Exit";
        TerminalScreen screen = new TerminalScreen("Smart Traffic Node: " + nodeId, menu);
        screen.setStatus("Server", serverHost + ":" + serverPort);
        screen.setStatus("Region", regionId);
        screen.setStatus("Controllers", String.valueOf(controllers));
        screen.setStatus("Node Port", String.valueOf(nodePort));
        screen.setStatus("Peers", "0");
        screen.setStatus("Neighbors", "None");
        screen.setStatus("Connection", "DISCONNECTED");
        
        TerminalRenderer renderer = new TerminalRenderer(screen);
        renderer.start();
        
        TrafficNode node = new TrafficNode(nodeId, regionId, serverHost, serverPort, nodePort, controllers, screen);
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
                case "q":
                    if (parts.length > 1) {
                        screen.cancelTask(parts[1]);
                    } else {
                        screen.cancelLatestTask();
                    }
                    break;
                case "s":
                    node.triggerLocalSnapshot();
                    break;
                case "t":
                    node.triggerTrafficUpdate();
                    break;
                case "m":
                    if (parts.length >= 3) {
                        node.sendManualMessage(parts[1], parts[2]);
                    } else {
                        EventQueue.warn("Usage: m <nodeId> <message>");
                    }
                    break;
                case "p":
                    node.printPeers();
                    break;
                case "n":
                    node.printNeighbors();
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
