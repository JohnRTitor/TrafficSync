package registry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Topology {

    private int totalNodes;

    private final Map<Integer, List<Integer>> adjacencyList;

    public Topology(String fileName) {

        adjacencyList = new HashMap<>();

        loadTopology(fileName);

    }

    private void loadTopology(String fileName) {

        try {

            BufferedReader br = new BufferedReader(new FileReader(fileName));

            String line = br.readLine();

            totalNodes = Integer.parseInt(line.trim());

            for(int i=1;i<=totalNodes;i++) {

                adjacencyList.put(i,new ArrayList<>());

            }

            while((line=br.readLine())!=null){

                line=line.trim();

                if(line.isEmpty())
                    continue;

                String[] parts=line.split("\\s+");

                int from=Integer.parseInt(parts[0]);

                int to=Integer.parseInt(parts[1]);

                adjacencyList.get(from).add(to);

            }

            br.close();

        }

        catch(IOException e){

            System.out.println("Unable to load topology.");

            e.printStackTrace();

        }

    }

    public List<Integer> getNeighbors(int nodeId){

        return adjacencyList.getOrDefault(nodeId,new ArrayList<>());

    }

    public List<Integer> getIncomingNeighbors(int nodeId){
        List<Integer> incoming = new ArrayList<>();
        for(Map.Entry<Integer, List<Integer>> entry : adjacencyList.entrySet()){
            if(entry.getValue().contains(nodeId)){
                incoming.add(entry.getKey());
            }
        }
        return incoming;
    }

    public int getTotalNodes(){

        return totalNodes;

    }

    public void printTopology(){

        System.out.println("\n----- Network Topology -----");

        for(int i=1;i<=totalNodes;i++){

            System.out.println(i+" -> "+adjacencyList.get(i));

        }

        System.out.println("----------------------------");

    }

}

