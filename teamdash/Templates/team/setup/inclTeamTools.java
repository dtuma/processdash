import java.io.IOException;
import java.net.URLEncoder;

import pspdash.TinyCGIBase;
import pspdash.data.DataRepository;
import pspdash.data.SimpleData;

public class inclTeamTools extends TinyCGIBase {

    private static final String URL = "../../team/tools/index.shtm?directory=";

    protected void writeContents() throws IOException {
        try {
            String prefix = getPrefix();
            if (prefix == null) return;

            DataRepository data = getDataRepository();
            String dataName = DataRepository.createDataName
                (prefix, "Team_Data_Directory");
            SimpleData d = data.getSimpleValue(dataName);
            if (d == null || !d.test()) {
                out.print(TEAM_DIR_MISSING_MSG);
                return;
            }

            String directory = d.format();
            String url = URL + URLEncoder.encode(directory);
            outStream.write(getRequest(url, true));
        } catch (Exception e) {
            out.print(TOOLS_MISSING_MSG);
        }
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
