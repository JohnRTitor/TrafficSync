package trafficsync.terminal;

// This class represents an ongoing process, like a global snapshot waiting for nodes to reply.
// It holds the current progress so the terminal can draw a progress bar. The VpsServer creates
// a Task when a snapshot begins and removes it when all responses arrive.
public class Task {
    // The id and title are final because they never change once a task is created.
    // Progress and status are intentionally mutable (not final) so that the VpsServer or
    // TrafficNode can update them as the snapshot progresses without creating a new object.
    public final String id;
    public final String title;
    public double progress;
    public String status;
    // This callback is invoked if the task needs to be cancelled. For snapshot tasks,
    // it cleans up the snapshot state and processes any queued snapshot triggers.
    public final Runnable onCancel;

    public Task(String id, String title, double progress, String status, Runnable onCancel) {
        this.id = id;
        this.title = title;
        this.progress = progress;
        this.status = status;
        this.onCancel = onCancel;
    }
}
