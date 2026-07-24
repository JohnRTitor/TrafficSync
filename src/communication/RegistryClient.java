package communication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RegistryClient {

    private static final String SERVER = "http://localhost:8080";

    private int nodeId;
    private String host;
    private int port;

    // Neighbor ID -> Port
    private final Map<Integer, Integer> neighbors = new HashMap<>();

    public boolean register(String host, int port) {

        try {
            URL url = java.net.URI.create(SERVER + "/register").toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            connection.setRequestProperty("Content-Type", "application/json");

            String body = "{" + "\"host\":\"" + host + "\"," + "\"port\":" + port + "}";

            OutputStream os = connection.getOutputStream();

            os.write(body.getBytes());
            os.flush();
            os.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            parseRegistration(response.toString());
            loadPeers();
            return true;

        } catch (Exception e) {
            System.out.println("Registry Error : " + e.getMessage());
            return false;
        }
    }

    private void parseRegistration(String json) {

        nodeId = Integer.parseInt(extract(json, "\"id\":", ","));
        host = extract(json, "\"host\":\"", "\"");
        port = Integer.parseInt(extract(json, "\"port\":", ","));

        String list = json.substring(json.indexOf("[") + 1, json.indexOf("]"));

        neighbors.clear();
        if (!list.isBlank()) {
            for (String value : list.split(",")) {
                neighbors.put(Integer.parseInt(value.trim()), 0);
            }
        }

        System.out.println();
        System.out.println("=================================");
        System.out.println(" Registration Successful");
        System.out.println("=================================");
        System.out.println("Node ID : " + nodeId);
        System.out.println("Host    : " + host);
        System.out.println("Port    : " + port);
    }

    public void loadPeers() {
        try {
            URL url = java.net.URI.create(SERVER + "/peers").toURL();

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

            for (Integer id : neighbors.keySet()) {
                String token = "\"id\":" + id;
                int index = json.indexOf(token);
                if (index == -1) continue;

                int portIndex = json.indexOf("\"port\":", index);
                int end = json.indexOf("}", portIndex);
                int peerPort = Integer.parseInt(json.substring(portIndex + 7, end).trim());

                neighbors.put(id, peerPort);
            }

        } catch (Exception e) {
            System.out.println(
                    "Unable to load peers.");
        }
    }

    private String extract(String text, String begin, String end) {

        int start = text.indexOf(begin) + begin.length();
        int finish = text.indexOf(end, start);

        return text.substring(start, finish);
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<Integer, Integer> getNeighbors() {
        return neighbors;
    }
}