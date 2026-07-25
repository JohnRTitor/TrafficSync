package app;

import node.RegionNode;
import node.TrafficControllerProcess;

import java.util.Scanner;

public class NodeRunner {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("==============================");
        System.out.println(" Distributed Snapshot Region");
        System.out.println("==============================");

        System.out.print("Enter Listening Port for Region (e.g. 5001): ");
        int port = scanner.nextInt();

        if (port < 1024 || port > 65535) {
            System.out.println("Invalid Port Number.");
            scanner.close();
            return;
        }

        try {
            RegionNode regionNode = new RegionNode(port);
            regionNode.startRegion("local_topology.txt");

            System.out.println("\nRegion " + regionNode.getRegionId() + " is running.");

            while (true) {
                System.out.println("\nSelect an action:");
                System.out.println("1. Start Traffic Simulator on all controllers");
                System.out.println("2. Stop Traffic Simulator on all controllers");
                System.out.println("3. Trigger Diffusing Computation (Leader Election)");
                System.out.println("4. Exit");
                System.out.print("Choice: ");

                int choice = scanner.nextInt();

                if (choice == 1) {
                    for (TrafficControllerProcess process : regionNode.getLocalProcesses().values()) {
                        process.startTraffic();
                    }
                } else if (choice == 2) {
                    for (TrafficControllerProcess process : regionNode.getLocalProcesses().values()) {
                        process.stopTraffic();
                    }
                } else if (choice == 3) {
                    for (TrafficControllerProcess process : regionNode.getLocalProcesses().values()) {
                        process.startDiffusingComputation();
                    }
                } else if (choice == 4) {
                    regionNode.stopRegion();
                    System.out.println("Exiting...");
                    break;
                } else {
                    System.out.println("Invalid Choice.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error initializing RegionNode: " + e.getMessage());
            e.printStackTrace();
        }

        scanner.close();
    }
}
