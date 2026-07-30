package trafficsync.terminal;

public class Event {
    // Log severity categories used by the terminal renderer.
    public enum Level { INFO, WARN, ERROR, SNAPSHOT, NETWORK, USER }
    
    // Immutable event details captured at the time of logging.
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
