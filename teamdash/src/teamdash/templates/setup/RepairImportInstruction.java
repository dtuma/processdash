package teamdash.templates.setup;

import java.io.File;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.util.StringUtils;

public class RepairImportInstruction {

    public static void maybeRepairForIndividual(DataContext data) {
        String projectID = getString(data, TeamDataConstants.PROJECT_ID);
        String prefix = "Import_" + projectID;

        String[] locations = new String[2];

        String url = getString(data, TeamDataConstants.TEAM_DATA_DIRECTORY_URL);
        if (StringUtils.hasValue(url))
            locations[0] = url + "-disseminate";

        String teamDir = getString(data, TeamDataConstants.TEAM_DATA_DIRECTORY);
        if (StringUtils.hasValue(teamDir))
            locations[1] = new File(teamDir, "disseminate").getPath();

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
