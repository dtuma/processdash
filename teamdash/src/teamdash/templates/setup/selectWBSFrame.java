// Copyright (C) 2002-2010 Tuma Solutions, LLC
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


package teamdash.templates.setup;
import net.sourceforge.processdash.data.DoubleData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.WebServer;


public class selectWBSFrame extends selectWBS {

    private String destUri = null;

    private int maxDepth = -1;


    protected void initialize(DashHierarchy hierarchy, PropertyKey key,
            String id, int depth, String rootPath) {
        super.initialize(hierarchy, key, id, depth, rootPath);
        initMaxDepth(rootPath);
        destUri = null;
    }

    protected boolean prune(DashHierarchy hierarchy, PropertyKey key,
            String id, int depth, String rootPath) {
        if (maxDepth >= 0 && depth > maxDepth)
            return true;
        else
            return super.prune(hierarchy, key, id, depth, rootPath);
    }

    private void initMaxDepth(String prefix) {
        if (!parameters.containsKey("EXPORT")) {
            // if we aren't currently exporting, display nodes to any arbitrary
            // depth for drill-down.
            maxDepth = -1;
        } else {
            // when exporting, check to see if the user has configured
            // a desired depth for drill-down
            String dataName = DataRepository.createDataName(prefix,
                    MAX_EXPORT_DEPTH_DATA_NAME);
            SimpleData data = getDataRepository().getSimpleValue(dataName);
            if (data instanceof DoubleData) {
                DoubleData doubleVal = (DoubleData) data;
                maxDepth = doubleVal.getInteger();
            } else {
                maxDepth = DEFAULT_MAX_EXPORT_DEPTH;
            }
        }
    }

    protected String getScript() { return ""; }

    protected void printLink(String rootPath, String relPath) {
        String target = getParameter("target");
        if (target == null) target = "topFrame";
        out.print("<a target='" + target + "' href=\"");
        out.print(WebServer.urlEncodePath(rootPath));
        if (relPath != null && relPath.length() > 0) {
            out.print("/");
            out.print(WebServer.urlEncodePath(relPath));
        }
        out.print("/");
        out.print(getDestUri());
        out.print("\">");
    }

    /** Get the uri of the dashboard resource which should be displayed
     * at the new prefix  (e.g  "/PID/summary_frame.shtm") */
    protected String getDestUri() {
        if (destUri == null)
            // first, check to see if a destUri was specified.
            destUri = getParameter("destUri");

        if (destUri == null) {
            // build the default destUri based on our process ID.
            String scriptName = (String) env.get("SCRIPT_NAME");
            int slashPos = scriptName.indexOf('/', 1);
            String processID = scriptName.substring(1, slashPos);
            destUri = "/" + processID + "/summary_frame.shtm";
        }

        return destUri;
    }

    private static final String MAX_EXPORT_DEPTH_DATA_NAME =
        "Export_Max_Node_Depth";
    private static final int DEFAULT_MAX_EXPORT_DEPTH = 2;
}
