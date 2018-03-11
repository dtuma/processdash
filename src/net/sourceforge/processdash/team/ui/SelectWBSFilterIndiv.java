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

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class SelectWBSFilterIndiv extends SelectWBSNode {

    private static final String REL_PATH_PARAM = "relPath";
    private static final String DEST_URI_PARAM = "destUri";
    private static final String WBS_FILTER_DATA_NAME = "Project_WBS_ID//Filter";
    private static final String EV_FILTER_DATA_NAME = "Earned_Value//Path_Filter";


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
        String result = "";
        SimpleData wbsDataName = getDataContext().getSimpleValue("Project_ID");
        if (wbsDataName != null)
            result = wbsDataName.format();

        String relPath = getParameter(REL_PATH_PARAM);
        if (relPath != null && relPath.length() > 0)
            result = result + "/" + relPath;

        return result;
    }

    protected String getNewEVPathFilter() {
        String relPath = getParameter(REL_PATH_PARAM);
        if (StringUtils.hasValue(relPath))
            return getPrefix() + "/" + relPath;
        else
            return null;
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
