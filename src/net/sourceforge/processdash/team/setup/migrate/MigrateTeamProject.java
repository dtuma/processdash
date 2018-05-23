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

package net.sourceforge.processdash.team.setup.migrate;

import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.team.sync.SyncWBS;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;

/**
 * This class initiates the process to migrate a team project from the old
 * format to the new format.
 */
public class MigrateTeamProject extends TinyCGIBase {

    /** The hierarchy path to the root of the enclosing team project */
    private String projectRoot;

    /** The processID used by the enclosing team project */
    @SuppressWarnings("unused")
    private String processID;

    /** The processID that the project should be converted to, if applicable */
    private String convertToProcessID;

    /** True if this is the team rollup side of the project */
    private boolean isTeam;


    @Override
    protected void doPost() throws IOException {
        rejectCrossSiteRequests(env);
        parseFormData();
        super.doPost();
    }

    @Override
    protected void writeContents() throws IOException {
        MigrationException me = null;

        try {
            findProject();
            String redirect;
            if (isTeam)
                redirect = handleTeam();
            else
                redirect = handleIndividual();

            sendRedirect(redirect);
        } catch (MigrationException mp) {
            me = mp;
        } catch (Exception e) {
            me = new MigrationException(e);
        }

        if (me != null) {
            me.printStackTrace();
            sendRedirect(me.getURL("migrateError.shtm"));
        }
    }

    private String handleTeam() throws Exception {
        if (!parameters.containsKey(CONFIRM_PARAM) || !checkPostToken()) {
            generatePostToken();
            return "migrateTeamConfirm.shtm";
        } else {
            MigrationToolTeam mtt = new MigrationToolTeam(
                    getDashboardContext(), projectRoot, null);
            mtt.migrate();
            SyncWBS.startBackgroundExport(projectRoot);
            return "migrateTeamSuccess.shtm";
        }
    }

    private String handleIndividual() throws Exception {
        if (!parameters.containsKey(CONFIRM_PARAM) || !checkPostToken()) {
            generatePostToken();
            return "migrateIndivConfirm.shtm";
        } else {
            MigrationToolIndivLauncher mti = new MigrationToolIndivLauncher(
                getDashboardContext(), projectRoot);
            mti.startMigration();
            return "migrateIndivWait.shtm";
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
                throw new MigrationException("notNeededMaster");
            }

            if (templateID != null && templateID.endsWith(TEAM_ROOT)) {
                projectRoot = key.path();
                processID = templateID.substring(0, templateID.length()
                        - TEAM_ROOT.length());
                isTeam = true;
                convertToProcessID = null;

                if (getBooleanValue("Team_Project_Migration_Complete"))
                    throw new MigrationException("alreadyUpgraded");
                if (getBooleanValue("Individuals_Using_Stubless_Phases"))
                    throw new MigrationException("upgradeNotNeeded");
                return;
            }

            if (templateID != null && templateID.endsWith(INDIV_ROOT)) {
                projectRoot = key.path();
                processID = templateID.substring(0, templateID.length()
                        - INDIV_ROOT.length());
                convertToProcessID = getStringValue(CONVERT_DATA_NAME);
                isTeam = false;
                return;
            }

            if (templateID != null && templateID.endsWith(INDIV2_ROOT)) {
                projectRoot = key.path();
                processID = templateID.substring(0, templateID.length()
                        - INDIV2_ROOT.length());
                convertToProcessID = getStringValue(CONVERT_DATA_NAME);
                if (StringUtils.hasValue(convertToProcessID))
                    return;

                if (getBooleanValue("Team_Project_Migration_Complete"))
                    throw new MigrationException("alreadyUpgraded");
                else
                    throw new MigrationException("upgradeNotNeeded");
            }

            key = key.getParent();
        }

        throw new MigrationException("notTeamProject");
    }

    private void sendRedirect(String url) {
        out.write("<html><head>");
        writeRedirectInstruction(url, 0);
        out.write("</head><body></body></html>");
    }

    private boolean getBooleanValue(String name) {
        SimpleData d = getSimpleValue(name);
        return (d == null ? false : d.test());
    }

    private String getStringValue(String name) {
        SimpleData d = getSimpleValue(name);
        return (d == null ? null : d.format());
    }

    private SimpleData getSimpleValue(String name) {
        String dataName = DataRepository.createDataName(projectRoot, name);
        return getDataRepository().getSimpleValue(dataName);
    }

    private static final String MASTER_ROOT = "/MasterRoot";

    private static final String TEAM_ROOT = "/TeamRoot";

    private static final String INDIV_ROOT = "/IndivRoot";

    private static final String INDIV2_ROOT = "/Indiv2Root";

    private static final String CONVERT_DATA_NAME = "Team_Project_Conversion_Needed";

    private static final String CONFIRM_PARAM = "run";

}
