package communication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RegistryClient {

    private static final String SERVER = "http://104.214.168.255:8080";

    private int nodeId;
    private String host;
    private int port;

    // Neighbor ID -> Host
    private final Map<Integer, String> neighborHosts = new HashMap<>();

    // Neighbor ID -> Port
    private final Map<Integer, Integer> neighborPorts = new HashMap<>();

    // Incoming neighbors
    private final Set<Integer> incomingNeighbors = new HashSet<>();

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

        neighborHosts.clear();
        neighborPorts.clear();
        if (!list.isBlank()) {
            for (String value : list.split(",")) {
                int id = Integer.parseInt(value.trim());

                neighborHosts.put(id, "");
                neighborPorts.put(id, 0);
            }
        }
        
        int incomingStart = json.indexOf("\"incoming\":[");
        incomingNeighbors.clear();
        if (incomingStart != -1) {
            String incomingList = json.substring(incomingStart + 12, json.indexOf("]", incomingStart));
            if (!incomingList.isBlank()) {
                for (String value : incomingList.split(",")) {
                    incomingNeighbors.add(Integer.parseInt(value.trim()));
                }
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

            for (Integer id : neighborPorts.keySet()) {
                String token = "\"id\":" + id;
                int index = json.indexOf(token);
                if (index == -1) continue;

                int hostIndex = json.indexOf("\"host\":\"", index);

                if (hostIndex == -1) {
                    continue;
                }

                int hostStart = hostIndex + 8;
                int hostEnd = json.indexOf("\"", hostStart);

                String peerHost = json.substring(hostStart, hostEnd);

                // Read Port
                int portIndex = json.indexOf("\"port\":", index);
                int end = json.indexOf("}", portIndex);

                int peerPort =
                        Integer.parseInt(
                                json.substring(portIndex + 7, end).trim());

                neighborHosts.put(id, peerHost);
                neighborPorts.put(id, peerPort);
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
        return neighborPorts;
    }
    
    public String getAggregatorHost() {
        try {
            URL url = java.net.URI.create(SERVER + "/aggregator").toURL();
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
            return extract(json, "\"host\":\"", "\"");
        } catch (Exception e) {
            return "";
        }
    }

    public int getAggregatorPort() {
        try {
            URL url = java.net.URI.create(SERVER + "/aggregator").toURL();
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
            int portIndex = json.indexOf("\"port\":");
            int end = json.indexOf("}", portIndex);
            return Integer.parseInt(json.substring(portIndex + 7, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public Set<Integer> getIncomingNeighbors() {
        return incomingNeighbors;
    }
    
    public void pingServer() {
        try {
            URL url = java.net.URI.create(SERVER + "/ping?nodeId=" + nodeId).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e) {
            System.out.println("Ping failed: " + e.getMessage());
        }
    }
    
    public void leaveServer() {
        try {
            URL url = java.net.URI.create(SERVER + "/leave?nodeId=" + nodeId).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e) {
            System.out.println("Leave failed: " + e.getMessage());
        }
    }

    public String getHost(int nodeId) {
        return neighborHosts.getOrDefault(nodeId, "");
    }

    public int getNeighborPort(int nodeId) {
        return neighborPorts.getOrDefault(nodeId, 0);
    }
}