package trafficsync.terminal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventQueue {
    // Shared non-blocking event channel between background work and the terminal UI.
    private static final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    
    // Convenience logging methods for the application's event categories.
    public static void push(Event.Level level, String message) {
        queue.offer(new Event(level, message));
    }
    
    public static void info(String message) { push(Event.Level.INFO, message); }
    public static void warn(String message) { push(Event.Level.WARN, message); }
    public static void error(String message) { push(Event.Level.ERROR, message); }
    public static void network(String message) { push(Event.Level.NETWORK, message); }
    public static void snapshot(String message) { push(Event.Level.SNAPSHOT, message); }
    
    public static Event poll() {
        return queue.poll();
    }
}
