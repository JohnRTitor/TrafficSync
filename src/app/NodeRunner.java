package app;

import node.Node;

import java.util.Scanner;

public class NodeRunner {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("==============================");
        System.out.println(" Distributed Snapshot Node");
        System.out.println("==============================");

        System.out.print("Enter Listening Port (e.g. 5001): ");

        int port = scanner.nextInt();

        if (port < 1024 || port > 65535) {
            System.out.println("Invalid Port Number.");
            return;
        }

        try {
            Node node = new Node(port);
            node.startCommunication();

            System.out.println("\nNode " + node.getNodeId() + " is running on port " + node.getPort());

            while (true) {
                System.out.println("\nSelect an action:");
                System.out.println("1. Start Traffic Simulator");
                System.out.println("2. Stop Traffic Simulator");
                System.out.println("3. Start Diffusing Computation");
                System.out.println("4. Start Snapshot");
                System.out.println("5. Print Local State");
                System.out.println("6. Exit");
                System.out.print("Choice: ");

                int choice = scanner.nextInt();

                if (choice == 1) {
                    node.startTraffic();
                } else if (choice == 2) {
                    node.stopTraffic();
                } else if (choice == 3) {
                    node.startDiffusingComputation();
                } else if (choice == 4) {
                    node.getSnapshotEngine().startSnapshot();
                } else if (choice == 5) {
                    System.out.println("\n==================================");
                    System.out.println("LOCAL STATE OF NODE " + node.getNodeId());
                    System.out.println("==================================");
                    System.out.println(node.getSnapshotEngine().getLocalState());
                } else if (choice == 6) {
                    node.stopCommunication();
                    System.out.println("Exiting...");
                    break;
                } else {
                    System.out.println("Invalid Choice.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error initializing Node: " + e.getMessage());
            e.printStackTrace();
        }

        scanner.close();
    }
}
