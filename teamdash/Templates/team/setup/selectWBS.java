
import java.io.IOException;

import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;


public class selectWBS extends selectHier {

    // start at the root of the current project.
    protected PropertyKey getStartingKey() {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        while (key != null) {
            String id = getID(hierarchy, key);
            if (id != null && id.endsWith("Root")) break;
            key = key.getParent();
        }
        return key;
    }

    protected void error() throws IOException {
        out.println("<HTML><BODY>");
        out.println("This script must be used from within a team project.");
        out.println("</BODY></HTML>");
    }

    // only display team roots and team nodes.
    protected boolean prune(DashHierarchy hierarchy, PropertyKey key) {
        String id = getID(hierarchy, key);
        if (id == null || id.length() == 0) return true;
        if (!id.endsWith("Root") && !id.endsWith("Node")) return true;
        return false;
    }

}
