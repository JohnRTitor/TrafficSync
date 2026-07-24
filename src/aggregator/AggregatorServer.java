package aggregator;

import communication.Server;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Scanner;

public class AggregatorServer {

    private static final String REGISTRY_URL = "http://localhost:8080";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==================================");
        System.out.println(" Global State Aggregator Server");
        System.out.println("==================================");
        
        System.out.print("Enter Listening Port (e.g. 9000): ");
        int port = scanner.nextInt();
        
        int totalNodes = fetchTotalNodes();
        if (totalNodes <= 0) {
            System.err.println("Failed to get total nodes from Registry or no nodes exist.");
            return;
        }

        if (!registerAggregator("localhost", port)) {
            System.err.println("Failed to register aggregator with Registry.");
            return;
        }

        SnapshotCollector collector = new SnapshotCollector(totalNodes);
        SnapshotReceiver receiver = new SnapshotReceiver(collector);
        
        Server server = new Server(port, receiver);
        server.start();

        System.out.println("\nAggregator Server is listening on port " + port);
        System.out.println("Press Ctrl+C to exit.");
    }

    private static int fetchTotalNodes() {
        try {
            URL url = java.net.URI.create(REGISTRY_URL + "/topology").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            String json = response.toString();
            int index = json.indexOf("\"totalNodes\":");
            if (index != -1) {
                int start = index + 13;
                int end = json.indexOf("}", start);
                return Integer.parseInt(json.substring(start, end).trim());
            }
        } catch (Exception e) {
            System.err.println("Error fetching topology: " + e.getMessage());
        }
        return 0;
    }

    private static boolean registerAggregator(String host, int port) {
        try {
            URL url = java.net.URI.create(REGISTRY_URL + "/aggregator").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String body = "{\"host\":\"" + host + "\",\"port\":" + port + "}";
            OutputStream os = connection.getOutputStream();
            os.write(body.getBytes());
            os.flush();
            os.close();

            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            System.err.println("Error registering aggregator: " + e.getMessage());
            return false;
        }
    }
}
