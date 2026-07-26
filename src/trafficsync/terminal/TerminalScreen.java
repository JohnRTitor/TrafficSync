package trafficsync.terminal;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerminalScreen {
    private final String title;
    private final Map<String, String> statusFields = new ConcurrentHashMap<>();
    private final LinkedList<Event> logs = new LinkedList<>();
    private final int MAX_LOGS = 15;
    
    private volatile String currentTask = "Idle";
    private volatile double taskProgress = -1; // -1 means no progress bar
    private volatile String taskStatus = "";
    private volatile String promptInput = "";
    
    private final String menu;

    public TerminalScreen(String title, String menu) {
        this.title = title;
        this.menu = menu;
    }

    public void setStatus(String key, String value) {
        statusFields.put(key, value);
    }

    public void setTask(String task, double progress, String status) {
        this.currentTask = task;
        this.taskProgress = progress;
        this.taskStatus = status;
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
        sb.append(Ansi.CYAN).append("=====================================================================\n");
        sb.append(" ").append(title).append("\n");
        sb.append("=====================================================================\n").append(Ansi.RESET);
        
        // Status
        for (Map.Entry<String, String> entry : statusFields.entrySet()) {
            sb.append(Ansi.BOLD).append(String.format("%-15s", entry.getKey() + " : ")).append(Ansi.RESET)
              .append(entry.getValue()).append("\n");
        }
        
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------\n").append(Ansi.RESET);
        sb.append(menu).append("\n");
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------\n").append(Ansi.RESET);
        
        // Current Task
        sb.append("Status:\n");
        sb.append(currentTask).append("\n");
        if (taskProgress >= 0) {
            int totalBars = 20;
            int filled = (int) (taskProgress * totalBars);
            sb.append("[");
            for (int i = 0; i < totalBars; i++) {
                sb.append(i < filled ? "=" : (i == filled ? ">" : "."));
            }
            sb.append("] ").append(String.format("%d%%", (int)(taskProgress * 100))).append("\n");
            sb.append(taskStatus).append("\n");
        } else {
            sb.append("\n\n");
        }
        
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------\n").append(Ansi.RESET);
        sb.append("Live Event Log\n");
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------\n").append(Ansi.RESET);
        
        // Logs
        for (Event e : logs) {
            String color = Ansi.RESET;
            switch (e.getLevel()) {
                case ERROR: color = Ansi.RED; break;
                case WARN: color = Ansi.YELLOW; break;
                case SNAPSHOT: color = Ansi.BLUE; break;
                case NETWORK: color = Ansi.GREEN; break;
                case USER: color = Ansi.CYAN; break;
                default: break;
            }
            // Format time
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
            String time = sdf.format(new java.util.Date(e.getTimestamp()));
            sb.append(color).append(time).append(" [").append(e.getLevel()).append("] ")
              .append(e.getMessage()).append(Ansi.RESET).append("\n");
        }
        
        // Fill empty lines to keep height constant
        for (int i = logs.size(); i < MAX_LOGS; i++) {
            sb.append("\n");
        }
        
        sb.append(Ansi.CYAN).append("---------------------------------------------------------------------\n").append(Ansi.RESET);
        sb.append("> ").append(promptInput).append(Ansi.CLEAR_LINE); // Clear rest of line to avoid artifacting

        return sb.toString();
    }
}
