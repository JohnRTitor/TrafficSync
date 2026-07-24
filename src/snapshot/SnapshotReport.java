package snapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the complete snapshot of one node.
 */
public class SnapshotReport {

    private LocalState localState;

    private List<ChannelState> channelStates;

    public SnapshotReport(LocalState localState) {

        this.localState = localState;
        this.channelStates = new ArrayList<>();
    }

    /**
     * Add a recorded channel state.
     */
    public void addChannelState(ChannelState channelState) {

        channelStates.add(channelState);
    }

    public LocalState getLocalState() {
        return localState;
    }

    public List<ChannelState> getChannelStates() {
        return channelStates;
    }

    /**
     * Print the snapshot report.
     */
    public void printReport() {

        System.out.println("\n====================================");
        System.out.println("         SNAPSHOT REPORT");
        System.out.println("====================================");

        System.out.println(localState);

        System.out.println("\nIncoming Channel States");

        if (channelStates.isEmpty()) {
            System.out.println("No channel states recorded.");
        } else {

            for (ChannelState channel : channelStates) {
                System.out.println(channel);
            }
        }

        System.out.println("====================================");
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append("\n========== SNAPSHOT REPORT ==========\n");

        builder.append(localState);

        builder.append("\nIncoming Channels:\n");

        if (channelStates.isEmpty()) {
            builder.append("None\n");
        } else {

            for (ChannelState channel : channelStates) {
                builder.append(channel);
            }
        }

        builder.append("=====================================\n");

        return builder.toString();
    }
}