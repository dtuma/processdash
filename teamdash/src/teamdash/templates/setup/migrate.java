package teamdash.templates.setup;

import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

/**
 * This class initiates the process to migrate a team project from the old
 * format to the new format.
 */
public class migrate extends TinyCGIBase {

    /** The hierarchy path to the root of the enclosing team project */
    private String projectRoot;

    /** The processID used by the enclosing team project */
    private String processID;

    /** True if this is the team rollup side of the project */
    private boolean isTeam;


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
            sendRedirect(me.getURL());
        }
    }

    private String handleTeam() throws Exception {
        if (!parameters.containsKey(CONFIRM_PARAM)) {
            return "migrateTeamConfirm.shtm";
        } else {
            MigrationToolTeam mtt = new MigrationToolTeam(
                    getDashboardContext(), projectRoot);
            mtt.migrate();
            sync.startBackgroundExport(projectRoot);
            return "migrateTeamSuccess.shtm";
        }
    }

    private String handleIndividual() throws Exception {
        if (!parameters.containsKey(CONFIRM_PARAM)) {
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
                isTeam = false;
                return;
            }

            if (templateID != null && templateID.endsWith(INDIV2_ROOT)) {
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
        out.write("<meta http-equiv='Refresh' CONTENT='0;URL=");
        out.write(url);
        out.write("'></head><body></body></html>");
    }

    private boolean getBooleanValue(String name) {
        SimpleData d = getSimpleValue(name);
        return (d == null ? false : d.test());
    }

    private SimpleData getSimpleValue(String name) {
        String dataName = DataRepository.createDataName(projectRoot, name);
        return getDataRepository().getSimpleValue(dataName);
    }

    private static final String MASTER_ROOT = "/MasterRoot";

    private static final String TEAM_ROOT = "/TeamRoot";

    private static final String INDIV_ROOT = "/IndivRoot";

    private static final String INDIV2_ROOT = "/Indiv2Root";

    private static final String CONFIRM_PARAM = "run";

}
