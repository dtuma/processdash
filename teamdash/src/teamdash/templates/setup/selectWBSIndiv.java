
package teamdash.templates.setup;
import java.io.IOException;
import java.util.Vector;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class selectWBSIndiv extends selectWBS {

    private static final String REL_PATH_PARAM = "relPath";
    private static final String DEST_URI_PARAM = "destUri";
    private static final String WBS_FILTER_DATA_NAME = "Project_WBS_ID//Filter";


    protected void doPost() throws IOException {
        parseFormData();

        String newId = getNewWBSFilter();
        String dataName = DataRepository.createDataName(getPrefix(),
                WBS_FILTER_DATA_NAME);
        // save the value of the new WBS ID filter
        getDataRepository().putValue(dataName, StringData.create(newId));
        // add a listener to prevent the filter data element
        // from being disposed
        getDataRepository().addDataListener(dataName,
                WBS_FILTER_KEEPER, false);
        // allow the new filter to take effect
        getDataRepository().waitForCalculations();

        out.print("Location: ");
        out.print(getParameter(DEST_URI_PARAM));
        out.print("\r\n\r\n");
    }

    protected String getNewWBSFilter() {
        String result = "";
        SimpleData wbsDataName = getDataContext().getSimpleValue("Project_ID");
        if (wbsDataName != null)
            result = wbsDataName.format();

        String relPath = getParameter(REL_PATH_PARAM);
        if (relPath != null && relPath.length() > 0)
            result = result + "/" + relPath;

        return result;
    }

    protected String getScript() {
        String destUri = getParameter(DEST_URI_PARAM);
        return StringUtils.findAndReplace(SCRIPT, "DESTURI",
                HTMLUtils.escapeEntities(destUri));
    }

    private static final String SCRIPT =
        "<form method='POST' action='selectWBSIndiv'>\n" +
        "<input type='hidden' name='"+REL_PATH_PARAM+"' value=''>\n" +
        "<input type='hidden' name='"+DEST_URI_PARAM+"' value='DESTURI'>\n" +
        "</form>\n" +
        "<script>\n" +
        "  function doClick(relPath) {\n" +
        "    document.forms[0].elements[0].value = relPath;\n" +
        "    document.forms[0].submit();\n" +
        "  }\n" +
        "</script>";


    /** Object to pin WBS filter data elements in the repository, and prevent
     * them from being disposed.
     * 
     * WBS filters are stored with an anonymous data name.  This prevents
     * them from being saved to any datafile, so they won't survive a
     * shutdown/restart of the dashboard (the desired behavior).  Unfortunately,
     * that also means that if an equation references one of these elements,
     * and is then disposed, the filter would get disposed too.
     * 
     * This do-nothing DataListener is used to register "interest" in the
     * element, to prevent it from being discarded.
     */
    private static DataListener WBS_FILTER_KEEPER = new DataListener() {
        public void dataValueChanged(DataEvent e) {}
        public void dataValuesChanged(Vector v) {}
    };
}
