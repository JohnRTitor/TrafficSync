package trafficsync.terminal;

// This class holds the details of a single log message.
// It keeps track of when the event happened and how severe it is (like an error vs regular info).
public class Event {
    // These categories help us color-code the output in the terminal.
    public enum Level { INFO, WARN, ERROR, SNAPSHOT, NETWORK, USER }
    
    // We make these final because an event should not change after it has happened.
    private final Level level;
    private final String message;
    private final long timestamp;

    public Event(Level level, String message) {
        this.level = level;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public Level getLevel() { return level; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
