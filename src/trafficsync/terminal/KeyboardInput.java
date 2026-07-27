package trafficsync.terminal;

import java.util.Scanner;
import java.util.function.Consumer;

public class KeyboardInput {
    private final TerminalScreen screen;
    private final Consumer<String> onCommand;
    private volatile boolean running = true;

    public KeyboardInput(TerminalScreen screen, Consumer<String> onCommand) {
        this.screen = screen;
        this.onCommand = onCommand;
    }

    public void start() {
        Thread inputThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (running) {
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        if (!line.isEmpty()) {
                            screen.setPromptInput(""); // clear prompt immediately
                            onCommand.accept(line);
                        }
                    }
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }
}
