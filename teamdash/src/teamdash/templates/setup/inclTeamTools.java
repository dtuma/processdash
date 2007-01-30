package teamdash.templates.setup;
import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;


public class inclTeamTools extends TinyCGIBase {

    private static final String WBS_EDITOR_URL =
        "../../team/tools/index.shtm?directory=";
    private static final String MASTER_PARAM = "&master";
    private static final String SYNC_PARAM = "&syncURL=";
    private static final String SYNC_URL = "sync.class?run";

    protected void writeContents() throws IOException {
        try {
            if (getPrefix() == null)
                return;

            String directory = getTeamDataDirectory();
            if (directory == null) {
                out.print(TEAM_DIR_MISSING_MSG);
                return;
            }

            String wbsURL = WBS_EDITOR_URL + HTMLUtils.urlEncode(directory);
            String scriptPath = (String) env.get("SCRIPT_PATH");
            String uri = resolveRelativeURI(scriptPath, wbsURL);

            if (parameters.containsKey("master"))
                uri = uri + MASTER_PARAM;

            String syncURI = resolveRelativeURI(scriptPath, SYNC_URL);
            uri = uri + SYNC_PARAM + HTMLUtils.urlEncode(syncURI);

            outStream.write(getRequest(uri, true));
        } catch (Exception e) {
            out.print(TOOLS_MISSING_MSG);
        }
    }

    private String getTeamDataDirectory() {
        String teamDir = getValue("Team_Directory_UNC");
        if (teamDir == null)
            teamDir = getValue("Team_Directory");
        if (teamDir == null)
            return null;

        String projectID = getValue("Project_ID");
        if (projectID == null || projectID.trim().length() == 0)
            return null;

        File f = new File(teamDir, "data");
        f = new File(f, projectID);
        return f.getPath();
    }

    private String getValue(String name) {
        DataRepository data = getDataRepository();
        String dataName = DataRepository.createDataName(getPrefix(), name);
        SimpleData d = data.getSimpleValue(dataName);
        if (d == null)
            return null;
        String result = d.format();
        if (result == null || result.trim().length() == 0)
            return null;
        else
            return result;
    }

    private static final String TEAM_DIR_MISSING_MSG =
            "<html><body>" +
            "<p><b>The advanced team tools (such as the Custom Process Editor " +
            "and the Work Breakdown Structure Editor) cannot be used until you " +
            "specify a team data directory on the project parameters page.</b>" +
            "</body></html>";

    private static final String TOOLS_MISSING_MSG =
        "<html><body>" +
        "<p><b>The advanced team tools (such as the Custom Process Editor " +
        "and the Work Breakdown Structure Editor) have not been installed " +
        "in this instance of the dashboard.</b>" +
        "</body></html>";

}
