package trafficsync.node;

import trafficsync.terminal.EventQueue;

public class ControllerThread extends Thread {
    private final String name;
    private final TrafficNode node;
    private volatile boolean running = true;

    public ControllerThread(String name, TrafficNode node) {
        this.name = name;
        this.node = node;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Random sleep between 5 to 15 seconds
                long sleepTime = 5000 + (long)(Math.random() * 10000);
                Thread.sleep(sleepTime);
                
                if (running) {
                    int cars = (int)(Math.random() * 50);
                    node.sendTrafficUpdate(name + " processed " + cars + " cars.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stopRunning() {
        running = false;
        this.interrupt();
    }
}
