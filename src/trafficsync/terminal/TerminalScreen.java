package trafficsync.terminal;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.SGR;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.Arrays;

public class TerminalScreen {

    private final String title;
    private final String menu;
    private final Map<String, String> statusFields = new ConcurrentHashMap<>();
    private final Map<String, Task> activeTasks = new LinkedHashMap<>();
    private final LinkedList<Event> logs = new LinkedList<>();
    private final int MAX_LOGS = 1000;
    private volatile boolean running = false;
    private Screen screen;
    private MultiWindowTextGUI gui;
    private Panel statusPanel;
    private Panel tasksPanel;
    private LogListBox logListBox;
    private TextBox commandInput;

    public TerminalScreen(String title, String menu) {
        this.title = title;
        this.menu = menu;
    }

    public void setStatus(String key, String value) {
        statusFields.put(key, value);
        updateStatusPanel();
    }

    public synchronized void addTask(Task task) {
        activeTasks.put(task.id, task);
        updateTasksPanel();
    }

    public synchronized void updateTask(String id, double progress, String status) {
        Task t = activeTasks.get(id);
        if (t != null) {
            t.progress = progress;
            t.status = status;
            updateTasksPanel();
        }
    }

    public synchronized void removeTask(String id) {
        activeTasks.remove(id);
        updateTasksPanel();
    }

    public void setPromptInput(String input) {
        if (commandInput != null && gui != null) {
            gui.getGUIThread().invokeLater(() -> commandInput.setText(input));
        }
    }

    public synchronized void addLog(Event event) {
        logs.add(event);
        if (logs.size() > MAX_LOGS) {
            logs.removeFirst();
        }
        updateLogPanel();
    }
    
    public synchronized void clearLogs() {
        logs.clear();
        updateLogPanel();
    }
    
    private void updateStatusPanel() {
        if (gui == null || statusPanel == null) return;
        gui.getGUIThread().invokeLater(() -> {
            statusPanel.removeAllComponents();
            for (Map.Entry<String, String> entry : statusFields.entrySet()) {
                statusPanel.addComponent(new Label(entry.getKey() + " :").setForegroundColor(TextColor.ANSI.CYAN));
                statusPanel.addComponent(new Label(entry.getValue()));
            }
        });
    }

    private synchronized void updateTasksPanel() {
        if (gui == null || tasksPanel == null) return;
        
        List<Task> currentTasks = new ArrayList<>(activeTasks.values());
        
        gui.getGUIThread().invokeLater(() -> {
            tasksPanel.removeAllComponents();
            if (currentTasks.isEmpty()) {
                tasksPanel.addComponent(new Label("Idle").setForegroundColor(TextColor.ANSI.DEFAULT));
            } else {
                for (Task t : currentTasks) {
                    tasksPanel.addComponent(new Label("[" + t.id + "] " + t.title));
                    if (t.progress >= 0) {
                        int totalBars = 20;
                        int filled = (int) (t.progress * totalBars);
                        StringBuilder bar = new StringBuilder("  [");
                        for (int i = 0; i < totalBars; i++) {
                            bar.append(i < filled ? "=" : (i == filled ? ">" : "."));
                        }
                        bar.append("] ").append(String.format("%d%%", (int)(t.progress * 100)));
                        tasksPanel.addComponent(new Label(bar.toString()).setForegroundColor(TextColor.ANSI.YELLOW));
                    }
                    tasksPanel.addComponent(new Label("  " + t.status));
                }
            }
        });
    }

    private synchronized void updateLogPanel() {
        if (gui == null || logListBox == null) return;
        
        List<Event> currentLogs = new ArrayList<>(logs);
        gui.getGUIThread().invokeLater(() -> {
            logListBox.clearItems();
            for (Event e : currentLogs) {
                logListBox.addItem(e);
            }
            
            // Auto-scroll to bottom
            if (logListBox.getItemCount() > 0) {
                logListBox.setSelectedIndex(logListBox.getItemCount() - 1);
            }
        });
    }

    public void start(Consumer<String> onCommand) {
        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            try {
                factory.setPreferTerminalEmulator(false);
                factory.setForceTextTerminal(true);
                screen = factory.createScreen();
            } catch (IOException e) {
                // Fallback for Windows environments (like IDE consoles or mintty) that lack stty
                factory = new DefaultTerminalFactory();
                factory.setForceTextTerminal(false);
                factory.setPreferTerminalEmulator(true);
                screen = factory.createScreen(); // Force Swing terminal
            }
            screen.startScreen();
            running = true;

            Panel mainPanel = new Panel(new LinearLayout(Direction.VERTICAL));
            
            statusPanel = new Panel(new GridLayout(2));
            mainPanel.addComponent(statusPanel.withBorder(Borders.singleLine(title)));
            updateStatusPanel();

            Panel menuPanel = new Panel(new LinearLayout(Direction.VERTICAL));
            menuPanel.addComponent(new Label(menu).setForegroundColor(TextColor.ANSI.WHITE));
            mainPanel.addComponent(menuPanel.withBorder(Borders.singleLine("Commands")));

            tasksPanel = new Panel(new LinearLayout(Direction.VERTICAL));
            mainPanel.addComponent(tasksPanel.withBorder(Borders.singleLine("Active Tasks")));
            updateTasksPanel();

            logListBox = new LogListBox(new TerminalSize(80, 15));
            mainPanel.addComponent(logListBox.withBorder(Borders.singleLine("Live Event Log")));

            commandInput = new TextBox(new TerminalSize(50, 1)) {
                @Override
                public Result handleKeyStroke(KeyStroke keyStroke) {
                    if (keyStroke.getKeyType() == KeyType.Enter) {
                        String cmd = getText().trim();
                        if (!cmd.isEmpty()) {
                            setText("");
                            onCommand.accept(cmd);
                        }
                        return Result.HANDLED;
                    }
                    return super.handleKeyStroke(keyStroke);
                }
            };
            
            Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
            inputPanel.addComponent(new Label("> ").setForegroundColor(TextColor.ANSI.CYAN));
            inputPanel.addComponent(commandInput);
            mainPanel.addComponent(inputPanel.withBorder(Borders.singleLine("Input")));

            BasicWindow window = new BasicWindow();
            window.setTheme(new SimpleTheme(TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT));
            window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
            window.setComponent(mainPanel);

            gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.DEFAULT));
            
            Thread eventThread = new Thread(() -> {
                while (running) {
                    Event e = EventQueue.poll();
                    if (e != null) {
                        addLog(e);
                    } else {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            eventThread.setDaemon(true);
            eventThread.start();

            gui.addWindowAndWait(window);
            
            running = false;
            eventThread.interrupt();
            screen.stopScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (gui != null && gui.getActiveWindow() != null) {
            gui.getActiveWindow().close();
        }
    }
}
