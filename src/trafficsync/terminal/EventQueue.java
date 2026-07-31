package trafficsync.terminal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// This acts as a mailbox between our background network threads and the user interface.
// Network threads drop events in here, and the terminal reads them out to display on screen.
public class EventQueue {
    // We use a BlockingQueue because it is thread-safe. If multiple nodes send updates at the exact
    // same millisecond, the queue will line them up without losing any data.
    private static final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();

    // These are shortcut methods. Instead of creating an Event object manually every time,
    // we can just call EventQueue.info("something happened").
    public static void push(Event.Level level, String message) {
        queue.offer(new Event(level, message));
    }

    public static void info(String message) {
        push(Event.Level.INFO, message);
    }

    public static void warn(String message) {
        push(Event.Level.WARN, message);
    }

    public static void error(String message) {
        push(Event.Level.ERROR, message);
    }

    public static void network(String message) {
        push(Event.Level.NETWORK, message);
    }

    public static void snapshot(String message) {
        push(Event.Level.SNAPSHOT, message);
    }

    // This is the consumer side. The TerminalScreen's background event thread calls this
    // method in a loop to pull events off the queue and display them in the log panel.
    // poll() returns null immediately if the queue is empty, so the caller sleeps briefly
    // before trying again to avoid spinning the CPU.
    public static Event poll() {
        return queue.poll();
    }
}
