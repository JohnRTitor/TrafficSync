package registry;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.io.OutputStream;

public class RegistryServer {

    private static RegistryManager manager;

    public static void main(String[] args) throws Exception {

        int port = 8080;

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Topology topology = new Topology("topology.txt");

        topology.printTopology();

        manager = new RegistryManager(topology);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/register", RegistryServer::registerNode);
        server.createContext("/peers", RegistryServer::getPeers);
        server.createContext("/topology", RegistryServer::getTopology);
        server.createContext("/aggregator", RegistryServer::handleAggregator);
        server.createContext("/ping", RegistryServer::pingNode);
        server.createContext("/leave", RegistryServer::leaveNode);

        server.setExecutor(null);

        server.start();

        System.out.println();
        System.out.println("==================================");
        System.out.println(" Registry Server Started");
        System.out.println(" Port : " + port);
        System.out.println("==================================");
    }

    private static void registerNode(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()));

        StringBuilder body = new StringBuilder();

        String line;

        while ((line = br.readLine()) != null) {
            body.append(line);
        }

        String request = body.toString();

        String host = "";
        int port = 0;

        int hostIndex = request.indexOf("\"host\"");

        if (hostIndex != -1) {

            int first = request.indexOf("\"", hostIndex + 6);
            int second = request.indexOf("\"", first + 1);

            host = request.substring(first + 1, second);

        }

        int portIndex = request.indexOf("\"port\"");

        if (portIndex != -1) {

            int colon = request.indexOf(":", portIndex);

            int comma = request.indexOf(",", colon);

            if (comma == -1)
                comma = request.indexOf("}", colon);

            port = Integer.parseInt(
                    request.substring(colon + 1, comma).trim());

        }

        boolean alreadyRegistered =
                manager.isRegistered(host, port);

        NodeInfo node =
                manager.registerNode(host, port);

        if (node == null) {

            String response =
                    "{\"error\":\"Maximum number of nodes reached\"}";

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json");

            exchange.sendResponseHeaders(
                    400,
                    response.length());

            exchange.getResponseBody().write(response.getBytes());

            exchange.close();

            return;
        }

        String response;

        if (alreadyRegistered) {

            System.out.println(
                    "Duplicate registration ignored for Node "
                            + node.getId());

            response =
                    JsonUtil.alreadyRegisteredResponse(
                            node,
                            manager.getNeighbors(node.getId()),
                            manager.getIncomingNeighbors(node.getId()));

        } else {

            System.out.println(
                    "Node "
                            + node.getId()
                            + " registered.");

            response =
                    JsonUtil.registrationResponse(
                            node,
                            manager.getNeighbors(node.getId()),
                            manager.getIncomingNeighbors(node.getId()));
        }

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json");

        exchange.sendResponseHeaders(
                200,
                response.length());

        OutputStream os = exchange.getResponseBody();

        os.write(response.getBytes());

        os.close();

        manager.printRegisteredNodes();
    }

    private static void getPeers(HttpExchange exchange)
            throws IOException {

        String response =
                JsonUtil.peerList(
                        manager.getRegisteredNodes());

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json");

        exchange.sendResponseHeaders(
                200,
                response.length());

        OutputStream os =
                exchange.getResponseBody();

        os.write(response.getBytes());

        os.close();
    }
    
    private static void getTopology(HttpExchange exchange) throws IOException {
        String response = "{\"totalNodes\":" + manager.getTotalNodes() + "}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static void handleAggregator(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                body.append(line);
            }
            String request = body.toString();
            String host = "";
            int port = 0;

            int hostIndex = request.indexOf("\"host\"");
            if (hostIndex != -1) {
                int first = request.indexOf("\"", hostIndex + 6);
                int second = request.indexOf("\"", first + 1);
                host = request.substring(first + 1, second);
            }

            int portIndex = request.indexOf("\"port\"");
            if (portIndex != -1) {
                int colon = request.indexOf(":", portIndex);
                int comma = request.indexOf(",", colon);
                if (comma == -1) comma = request.indexOf("}", colon);
                port = Integer.parseInt(request.substring(colon + 1, comma).trim());
            }

            manager.setAggregatorInfo(host, port);
            System.out.println("Aggregator registered at " + host + ":" + port);

            String response = "{\"status\":\"ok\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            String host = manager.getAggregatorHost();
            int port = manager.getAggregatorPort();
            String response = "{\"host\":\"" + host + "\",\"port\":" + port + "}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private static void pingNode(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("nodeId=")) {
            String nodeIdStr = query.substring(query.indexOf("nodeId=") + 7);
            System.out.println("Node " + nodeIdStr + " pinged the server.");
        } else {
            System.out.println("Unknown node pinged the server.");
        }
        
        String response = "{\"status\":\"ok\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    private static void leaveNode(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("nodeId=")) {
            int nodeId = Integer.parseInt(query.substring(query.indexOf("nodeId=") + 7));
            manager.removeNode(nodeId);
            System.out.println("Node " + nodeId + " left the network.");
        }
        
        String response = "{\"status\":\"ok\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        
        manager.printRegisteredNodes();
    }
}
