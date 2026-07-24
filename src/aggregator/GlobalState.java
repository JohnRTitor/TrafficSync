package aggregator;

import snapshot.SnapshotReport;
import java.util.Map;
import java.time.LocalDateTime;

public class GlobalState {

    private final Map<Integer, SnapshotReport> snapshots;

    public GlobalState(Map<Integer, SnapshotReport> snapshots) {
        this.snapshots = snapshots;
    }

    public void print() {
        System.out.println("\n============================");
        System.out.println("        GLOBAL STATE");
        System.out.println("============================");

        for (int i = 1; i <= snapshots.size(); i++) {
            SnapshotReport report = snapshots.get(i);
            if (report != null) {
                System.out.println("\nSite " + i);
                System.out.println(report.toString().replace("========== SNAPSHOT REPORT ==========", "").replace("=====================================", "").trim());
            } else {
                System.out.println("\nSite " + i + " data missing.");
            }
        }

        System.out.println("\nTotal sites: " + snapshots.size());
        System.out.println("Snapshot Completion Time: " + LocalDateTime.now());
        System.out.println("============================");
    }
}
