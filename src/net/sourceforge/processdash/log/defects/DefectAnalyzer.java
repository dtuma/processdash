// Copyright (C) 2001-2016 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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


package net.sourceforge.processdash.log.defects;

import java.util.Map;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.Prop;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.tool.db.DatabasePlugin;
import net.sourceforge.processdash.tool.db.QueryUtils;


public class DefectAnalyzer {

    public interface Task {

        /** Analyze or act upon a defect in some way.
         *
         * @param d The defect to be analyzed.  The defect should be
         *          considered read-only.
         */
        void analyze(String path, Defect d);
    }

    public static final String NO_CHILDREN_PARAM = "noChildren";

    public static final String DB_MODE_PARAM = "dbMode";


    public static void run(DashHierarchy props,
                           DataRepository data,
                           String prefix,
                           Map queryParameters,
                           Task t) {

        if (queryParameters.containsKey(DB_MODE_PARAM)) {
            DatabasePlugin plugin = QueryUtils.getDatabasePlugin(data);
            ListData criteria = getDatabaseCriteria(data, prefix,
                queryParameters);
            String pid = getProcessID(data, prefix);
            if (plugin != null && criteria != null && pid != null)
                ImportedDefectManager.run(plugin, criteria.asList(), pid, t);

        } else {
            String [] prefixes = ResultSet.getPrefixList
                    (data, queryParameters, prefix);
            boolean includeChildren = !queryParameters.containsKey(NO_CHILDREN_PARAM);
            run(props, data, prefixes, includeChildren, t);
        }
    }

    private static ListData getDatabaseCriteria(DataRepository data,
            String prefix, Map queryParameters) {
        String dataName = (String) queryParameters.get("for");
        if (dataName.startsWith("[") && dataName.endsWith("]"))
            dataName = dataName.substring(1, dataName.length() - 1);
        dataName = DataRepository.createDataName(prefix, dataName);
        ListData criteria = ListData.asListData(data.getSimpleValue(dataName));
        return criteria;
    }

    private static String getProcessID(DataRepository data, String prefix) {
        String dataName = DataRepository.createDataName(prefix,
            TeamDataConstants.PROCESS_ID);
        SimpleData sd = data.getSimpleValue(dataName);
        return (sd == null ? null : sd.format());
    }

    public static void run(DashHierarchy props, DataRepository data,
            String[] prefixes, boolean includeChildren, Task t) {
        for (int i = 0;   i < prefixes.length;   i++)
            run(props, prefixes[i], includeChildren, t);
        ImportedDefectManager.run(props, data, prefixes, includeChildren, t);
    }

    /** Perform some analysis task on all the defects under a given node
     *  in the hierarchy.
     *
     * @param props The PSPProperties object to be walked.
     * @param path  The path of the node in the hierarchy where the analysis
     *    should start.
     * @param includeChildren if true, the node and all its children will be
     *    analyzed;  if false, only the node itself will be visited.
     * @param t An analysis task to perform.  Every defect encountered will
     *    be passed to the task, in turn.
     */
    public static void run(DashHierarchy props, String path,
            boolean includeChildren, Task t) {
        PropertyKey pKey;
        if (path == null || path.length() == 0)
            pKey = PropertyKey.ROOT;
        else
            pKey = props.findExistingKey(path);

        if (pKey != null)
            run(props, pKey, includeChildren, t);
    }

    /** Perform some analysis task on all the defects under a given node
     *  in the hierarchy.
     *
     * @param props The PSPProperties object to be walked.
     * @param pKey  The PropertyKey of the node in the hierarchy where the
     *    analysis should start.
     * @param includeChildren if true, the node and all its children will be
     *    analyzed;  if false, only the node itself will be visited.
     * @param t An analysis task to perform.  Every defect encountered will
     *    be passed to the task, in turn.
     */
    public static void run(DashHierarchy props, PropertyKey pKey,
            boolean includeChildren, Task t) {
        Prop prop = props.pget (pKey);
        String path = pKey.path();
        String defLogName = prop.getDefectLog ();

        // If this node has a defect log,
        if (defLogName != null && defLogName.length() != 0) {
            DefectLog dl = new DefectLog
                (dataDirectory + defLogName, path, null);
            // read all the defects in that log, and
            Defect[] defects = dl.readDefects();
            for (int d=0;  d < defects.length;  d++)
                if (defects[d] != null)   // pass them to the analyzer task.
                    t.analyze(path, defects[d]);
        }

        // recursively analyze all the children of this node.
        if (includeChildren)
            for (int i = 0; i < prop.getNumChildren(); i++)
                run(props, prop.getChild(i), includeChildren, t);
    }

    private static String dataDirectory;

    /** Register the directory where defect logs will be found.
     * This only needs to be called once, but <b>must</b> be called before
     * calling either variant of run().
     */
    public static void setDataDirectory(String d) {
        dataDirectory = d;
        if (!dataDirectory.endsWith(Settings.sep))
            dataDirectory = dataDirectory + Settings.sep;
    }

    /** Some html content may request a defect analysis with the parameter
     * "for=auto".  This is a request for the defect logic to figure out the
     * most appropriate query parameters to use.  This method performs that
     * task, replacing the "for" parameter with an appropriate value based
     * on the active data context.
     */
    public static void refineParams(Map parameters, DataContext data) {
        if (!"auto".equals(parameters.get("for")))
            return;

        if (hasTag(data, "Database-Driven Rollup Tag")) {
            parameters.put("for", "[DB_Filter_Criteria]");
            parameters.put(DB_MODE_PARAM, "t");
        } else if (hasTag(data, "Rollup Tag")) {
            parameters.put("for", "[Rollup_List]");
            if (hasTag(data, "Historical Data Tag"))
                parameters.put("order", "Completed");
            if (hasTag(data, "Exclude Children of Rollup_List for Defects"))
                parameters.put(NO_CHILDREN_PARAM, "t");
        } else {
            parameters.put("for", ".");
        }
    }

    static boolean hasTag(DataContext data, String name) {
        return data.getSimpleValue(name) instanceof TagData;
    }
}
