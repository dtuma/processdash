package teamdash.templates.setup;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.HTMLUtils;

public class filterWBS extends selectWBS {

    String wbsID;

    protected void initialize(DashHierarchy properties, PropertyKey key,
            String rootId, int i, String rootPrefix) {
        super.initialize(properties, key, rootId, i, rootPrefix);

        String dataName = DataRepository.createDataName(rootPrefix,
                "Project_WBS_ID");
        SimpleData wbsIdVal = getDataRepository().getSimpleValue(dataName);
        wbsID = (wbsIdVal == null ? "NULL" : wbsIdVal.format());
    }

    protected String getScript() {
        return "";
    }

    protected void printLink(String rootPath, String relPath) {
        out.print("<input type='checkbox' class='notData' name='");
        out.print(HTMLUtils.escapeEntities(wbsID));
        if (relPath != null && relPath.length() > 0) {
            out.print("/");
            out.print(HTMLUtils.escapeEntities(relPath));
        }
        out.print("' checked onclick='updateSelected(this);' />&nbsp;");
        out.print("<a href='#' onclick='toggleSelected(this); return false;'>");
    }


}
