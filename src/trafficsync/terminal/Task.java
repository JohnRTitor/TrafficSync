package trafficsync.terminal;

public class Task {
    // Mutable task state displayed in the terminal's active-task panel.
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
