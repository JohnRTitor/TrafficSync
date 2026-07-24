package communication;

import common.Message;
import handler.MessageHandler;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
    private final int port;
    private final MessageHandler messageHandler;

    public Server(int port, MessageHandler messageHandler) {
        this.port = port;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {

        try {

            ServerSocket serverSocket = new ServerSocket(port);

            while (!isInterrupted()) {

                Socket socket = serverSocket.accept();

                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Message message = (Message) in.readObject();

                if (messageHandler != null) {
                    messageHandler.onMessage(message);
                } else {
                    System.out.println();
                    System.out.println("========== MESSAGE RECEIVED ==========");
                    System.out.println(message);
                    System.out.println("======================================");
                }

                in.close();
                socket.close();
            }

            serverSocket.close();

        } catch (Exception e) {

            if (!isInterrupted()) {
                System.out.println("Server Error : " + e.getMessage());
            }
        }
    }
}