package trafficsync.terminal;

// This class holds the details of a single log message that appears in the terminal.
// It keeps track of when the event happened and how severe it is (like an error vs regular info).
// Events are created by various parts of the system (via EventQueue) and consumed by the
// TerminalScreen to render color-coded, timestamped log entries.
public class Event {
    // These categories help us color-code the output in the terminal.
    public enum Level {
        INFO,
        WARN,
        ERROR,
        SNAPSHOT,
        NETWORK,
        USER
    }

    // We make these final because an event should not change after it has happened.
    private final Level level;
    private final String message;
    private final long timestamp;

    public Event(Level level, String message) {
        this.level = level;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // The LogListBox uses the level to pick a color for each log entry
    // (e.g., red for errors, green for network events).
    public Level getLevel() {
        return level;
    }

    // Returns the human-readable text that will be shown in the log panel.
    public String getMessage() {
        return message;
    }

    // The timestamp is formatted into HH:mm:ss by the LogListBox renderer
    // so the user can see exactly when each event occurred.
    public long getTimestamp() {
        return timestamp;
    }
}
