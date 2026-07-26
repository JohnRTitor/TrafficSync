package trafficsync.terminal;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

public class TerminalScreen {
    
    public static class Task {
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

    private final String title;
    private final Map<String, String> statusFields = new ConcurrentHashMap<>();
    private final LinkedList<Event> logs = new LinkedList<>();
    private final int MAX_LOGS = 15;
    
    private final Map<String, Task> activeTasks = new LinkedHashMap<>();
    private volatile String promptInput = "";
    
    private final String menu;

    public TerminalScreen(String title, String menu) {
        this.title = title;
        this.menu = menu;
    }

    public void setStatus(String key, String value) {
        statusFields.put(key, value);
    }

    public synchronized void addTask(Task task) {
        activeTasks.put(task.id, task);
    }

    public synchronized void updateTask(String id, double progress, String status) {
        Task t = activeTasks.get(id);
        if (t != null) {
            t.progress = progress;
            t.status = status;
        }
    }

    public synchronized void removeTask(String id) {
        activeTasks.remove(id);
    }

    public synchronized void cancelLatestTask() {
        if (!activeTasks.isEmpty()) {
            // Get the last added task
            String lastId = null;
            for (String id : activeTasks.keySet()) {
                lastId = id;
            }
            if (lastId != null) {
                cancelTask(lastId);
            }
        } else {
            EventQueue.warn("No active tasks to cancel.");
        }
    }

    public synchronized void cancelTask(String id) {
        Task t = activeTasks.remove(id);
        if (t != null) {
            if (t.onCancel != null) {
                t.onCancel.run();
            }
            EventQueue.info("Cancelled task: " + t.title);
        } else {
            EventQueue.warn("Task not found: " + id);
        }
    }

    @Deprecated
    public void setTask(String task, double progress, String status) {
        // Keeping for backward compatibility temporarily if needed, but doing nothing.
    }
    
    public void setPromptInput(String input) {
        this.promptInput = input;
    }

    public synchronized void addLog(Event event) {
        logs.add(event);
        if (logs.size() > MAX_LOGS) {
            logs.removeFirst();
        }
    }
    
    public synchronized void clearLogs() {
        logs.clear();
    }

    public synchronized String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.CURSOR_HOME);
        
        // Header
        sb.append(Ansi.CYAN).append("=====================================================================").append(Ansi.CLEAR_LINE).append("\n");
        sb.append(" ").append(title).append(Ansi.CLEAR_LINE).append("\n");
        sb.append("=====================================================================").append(Ansi.RESET).append(Ansi.CLEAR_LINE).append("\n");
        
        // Status
        for (Map.Entry<String, String> entry : statusFields.entrySet()) {
            sb.append(Ansi.BOLD).append(String.format("%-15s", entry.getKey() + " : ")).append(Ansi.RESET)
              .append(entry.getValue()).append(Ansi.CLEAR_LINE).append("\n");
        }
        
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------").append(Ansi.CLEAR_LINE).append("\n").append(Ansi.RESET);
        sb.append(menu).append(Ansi.CLEAR_LINE).append("\n");
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------").append(Ansi.CLEAR_LINE).append("\n").append(Ansi.RESET);
        
        // Active Tasks
        sb.append("Active Tasks:").append(Ansi.CLEAR_LINE).append("\n");
        if (activeTasks.isEmpty()) {
            sb.append("Idle").append(Ansi.CLEAR_LINE).append("\n");
            sb.append(Ansi.CLEAR_LINE).append("\n");
        } else {
            for (Task t : activeTasks.values()) {
                sb.append("[").append(t.id).append("] ").append(t.title).append(Ansi.CLEAR_LINE).append("\n");
                if (t.progress >= 0) {
                    int totalBars = 20;
                    int filled = (int) (t.progress * totalBars);
                    sb.append("  [");
                    for (int i = 0; i < totalBars; i++) {
                        sb.append(i < filled ? "=" : (i == filled ? ">" : "."));
                    }
                    sb.append("] ").append(String.format("%d%%", (int)(t.progress * 100))).append(Ansi.CLEAR_LINE).append("\n");
                    sb.append("  ").append(t.status).append(Ansi.CLEAR_LINE).append("\n");
                } else {
                    sb.append("  ").append(t.status).append(Ansi.CLEAR_LINE).append("\n");
                }
            }
        }
        
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------").append(Ansi.CLEAR_LINE).append("\n").append(Ansi.RESET);
        sb.append("Live Event Log").append(Ansi.CLEAR_LINE).append("\n");
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------").append(Ansi.CLEAR_LINE).append("\n").append(Ansi.RESET);
        
        // Logs
        for (Event e : logs) {
            String color = Ansi.RESET;
            switch (e.getLevel()) {
                case ERROR: color = Ansi.RED; break;
                case WARN: color = Ansi.YELLOW; break;
                case SNAPSHOT: color = Ansi.BLUE; break;
                case NETWORK: color = Ansi.GREEN; break;
                case USER: color = Ansi.CYAN; break;
                case INFO: color = Ansi.RESET; break;
                default: break;
            }
            // Format time
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
            String time = sdf.format(new java.util.Date(e.getTimestamp()));
            
            String levelText = String.format("%-8s", "[" + e.getLevel() + "]");
            
            sb.append(color).append(time).append(" ").append(levelText).append(" ")
              .append(e.getMessage()).append(Ansi.RESET).append(Ansi.CLEAR_LINE).append("\n");
        }
        
        // Fill empty lines to keep height constant (simplified for dynamic tasks)
        // Adjust this if UI jumps too much, but dynamic task lists naturally change height.
        int requiredPadding = MAX_LOGS - logs.size();
        for (int i = 0; i < requiredPadding; i++) {
            sb.append(Ansi.CLEAR_LINE).append("\n");
        }
        
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------").append(Ansi.CLEAR_LINE).append("\n").append(Ansi.RESET);
        sb.append("> ").append(promptInput).append(Ansi.CLEAR_LINE); // Clear rest of line to avoid artifacting

        return sb.toString();
    }
}
