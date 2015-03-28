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

package net.sourceforge.processdash.team.setup.migrate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.sync.SyncWBS;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;

/**
 * This class initiates the process to convert a team project from one metrics
 * collection framework to another.
 */
public class ConvertTeamProjectMCF extends TinyCGIBase {

    /** The hierarchy path to the root of the enclosing team project */
    private String projectRoot;

    /** The processID used by the enclosing team project */
    private String processID;


    @Override
    protected void writeContents() throws IOException {
        MigrationException me = null;

        try {
            findProject();
            String redirect = handleTeam();
            sendRedirect(redirect);
        } catch (MigrationException mp) {
            me = mp;
        } catch (Exception e) {
            me = new MigrationException(e);
        }

        if (me != null) {
            me.printStackTrace();
            sendRedirect(me.getURL("convertError.shtm"));
        }
    }

    private String handleTeam() throws Exception {
        if (!parameters.containsKey(CONFIRM_PARAM)) {
            setupProcessConversionOptions();
            return "convertTeamConfirm.shtm";
        } else {
            String targetPID = getParameter(TARGET_PID_PARAM);
            if (!StringUtils.hasValue(targetPID))
                throw new MigrationException("noTargetPIDSelected");

            MigrationToolTeam mtt = new MigrationToolTeam(
                    getDashboardContext(), projectRoot, targetPID);
            mtt.convert();
            SyncWBS.startBackgroundExport(projectRoot);
            return "convertTeamSuccess.shtm";
        }
    }



    /**
     * Locates the enclosing team project, and sets the values of the
     * {@link #projectRoot} and {@link #processID} fields accordingly. If there
     * is no enclosing team project, both will be set to null.
     */
    private void findProject() throws MigrationException {
        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        while (key != null) {
            String templateID = hierarchy.getID(key);

            if (templateID != null && templateID.endsWith(MASTER_ROOT)) {
                throw new MigrationException("notSupportedMaster");
            }

            String teamTemplateID = getTeamPID(templateID);
            if (teamTemplateID != null) {
                projectRoot = key.path();
                processID = teamTemplateID;
                return;
            }

            key = key.getParent();
        }

        throw new MigrationException("notTeamProject");
    }


    private void setupProcessConversionOptions() throws MigrationException {
        Map<String, String> processes = getTeamProcessIDs();
        ListData pidList = new ListData();

        for (Map.Entry<String, String> e : processes.entrySet()) {
            String onePID = e.getKey();
            String oneName = e.getValue();
            if (processID.equals(onePID))
                continue;

            pidList.add(onePID);
            putStringValue(DISPLAY_DATA_NAME_PREFIX + onePID, oneName);
        }

        if (pidList.size() == 0)
            throw new MigrationException("noOtherTeamProcesses");

        putSimpleValue(ID_LIST_DATA_NAME, pidList);
    }

    /**
     * Get a list of all the team processes installed in the dashboard.
     * 
     * @return a Map mapping process IDs to process names
     */
    private Map<String, String> getTeamProcessIDs() {
        Map<String, String> result = new HashMap<String, String>();

        // iterate over all the processes in the dashboard
        Map templates = DashController.getTemplates();
        for (Iterator i = templates.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String templateID = (String) e.getKey();
            String templateName = (String) e.getValue();

            // if this appears to be a team process template, add it to our
            // results list.
            String pid = getTeamPID(templateID);
            if (pid != null)
                result.put(pid, templateName);
        }

        return result;
    }

    /**
     * If the given template ID appears to describe the root of a team project,
     * return the associated process ID. Otherwise, return null.
     */
    private String getTeamPID(String templateID) {
        if (templateID != null && templateID.endsWith(TEAM_ROOT))
            return templateID.substring(0, templateID.length()
                    - TEAM_ROOT.length());
        else
            return null;
    }



    private void sendRedirect(String url) {
        out.write("<html><head>");
        out.write("<meta http-equiv='Refresh' CONTENT='0;URL=");
        out.write(url);
        out.write("'></head><body></body></html>");
    }

    private void putStringValue(String name, String value) {
        putSimpleValue(name, StringData.create(value));
    }

    private void putSimpleValue(String name, SimpleData value) {
        String dataName = DataRepository.createDataName(projectRoot, name);
        getDataRepository().putValue(dataName, value);
    }



    private static final String MASTER_ROOT = "/MasterRoot";

    private static final String TEAM_ROOT = "/TeamRoot";

    private static final String ID_LIST_DATA_NAME = "Conversion//PID_List";

    private static final String DISPLAY_DATA_NAME_PREFIX = "Conversion//Display_Name_";

    private static final String CONFIRM_PARAM = "run";

    private static final String TARGET_PID_PARAM = "PID";

}
