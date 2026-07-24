package aggregator;

import snapshot.SnapshotReport;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SnapshotCollector {

    private final int totalSites;
    private final Map<Integer, SnapshotReport> snapshots;

    public SnapshotCollector(int totalSites) {
        this.totalSites = totalSites;
        this.snapshots = new ConcurrentHashMap<>();
        System.out.println("[Aggregator] Waiting for snapshots from " + totalSites + " sites...");
    }

    public synchronized void receiveSnapshot(int siteId, String payload) {
        if (snapshots.containsKey(siteId)) {
            System.err.println("[Aggregator] Duplicate snapshot received from Site " + siteId + ". Ignoring.");
            return;
        }

        if (siteId < 1 || siteId > totalSites) {
            System.err.println("[Aggregator] Unknown Site ID: " + siteId + ". Ignoring.");
            return;
        }

        try {
            SnapshotReport report = SnapshotSerializer.deserialize(payload);
            snapshots.put(siteId, report);
            System.out.println("[Aggregator] Snapshot received from Site " + siteId);
            System.out.println("[Aggregator] " + snapshots.size() + "/" + totalSites + " snapshots collected");
            
            checkCompletion();
        } catch (Exception e) {
            System.err.println("[Aggregator] Invalid snapshot format from Site " + siteId + ": " + e.getMessage());
        }
    }
    
    private void checkCompletion() {
        if (snapshots.size() == totalSites) {
            System.out.println("[Aggregator] All snapshots received");
            System.out.println("[Aggregator] Constructing global state...");
            
            GlobalState globalState = new GlobalState(snapshots);
            globalState.print();
            
            System.out.println("[Aggregator] Snapshot completed successfully");
        }
    }
}
