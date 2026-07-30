package trafficsync.cli;

import trafficsync.config.EnvReader;
import trafficsync.server.VpsServer;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.TerminalScreen;

public class ServerApp {
    public static void main(String[] args) {
        String envPath = args.length > 0 ? args[0] : ".env";
        EnvReader config = new EnvReader(envPath);
        
        int tcpPort = config.getInt("SERVER_PORT", 9000);
        
        String menu = "[s] Snapshot All | [m <node> <msg>] Send Msg | [b <msg>] Broadcast | [c] Clear Logs | [x] Exit";
        TerminalScreen screen = new TerminalScreen("VPS Coordinator Server", menu);
        screen.setStatus("Server IP", "0.0.0.0");
        screen.setStatus("TCP Port", String.valueOf(tcpPort));
        
        VpsServer server = new VpsServer(tcpPort, screen);
        
        try {
            server.start();
        } catch (Exception e) {
            EventQueue.error("Server failed to start: " + e.getMessage());
        }
        
        screen.start(command -> {
            screen.setPromptInput(command);
            String[] parts = command.split(" ", 3);
            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                case "x":
                    server.stop();
                    screen.stop();
                    System.exit(0);
                    break;

                case "c":
                    screen.clearLogs();
                    break;
                case "s":
                    server.triggerGlobalSnapshot();
                    break;
                case "m":
                    if (parts.length >= 3) {
                        server.sendMessageToNode(parts[1], parts[2]);
                    } else {
                        EventQueue.warn("Usage: m <nodeId> <message>");
                    }
                    break;
                case "b":
                    String bMsg = command.substring(1).trim();
                    if (!bMsg.isEmpty()) {
                        server.broadcastMessage(bMsg);
                    } else {
                        EventQueue.warn("Usage: b <message>");
                    }
                    break;
                default:
                    EventQueue.warn("Unknown command: " + command);
            }
        });
    }
}
