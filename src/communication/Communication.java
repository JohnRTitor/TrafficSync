package communication;

import common.Message;

/**
 * Communication interface.
 * 
 * Defines standard communication methods for starting, sending, and stopping.
 */
public interface Communication {

    /**
     * Start the communication service.
     */
    void start();

    /**
     * Send a message to another node.
     *
     * @param message Message to send
     */
    void send(Message message);

    /**
     * Stop the communication service.
     */
    void stop();

}