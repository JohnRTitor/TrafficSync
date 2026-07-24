package registry;

import java.util.*;

public class JsonUtil {

    public static String registrationResponse(
            NodeInfo node,
            List<Integer> neighbors,
            List<Integer> incomingNeighbors) {

        StringBuilder sb = new StringBuilder();

        sb.append("{");

        sb.append("\"status\":\"registered\",");

        sb.append("\"id\":")
                .append(node.getId())
                .append(",");

        sb.append("\"host\":\"")
                .append(node.getHost())
                .append("\",");

        sb.append("\"port\":")
                .append(node.getPort())
                .append(",");

        sb.append("\"neighbors\":[");

        for (int i = 0; i < neighbors.size(); i++) {

            sb.append(neighbors.get(i));

            if (i != neighbors.size() - 1)
                sb.append(",");

        }

        sb.append("],");
        
        sb.append("\"incoming\":[");
        for (int i = 0; i < incomingNeighbors.size(); i++) {
            sb.append(incomingNeighbors.get(i));
            if (i != incomingNeighbors.size() - 1)
                sb.append(",");
        }
        sb.append("]}");

        return sb.toString();

    }

    public static String alreadyRegisteredResponse(
            NodeInfo node,
            List<Integer> neighbors,
            List<Integer> incomingNeighbors) {

        StringBuilder sb = new StringBuilder();

        sb.append("{");

        sb.append("\"status\":\"already_registered\",");

        sb.append("\"id\":")
                .append(node.getId())
                .append(",");

        sb.append("\"host\":\"")
                .append(node.getHost())
                .append("\",");

        sb.append("\"port\":")
                .append(node.getPort())
                .append(",");

        sb.append("\"neighbors\":[");

        for (int i = 0; i < neighbors.size(); i++) {

            sb.append(neighbors.get(i));

            if (i != neighbors.size() - 1)
                sb.append(",");

        }

        sb.append("],");
        
        sb.append("\"incoming\":[");
        for (int i = 0; i < incomingNeighbors.size(); i++) {
            sb.append(incomingNeighbors.get(i));
            if (i != incomingNeighbors.size() - 1)
                sb.append(",");
        }
        sb.append("]}");

        return sb.toString();

    }

    public static String peerList(
            Collection<NodeInfo> nodes) {

        StringBuilder sb = new StringBuilder();

        sb.append("[");

        int i = 0;

        for (NodeInfo node : nodes) {

            sb.append("{");

            sb.append("\"id\":")
                    .append(node.getId())
                    .append(",");

            sb.append("\"host\":\"")
                    .append(node.getHost())
                    .append("\",");

            sb.append("\"port\":")
                    .append(node.getPort());

            sb.append("}");

            if (i != nodes.size() - 1)
                sb.append(",");

            i++;

        }

        sb.append("]");

        return sb.toString();

    }

}
