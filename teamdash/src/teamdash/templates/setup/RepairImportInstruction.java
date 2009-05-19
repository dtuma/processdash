package teamdash.templates.setup;

import java.io.File;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.util.StringUtils;

public class RepairImportInstruction {

    public static void maybeRepairForIndividual(DataContext data) {
        maybeRepair(data, true);
    }

    public static void maybeRepairForTeam(DataContext data) {
        maybeRepair(data, false);
    }

    private static void maybeRepair(DataContext data, boolean indiv) {
        String projectID = getString(data, TeamDataConstants.PROJECT_ID);
        String prefix = "Import_" + projectID;

        String[] locations = new String[2];

        String url = getString(data, TeamDataConstants.TEAM_DATA_DIRECTORY_URL);
        if (StringUtils.hasValue(url)) {
            String loc = url;
            if (indiv)
                loc = loc + "-" + TeamDataConstants.DISSEMINATION_DIRECTORY;
            locations[0] = loc;
        }

        String teamDir = getString(data, TeamDataConstants.TEAM_DATA_DIRECTORY);
        if (StringUtils.hasValue(teamDir)) {
            File loc = new File(teamDir);
            if (indiv)
                loc = new File(loc, TeamDataConstants.DISSEMINATION_DIRECTORY);
            locations[1] = loc.getPath();
        }

        try {
            DashController.repairImportSetting(projectID, prefix, locations);
        } catch (Throwable t) {
            // this method will be undefined prior to Process Dashboard 1.10.5
        }
    }

    private static String getString(DataContext data, String name) {
        SimpleData value = data.getSimpleValue(name);
        return (value == null ? null : value.format());
    }

}
