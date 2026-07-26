package trafficsync.terminal;

public class TerminalRenderer {
    private final TerminalScreen screen;
    private volatile boolean running = false;
    private Thread renderThread;

    public TerminalRenderer(TerminalScreen screen) {
        this.screen = screen;
    }

    public void start() {
        running = true;
        System.out.print(Ansi.ALT_SCREEN_ON);
        System.out.print(Ansi.CLEAR_SCREEN);
        System.out.print(Ansi.HIDE_CURSOR);
        
        renderThread = new Thread(() -> {
            while (running) {
                // Poll events and add to screen
                Event e;
                while ((e = EventQueue.poll()) != null) {
                    screen.addLog(e);
                }
                
                // Draw
                System.out.print(screen.render());
                System.out.flush();
                
                try {
                    Thread.sleep(100); // ~10 FPS
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        renderThread.setDaemon(true);
        renderThread.start();
    }

    public void stop() {
        running = false;
        if (renderThread != null) {
            renderThread.interrupt();
        }
        System.out.print(Ansi.SHOW_CURSOR);
        System.out.print(Ansi.ALT_SCREEN_OFF);
        System.out.println("\nExiting...");
    }
}
