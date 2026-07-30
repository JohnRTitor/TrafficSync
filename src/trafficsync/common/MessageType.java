package trafficsync.common;

public enum MessageType {
    // Registration and peer-discovery messages.
    REGISTER,
    REGISTER_ACK,
    // Traffic simulation and distributed-snapshot messages.
    TRAFFIC_UPDATE,
    MARKER,
    SNAPSHOT_RESPONSE,
    PEER_LIST,
    // User commands and node-identity lookup messages.
    MANUAL_MESSAGE,
    SNAPSHOT_TRIGGER,
    START_SNAPSHOT,
    LOCAL_SNAPSHOT_DONE,
    QUERY_NODE_ID,
    QUERY_NODE_ID_RESPONSE
}
