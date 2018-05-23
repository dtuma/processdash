// Copyright (C) 2002-2018 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.team.ui;

import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.function.Globsearch;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.HTMLUtils;

import org.json.simple.JSONArray;

public class SelectLabelFilter extends SelectWBSNode {


    private static final String FILTER_DATA_NAME = "Label//Filter";

    private static final String LABELS_DATA_NAME = "Task_Labels";


    protected void writeContents() throws IOException {
        PropertyKey projectRootKey = getStartingKey();
        if (projectRootKey == null) { error(); return; }
        String projectRoot = projectRootKey.path();

        String snippetDestUri = getParameter("destUri");

        out.print("<html><head><title>Choose Label Filter</title>\n");
        out.print(HTMLUtils.cssLinkHtml("/style.css"));
        out.print(HTMLUtils.cssLinkHtml("autocomplete.css"));
        out.print(HTMLUtils.scriptLinkHtml("/lib/prototype.js"));
        out.print(HTMLUtils.scriptLinkHtml("/lib/scriptaculous.js"));
        out.print("</head><body><h3>Choose Label Filter</h3>\n"
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
        out.print("' id='labelFilter'/></td></tr>\n");
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
        JSONArray labels = getDefinedLabelsArray(projectRoot);
        if (!labels.isEmpty()) {
            out.write("<div id='autocomplete' style='display:none'></div>\n");
            out.write("<script language='JavaScript'>\n");
            out.write("    var labels = "  + labels + ";\n");
            out.write("    new Autocompleter.Local('labelFilter', "
                    + "'autocomplete', labels, "
                    + "{ tokens: ' |()-'.split(''), partialChars: 1 });\n");
            out.write("</script>\n");
        }
        out.print("</body></html>\n");
    }

    private JSONArray getDefinedLabelsArray(String projectRoot) {
        String dataName = DataRepository.createDataName(projectRoot,
            LABELS_DATA_NAME);
        SimpleData labelsValue = getDataRepository().getSimpleValue(dataName);
        ListData list = ListData.asListData(labelsValue);

        JSONArray result = new JSONArray();
        try {
            result.addAll(Globsearch.getTags(list));
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        } catch (Throwable t) {
            // the Globsearch.getTags() method was added in PD 1.14.5.  In
            // earlier versions, this will throw an exception.  Gracefully
            // degrade and diable autocompletion support.
        }
        return result;
    }


    protected void doPost() throws IOException {
        rejectCrossSiteRequests(env);
        PropertyKey projectRootKey = getStartingKey();
        String projectRoot = projectRootKey.path();
        String dataName = DataRepository.createDataName(projectRoot,
                FILTER_DATA_NAME);

        parseFormData();

        String newFilter = getParameter("filter");
        if (newFilter == null || newFilter.trim().length() == 0
                || parameters.containsKey("remove"))
            // remove the label filter
            getDataRepository().putValue(dataName, null);
        else {
            // save the value of the new label filter
            getDataRepository().putValue(dataName,
                    StringData.create(newFilter.trim()));
            // add a listener to prevent the filter data element
            // from being disposed
            getDataRepository().addDataListener(dataName,
                    LABEL_FILTER_KEEPER, false);
        }
        // The changed filter may affect many other calculations, and those
        // values may take a moment or two to recalculate. In the next step when
        // we redirect to the destUri, it will be typical for that page to use
        // the results of those calculations, potentially in charts or tables.
        // If we redirect immediately, those charts/tables could be generated
        // from unrecalculated values. To avoid this, we intentionally wait for
        // the recalculations to finish before we redirect to the target page.
        getDataRepository().waitForCalculations();

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
