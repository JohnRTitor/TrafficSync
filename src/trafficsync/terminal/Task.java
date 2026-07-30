package trafficsync.terminal;

// This class represents an ongoing process, like a global snapshot waiting for nodes to reply.
// It holds the current progress so the terminal can draw a progress bar.
public class Task {
    // These variables hold the details we want to show on the screen.
    // They are public so the terminal can easily update the progress percentage.
    public final String id;
    public final String title;
    public double progress;
    public String status;
    public final Runnable onCancel;

    public Task(String id, String title, double progress, String status, Runnable onCancel) {
        this.id = id;
        this.title = title;
        this.progress = progress;
        this.status = status;
        this.onCancel = onCancel;
    }
}
