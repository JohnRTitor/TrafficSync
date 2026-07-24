package registry;

public class TestRegistry {

    public static void main(String[] args) {

        Topology topology = new Topology("topology.txt");

        RegistryManager manager = new RegistryManager(topology);

        manager.registerNode("192.168.1.10", 5001);
	manager.registerNode("192.168.1.11", 5002);
	manager.registerNode("192.168.1.12", 5003);

        manager.printRegisteredNodes();

        System.out.println();

        System.out.println("Neighbors of Node 1 : "
                + manager.getNeighbors(1));

        System.out.println("Neighbors of Node 2 : "
                + manager.getNeighbors(2));

        System.out.println("Neighbors of Node 3 : "
                + manager.getNeighbors(3));

    }

}
