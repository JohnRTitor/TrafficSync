# TrafficSync: Distributed Snapshot System

TrafficSync is a distributed Java application that simulates network traffic among independent nodes and implements the **Chandy-Lamport Distributed Snapshot Algorithm** over real network sockets. The project is designed with a decentralized architecture where nodes communicate asynchronously, and a central bootstrap registry server helps nodes discover their peers.

## Project Architecture

The codebase is divided into several modules (packages) with a clear separation of concerns.

### 1. `registry` (Bootstrap/VPS Server)
- **Role:** Acts as the network's central directory service.
- **Functionality:** 
  - Loads the network topology from a `topology.txt` file.
  - Exposes an HTTP server (default port `8080`) with endpoints for nodes to `/register`, `/leave`, and `/ping`.
  - Distributes the peer map (neighbors) to nodes via the `/peers` endpoint, along with incoming edge definitions.
- **Key Classes:** `RegistryServer`, `RegistryManager`, `Topology`.

### 2. `communication` (Site Socket Layer)
- **Role:** Handles physical transmission of messages over TCP sockets.
- **Functionality:**
  - Implements the `Communication` interface that abstract algorithms depend on.
  - Dynamically resolves neighbor IP addresses and ports using the `RegistryClient`.
  - Spawns a background `Server` thread to accept incoming connections and pushes parsed payloads to the `MessageHandler`.
- **Key Classes:** `SocketCommunication`, `Server`, `Client`, `RegistryClient`.

### 3. `node` and `handler` (Local Orchestration)
- **Role:** Glues together the network and algorithm engines for a single machine.
- **Functionality:**
  - `Node` acts as the primary facade, initializing connections and registering with the central server.
  - `MessageHandler` acts as a router, examining the `MessageType` of incoming network payloads and forwarding them to the appropriate algorithm engine (e.g., passing a `MARKER` message to the `SnapshotEngine`).
- **Key Classes:** `Node`, `MessageHandler`.

### 4. `snapshot` (Chandy-Lamport Global Snapshot)
- **Role:** Records a consistent global state of the distributed system.
- **Functionality:**
  - When triggered, it records the node's local state and sends `MARKER` messages to all outgoing channels.
  - Listens for incoming `MARKER` messages from neighbors to begin recording incoming channel states.
  - Combines the local state and the state of the channels into a final `SnapshotReport`.
- **Key Classes:** `SnapshotEngine`, `LocalState`, `ChannelState`.

### 5. `diffusing` (Termination Detection)
- **Role:** Implements Dijkstra-Scholten's diffusing computation algorithm.
- **Functionality:** 
  - Uses `EXPLORE` and `ECHO` messages to build a spanning tree and determine when a distributed computation has fully completed across all nodes.
- **Key Classes:** `DiffusingEngine`.

### 6. `traffic` (Traffic Simulation)
- **Role:** Simulates the background noise/workload of the network.
- **Functionality:**
  - Spawns a thread that randomly generates and sends `TRAFFIC` (Application) messages to neighboring nodes to simulate a busy network while the snapshot is running.
- **Key Classes:** `TrafficSimulator`.

---

## How to Run the Project

### Prerequisites
- JDK 11 or higher installed on your system.

### 1. Compile the Project
Open a terminal in the root directory and compile the source code:
```bash
mkdir -p out
javac -d out $(find src -name "*.java")
```

### 2. Start the Registry Server
The registry server is required to distribute the network topology before any nodes can communicate. Ensure there is a valid `topology.txt` file in your working directory.
```bash
java -cp out registry.RegistryServer 8080
```

### 3. Start the Nodes
Open a new terminal window for each node you want to run (e.g., if your topology expects 3 nodes).
```bash
java -cp out app.NodeRunner
```
The interactive CLI will ask for a listening port (e.g., `5001`, `5002`, `5003`).

### 4. Running Algorithms
Once multiple nodes are running and registered, you can use the interactive `NodeRunner` CLI on any node to:
- **1. Start Traffic Simulator:** Generate randomized application messages across the network.
- **2. Stop Traffic Simulator:** Halt the background traffic thread.
- **3. Start Diffusing Computation:** Begin the Dijkstra-Scholten termination detection algorithm.
- **4. Start Snapshot:** Trigger the Chandy-Lamport algorithm to record the global state.
- **5. Print Local State:** View the locally recorded snapshot variables and channel buffers for that specific node.
- **6. Exit:** Cleanly unregister from the Registry Server and shut down the node.
- **7. Send Manual Message:** Manually input a destination Node ID and a custom text payload to send an `APPLICATION` message.
- **8. Refresh Peers:** Query the Registry Server to refresh dynamic connections, particularly useful if neighbors boot up after you.
- **9. Ping Server:** Send a health-check ping to the Registry Server.
