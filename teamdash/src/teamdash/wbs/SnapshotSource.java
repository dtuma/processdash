
package teamdash.wbs;

public interface SnapshotSource {

    public Object getSnapshot();
    public void restoreSnapshot(Object snapshot);

}
