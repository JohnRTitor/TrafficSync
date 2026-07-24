package common;

/**
 * Different types of messages exchanged between nodes.
 */
public enum MessageType {

    // Used for Diffusing Computation
    EXPLORE,
    ECHO,

    // Used for normal application traffic
    TRAFFIC,
    APPLICATION,

    // Used for Chandy-Lamport Snapshot
    MARKER,
    SNAPSHOT_REPORT,
    SNAPSHOT
}