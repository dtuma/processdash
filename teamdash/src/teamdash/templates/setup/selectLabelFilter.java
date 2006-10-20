package teamdash.templates.setup;

import java.io.IOException;
import java.util.Vector;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.HTMLUtils;

public class selectLabelFilter extends selectWBS {


    private static final String FILTER_DATA_NAME = "Label//Filter";

    private static final String LABELS_DATA_NAME = "Task_Labels";


    protected void writeContents() throws IOException {
        PropertyKey projectRootKey = getStartingKey();
        if (projectRootKey == null) { error(); return; }
        String projectRoot = projectRootKey.path();

        String snippetDestUri = getParameter("destUri");

        out.print("<html><head><title>Choose Label Filter</title>\n"
                + "<link rel=stylesheet type='text/css' href='/style.css'>\n"
                + "</head><body><h3>Choose Label Filter</h3>\n"
                + "<p>You can filter the view of project metrics, defects, and "
                + "earned value by selecting a label filter.  Only items from "
                + "the work breakdown structure whose labels match the filter "
                + "will be included.</p>");

        out.print("<form action='selectLabelFilter' method='POST'");
        if (snippetDestUri == null)
            out.print(" target='topFrame'>\n");
        else {
            out.print(">\n<input type='hidden' name='destUri' value=\"");
            out.print(HTMLUtils.escapeEntities(snippetDestUri));
            out.print("\">\n");
        }
        out.print("<table><tr>"
                + "<td><b>Label Filter:</b>&nbsp;</td>"
                + "<td><input type='text' name='filter' size='80' value='");
        String currentFilter = getCurrentFilter(getDataRepository(),
                projectRoot);
        if (currentFilter != null)
            out.print(HTMLUtils.escapeEntities(currentFilter));
        out.print("'/></td></tr>\n");
        out.print("<tr><td></td><td>"
                + "<input type='submit' name='apply' value='Apply Filter'/> "
                + "<input type='submit' name='remove' value='Remove Filter'/>"
                + "</td></tr></table></form>\n");

        out.print("<p>&nbsp;</p>");

        out.print("<p>Filters can describe complex search criteria. Here are "
                + "examples of several valid searches:<table border cellpadding=4 style='margin-left: 1cm'>"
                + "<tr><td>xyz</td><td>Choose all tasks that have the label <b>xyz</b></td></tr>\n"
                + "<tr><td>xyz*</td><td>Choose all tasks that have a label starting with the letters <b>xyz</b></td></tr>\n"
                + "<tr><td>xyz abc</td><td>Choose all tasks that have label <b>xyz</b> and label <b>abc</b></td></tr>\n"
                + "<tr><td>xyz | abc</td><td>Choose all tasks that have label <b>xyz</b> or label <b>abc</b></td></tr>\n"
                + "<tr><td>xyz -abc</td><td>Choose all tasks that have label <b>xyz</b> but not label <b>abc</b></td></tr>\n"
                + "<tr><td>(xyz -abc) | efg</td><td>Choose all tasks that have label <b>xyz</b> " +
                                "and not label <b>abc</b>, or that have label <b>efg</b></td></tr>\n"
                + "</table>");

        out.print("<p>&nbsp;</p>");

        out.print("<p><i>Labels are attached to work breakdown structure "
                + "items using the Work Breakdown Structure editor, and are "
                + "copied to this project when you synchronize it to the WBS. "
                + "To perform these operations, see the Team Project Tools "
                + "and Settings page.</i></p>\n");
        out.print("</body></html>\n");
    }


    protected void doPost() throws IOException {
        PropertyKey projectRootKey = getStartingKey();
        String projectRoot = projectRootKey.path();
        String dataName = DataRepository.createDataName(projectRoot,
                FILTER_DATA_NAME);

        parseFormData();

        String newFilter = getParameter("filter");
        if (newFilter == null || newFilter.trim().length() == 0
                || parameters.containsKey("remove"))
            getDataRepository().putValue(dataName, null);
        else {
            getDataRepository().putValue(dataName,
                    StringData.create(newFilter.trim()));
            getDataRepository().addDataListener(dataName,
                    LABEL_FILTER_KEEPER, false);
        }

        String destUri = getParameter("destUri");
        if (destUri == null)
            destUri = "../summary_frame.shtm";
        out.write("Location: " + destUri + "\r\n\r\n");
    }

    /** Object to pin label filter data elements in the repository, and prevent
     * them from being disposed.
     * 
     * Label filters are stored with an anonymous data name.  This prevents
     * them from being saved to any datafile, so they won't survive a
     * shutdown/restart of the dashboard (the desired behavior).  Unfortunately,
     * that also means that if an equation references one of these elements,
     * and is then disposed, the label would get disposed too.
     * 
     * This do-nothing DataListener is used to register "interest" in the
     * element, to prevent it from being discarded.
     */
    private static DataListener LABEL_FILTER_KEEPER = new DataListener() {
        public void dataValueChanged(DataEvent e) {}
        public void dataValuesChanged(Vector v) {}
    };


    /** If a filter is in effect for the given project, return its text.  If a
     * filter is not in effect, but one would make sense, return the empty
     * string.  Otherwise, return null.
     */
    public static String getCurrentFilter(DataRepository data,
            String projectRoot) {
        // check to see if a filter is in effect.  If so, return its text.
        String dataName = DataRepository.createDataName(projectRoot,
                FILTER_DATA_NAME);
        SimpleData filterValue = data.getSimpleValue(dataName);
        if (filterValue != null && filterValue.test())
            return filterValue.format();

        // No filter is in effect.  Does a filter even make sense?  Check to
        // see if any labels are defined for this project.
        dataName = DataRepository.createDataName(projectRoot,
                LABELS_DATA_NAME);
        SimpleData labelsValue = data.getSimpleValue(dataName);
        if (labelsValue != null && labelsValue.test())
            return "";
        else
            return null;
    }
}
