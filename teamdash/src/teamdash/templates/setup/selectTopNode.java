
package teamdash.templates.setup;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;


public class selectTopNode extends selectHier {

    protected PropertyKey getStartingKey() {
        return PropertyKey.ROOT;
    }

    protected boolean prune(DashHierarchy hierarchy, PropertyKey key) {
        String id = getID(hierarchy, key);
        if (id != null && id.length() > 0) return true;
        return false;
    }

}
