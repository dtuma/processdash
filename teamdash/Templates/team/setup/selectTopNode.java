
import pspdash.*;

public class selectTopNode extends selectHier {

    protected PropertyKey getStartingKey() {
        return PropertyKey.ROOT;
    }

    protected boolean prune(PSPProperties hierarchy, PropertyKey key) {
        String id = getID(hierarchy, key);
        if (id != null && id.length() > 0) return true;
        return false;
    }

}
