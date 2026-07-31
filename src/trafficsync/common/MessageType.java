package trafficsync.common;

// This enum lists all the different kinds of messages our system understands.
// Using an enum is safer than using raw strings because the compiler will catch
// any typo at compile time. Each constant maps directly to a stage in one of
// the system's workflows: registration, traffic exchange, or snapshot collection.
public enum MessageType {
    // These messages are used when a node first connects and needs to learn about other nodes.
    REGISTER,
    REGISTER_ACK,
    // PEER_LIST is sent by the VPS to every node whenever a node joins or leaves,
    // so each node always has an up-to-date list of who else is in the network.
    PEER_LIST,
    // These messages are for the normal traffic updates exchanged between controller threads.
    TRAFFIC_UPDATE,
    // MARKER is the core Chandy-Lamport message that tells a receiver to record its state.
    MARKER,
    // SNAPSHOT_TRIGGER is sent from the VPS to a node to initiate a snapshot round.
    // START_SNAPSHOT is used internally within a node to tell the designated initiator
    // controller thread to begin the Chandy-Lamport algorithm.
    SNAPSHOT_TRIGGER,
    START_SNAPSHOT,
    // LOCAL_SNAPSHOT_DONE is sent by each controller thread back to the TrafficNode
    // once it has finished recording its local state and channel messages.
    LOCAL_SNAPSHOT_DONE,
    // SNAPSHOT_RESPONSE is the final aggregated state that a node sends back to the VPS.
    SNAPSHOT_RESPONSE,
    // These are triggered when a user types a command into the terminal interface.
    MANUAL_MESSAGE,
    QUERY_NODE_ID,
    QUERY_NODE_ID_RESPONSE
}
