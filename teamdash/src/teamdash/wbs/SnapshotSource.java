
package teamdash.wbs;

/** Defines the semantics for an object which can generate and restore
 * snapshots of its state.
 */
public interface SnapshotSource {

    public Object getSnapshot();
    public void restoreSnapshot(Object snapshot);

}
