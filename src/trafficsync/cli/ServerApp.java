package trafficsync.cli;

import trafficsync.config.EnvReader;
import trafficsync.server.HttpInspector;
import trafficsync.server.VpsServer;
import trafficsync.terminal.EventQueue;
import trafficsync.terminal.KeyboardInput;
import trafficsync.terminal.TerminalRenderer;
import trafficsync.terminal.TerminalScreen;

public class ServerApp {
    public static void main(String[] args) {
        String envPath = args.length > 0 ? args[0] : ".env";
        EnvReader config = new EnvReader(envPath);
        
        int tcpPort = config.getInt("SERVER_PORT", 9000);
        int httpPort = config.getInt("HTTP_PORT", 8080);
        
        String menu = "[q] Quit Current Task | [r] Refresh | [c] Clear Logs | [x] Exit";
        TerminalScreen screen = new TerminalScreen("VPS Coordinator Server", menu);
        screen.setStatus("Server IP", "0.0.0.0");
        screen.setStatus("TCP Port", String.valueOf(tcpPort));
        screen.setStatus("HTTP Port", String.valueOf(httpPort));
        
        TerminalRenderer renderer = new TerminalRenderer(screen);
        renderer.start();
        
        VpsServer server = new VpsServer(tcpPort, screen);
        HttpInspector inspector = new HttpInspector(httpPort, server);
        
        try {
            server.start();
            inspector.start();
        } catch (Exception e) {
            EventQueue.error("Server failed to start: " + e.getMessage());
        }
        
        KeyboardInput input = new KeyboardInput(screen, command -> {
            switch (command.toLowerCase()) {
                case "x":
                    server.stop();
                    inspector.stop();
                    renderer.stop();
                    System.exit(0);
                    break;
                case "c":
                    screen.clearLogs();
                    break;
                case "r":
                    // Redraw triggers naturally
                    break;
                default:
                    EventQueue.warn("Unknown command: " + command);
            }
        });
        input.start();
        
        // Block main thread
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
