// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.setup;

import static net.sourceforge.processdash.team.setup.TeamStartBootstrap.TEAM_PID;
import static net.sourceforge.processdash.team.setup.TeamStartBootstrap.TEAM_PID_LIST;
import static net.sourceforge.processdash.team.setup.TeamStartBootstrap.TEAM_PROC_NAME;

import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ImmutableStringData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.util.ObjectCounter;

/**
 * This class finds a set of process templates representing MCF root nodes,
 * and saves metadata about those templates into temporary variables in the
 * data repository. It also identifies a "recommended" MCF to use for the
 * creation of a new project.
 */
public class TeamStartMcfUtil {
    
    /**
     * Find a list of the metrics collection frameworks known to this dashboard.
     * 
     * If only one is present, save it as the "chosen" MCF for setup purposes,
     * and return the template ID.
     * 
     * If multiple options are found, save them into the DataContext. Choose the
     * "best" option and set it as the "chosen" MCF. Then return null.
     */
    public static String setupMcfMetadata(DataContext data, DashHierarchy hier,
            String templateIdSuffix) {
        TeamStartMcfUtil mcfUtil = new TeamStartMcfUtil(data, hier,
                templateIdSuffix);
        return mcfUtil.setupMcfMetadata();
    }



    private DataContext data;

    private DashHierarchy hier;

    private String templateIdSuffix;

    private TeamStartMcfUtil(DataContext data, DashHierarchy hier,
            String templateIdSuffix) {
        this.data = data;
        this.hier = hier;
        this.templateIdSuffix = templateIdSuffix;
    }

    private String setupMcfMetadata() {
        // get a list of all the team processes
        Map processes = getTeamProcesses();
        Map.Entry e;

        // If there is only one process installed, save and return it
        if (processes.size() == 1) {
            e = (Map.Entry) processes.entrySet().iterator().next();
            putValue(TEAM_PID, (String) e.getKey());
            putValue(TEAM_PROC_NAME, (String) e.getValue());
            putValue(TEAM_PID_LIST, "");
            return (String) e.getKey();
        }

        if (processes.size() > 0) {
            // Save information about the available processes into the
            // data repository.
            Iterator i = processes.entrySet().iterator();
            String pidList = ";";
            while (i.hasNext()) {
                e = (Map.Entry) i.next();
                String pid = (String) e.getKey();
                String processName = (String) e.getValue();
                pidList = pidList + pid + ";";
                putValue("setup//Process_Name{"+pid+"}", processName);
            }
            putValue(TEAM_PID_LIST, pidList);

            if (getValue(TEAM_PID) == null) {
                // identify a suggested process, and write it into the repository.
                String suggestedPid = findMostCommonlyUsedTeamProcessId();
                if (suggestedPid == null)
                    suggestedPid = chooseMostCustomProcessId(processes.keySet());
                putValue(TEAM_PID, suggestedPid);
            }
        }

        return null;
    }

    /** Get a list of all the team processes installed in the dashboard.
     * @return a Map mapping process IDs to process names
     */
    protected Map getTeamProcesses() {
        // get a list of all the processes in the dashboard
        Map templates = DashController.getTemplates();
        Iterator i = templates.keySet().iterator();
        while (i.hasNext()) {
            String id = (String) i.next();
            // filter out process templates which are not "team roots"
            if (!id.endsWith(templateIdSuffix))
                i.remove();
        }
        return templates;
    }

    /** Look at past projects to see if a particular team process has been
     * used often.  If so, return that process ID.
     */
    private String findMostCommonlyUsedTeamProcessId() {
        ObjectCounter<String> counts = new ObjectCounter<String>();
        countProcessUsage(counts, hier, PropertyKey.ROOT);
        return chooseMostCustomProcessId(counts.getMostCommonObjects());
    }

    private void countProcessUsage(ObjectCounter<String> counts,
            DashHierarchy hier, PropertyKey node) {
        String templateId = hier.getID(node);
        if (templateId != null && templateId.endsWith(templateIdSuffix)) {
            counts.add(templateId);
        } else {
            for (int i = hier.getNumChildren(node);  i-- > 0;)
                countProcessUsage(counts, hier, hier.getChildKey(node, i));
        }
    }

    private String chooseMostCustomProcessId(Iterable<String> pids) {
        String result = null;
        if (pids != null) {
            for (String onePid : pids) {
                int resultPref = getProcessCustomizationRating(result);
                int onePref = getProcessCustomizationRating(onePid);
                result = (resultPref > onePref ? result : onePid);
            }
        }
        return result;
    }
    private int getProcessCustomizationRating(String templateId) {
        if (templateId == null) return -1;
        if (templateId.startsWith("PDSSD/")) return 0; // open source process
        if (templateId.startsWith("TSP/")) return 1;  // the TSP process
        return 2;  // a custom process
    }

    private SimpleData getValue(String name) {
        return data.getSimpleValue(name);
    }

    private void putValue(String name, String value) {
        if (value == null) value = "";
        data.putValue(name, new ImmutableStringData(value));
    }

}
