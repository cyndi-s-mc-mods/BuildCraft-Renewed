package buildcraft.lib.transfer;

/** Storage that can be rolled back, so it can take part in Fabric's and NeoForge's transfer transactions. */
public interface ISnapshotable {
    Object createSnapshot();

    void restoreSnapshot(Object snapshot);

    /** Called once a change made during a transaction is final. Usually marks the block entity as changed. */
    void onSnapshotCommit();
}
