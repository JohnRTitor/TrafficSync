package communication;

import common.Message;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    public void send(String host, int port, Message message) {

        try {

            Socket socket = new Socket(host, port);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(message);
            out.flush();

            System.out.println();
            System.out.println("=================================");
            System.out.println(" Message Sent Successfully");
            System.out.println("=================================");
            System.out.println("From Node : " + message.getSenderId());
            System.out.println("To Node   : " + message.getReceiverId());
            System.out.println("Type      : " + message.getType());
            System.out.println("Port      : " + port);
            System.out.println("=================================");

            out.close();
            socket.close();

        } catch (Exception e) {

            System.out.println("Client Error : " + e.getMessage());
        }
    }
}