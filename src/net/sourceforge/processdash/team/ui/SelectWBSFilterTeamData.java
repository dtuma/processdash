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
import java.util.Vector;

import org.w3c.dom.Element;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;


/**
 * This class allows the user to drill down into the component hierarchy
 * on a team rollup which is using data-based filtering (as opposed to
 * frame-based filtering).
 */
public class SelectWBSFilterTeamData extends SelectWBSNode {

    private static final String REL_PATH_PARAM = "relPath";
    private static final String WBS_ID_PARAM = "wbsID";
    private static final String DEST_URI_PARAM = "destUri";
    private static final String WBS_FILTER_DATA_NAME = "Project_WBS_ID//Relative_Path_Filter";
    private static final String EV_FILTER_DATA_NAME = "Earned_Value//Merged_Path_Filter";


    @Override
    protected void printTree(DashHierarchy hierarchy, PropertyKey key,
            String id, int depth, String rootPath) {

        // print a row for the root node
        int pos = rootPath.lastIndexOf('/');
        String rootName = rootPath.substring(pos+1);
        printTreeRowStart("wbs", 0);
        printLink("", "", rootName);
        out.println("</td></tr>");

        // print rows for subcomponents
        Element xml = getComponentInfo(rootPath);
        if (xml != null)
            printTree(1, null, xml);
    }

    private Element getComponentInfo(String rootPath) {
        SimpleData d = getDataRepository().getSimpleValue(
            DataRepository.createDataName(rootPath,
                TeamDataConstants.PROJECT_COMPONENT_INFO));
        if (d != null) {
            try {
                return XMLUtils.parse(d.format()).getDocumentElement();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void printTree(int depth, String prefix, Element parent) {
        for (Element node : XMLUtils.getChildElements(parent)) {
            String name = node.getAttribute("name");
            String id = node.getAttribute("id");
            String relPath = (prefix == null ? name : prefix + "/" + name);
            printTreeRowStart("wbs-" + id, depth);
            printLink(relPath, id, name);
            out.println("</td></tr>");

            printTree(depth + 1, relPath, node);
        }
    }

    private void printLink(String relPath, String id, String displayName) {
        out.print("<a href='javascript:doClick(\""
                + escapeJavascriptArg(relPath) + "\", \""
                + escapeJavascriptArg(id) + "\");'>");
        out.print(HTMLUtils.escapeEntities(displayName));
        out.print("</a>");
    }

    protected void doPost() throws IOException {
        rejectCrossSiteRequests(env);
        parseFormData();

        // save the new value of the WBS filter
        String newId = getNewWBSFilter();
        putValue(WBS_FILTER_DATA_NAME, newId);

        // save the new value of the EV filter
        String newPath = getNewEVPathFilter();
        putValue(EV_FILTER_DATA_NAME, newPath);

        // allow the new filters to take effect
        getDataRepository().waitForCalculations();

        out.print("Location: ");
        out.print(getParameter(DEST_URI_PARAM));
        out.print("\r\n\r\n");
    }

    private void putValue(String name, String value) {
        String dataName = DataRepository.createDataName(getPrefix(), name);
        // save the value of the new filter
        StringData val = (value == null ? null :  StringData.create(value));
        getDataRepository().putValue(dataName, val);
        // add a listener to prevent the data element from being disposed
        getDataRepository().addDataListener(dataName, WBS_FILTER_KEEPER, false);
    }

    protected String getNewWBSFilter() {
        String relPath = getParameter(REL_PATH_PARAM);
        if (StringUtils.hasValue(relPath))
            return relPath;
        else
            return null;
    }

    protected String getNewEVPathFilter() {
        String wbsId = getParameter(WBS_ID_PARAM);
        if (!StringUtils.hasValue(wbsId))
            return null;

        String projectID = "";
        SimpleData projectIDVal = getDataContext().getSimpleValue(
            TeamDataConstants.PROJECT_ID);
        if (projectIDVal != null)
            projectID = projectIDVal.format();
        return projectID + ":" + wbsId;
    }

    protected String getScript() {
        String destUri = getParameter(DEST_URI_PARAM);
        return StringUtils.findAndReplace(SCRIPT, "DESTURI",
                HTMLUtils.escapeEntities(destUri));
    }

    private static final String SCRIPT =
        "<form method='POST' action='selectWBSData'>\n" +
        "<input type='hidden' name='"+REL_PATH_PARAM+"' value=''>\n" +
        "<input type='hidden' name='"+WBS_ID_PARAM+"' value=''>\n" +
        "<input type='hidden' name='"+DEST_URI_PARAM+"' value='DESTURI'>\n" +
        "</form>\n" +
        "<script>\n" +
        "  function doClick(relPath, wbsId) {\n" +
        "    document.forms[0].elements[0].value = relPath;\n" +
        "    document.forms[0].elements[1].value = wbsId;\n" +
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

    public static boolean usesDataBasedFilter(DataRepository data, String path) {
        String dataName = DataRepository.createDataName(path,
            "Rollup_Uses_In_Place_WBS_Filter");
        SimpleData value = data.getSimpleValue(dataName);
        return value != null && value.test();
    }

}
