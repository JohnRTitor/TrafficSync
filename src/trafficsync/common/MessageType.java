package trafficsync.common;

// This enum lists all the different kinds of messages our system understands.
// Using an enum is safer than using raw strings because it prevents spelling mistakes.
public enum MessageType {
    // These messages are used when a node first connects and needs to learn about other nodes.
    REGISTER,
    REGISTER_ACK,
    // These messages are for the normal traffic updates and the Chandy-Lamport snapshot algorithm.
    TRAFFIC_UPDATE,
    MARKER,
    SNAPSHOT_RESPONSE,
    PEER_LIST,
    // These are triggered when a user types a command into the terminal interface.
    MANUAL_MESSAGE,
    SNAPSHOT_TRIGGER,
    START_SNAPSHOT,
    LOCAL_SNAPSHOT_DONE,
    QUERY_NODE_ID,
    QUERY_NODE_ID_RESPONSE
}
