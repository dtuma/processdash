import java.io.File;
import java.io.IOException;

import pspdash.PSPProperties;
import pspdash.PropertyKey;
import pspdash.TinyCGIBase;
import pspdash.TinyCGIException;
import pspdash.data.DataRepository;
import pspdash.data.SimpleData;

public class sync extends TinyCGIBase {


    private String projectRoot;
    private String processID;
    private File wbsFile;
    private String initials;
    private boolean isTeam;
    private boolean fullCopyMode;


    public void writeContents() throws IOException {

        // locate the root of the included project.
        findProject();

        // load data values from that project.
        loadValues();

        HierarchySynchronizer synch = new HierarchySynchronizer
            (projectRoot, processID, wbsFile, initials, fullCopyMode,
             getPSPProperties(), getDataRepository());

        synch.setWhatIfMode(parameters.containsKey("whatIf"));
        synch.sync();
        synch.dumpChanges(out);
    }

    private void findProject() {
        PSPProperties hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        do {
            String templateID = hierarchy.getID(key);

            if (templateID != null && templateID.endsWith(TEAM_ROOT)) {
                projectRoot = key.path();
                processID = templateID.substring
                    (0, templateID.length() - TEAM_ROOT.length());
                isTeam = true;
                return;
            }
            if (templateID != null && templateID.endsWith(INDIV_ROOT)) {
                projectRoot = key.path();
                processID =
                    templateID.substring(
                        0,
                        templateID.length() - INDIV_ROOT.length());
                isTeam = false;
                return;
            }

            key = key.getParent();
        } while (key != null);

        projectRoot = processID = null;
    }

    private void loadValues() throws IOException {
        if (projectRoot == null)
            signalError("This synchronization tool only works from "+
                        "within a team project.");

        DataRepository data = getDataRepository();

        String teamDirectory = null;
        SimpleData d = data.getSimpleValue
            (DataRepository.createDataName(projectRoot, TEAMDIR_DATA_NAME));
        if (d == null || !d.test() ||
            "Enter network directory path".equals(teamDirectory = d.format()))
            signalError("Team directory unspecified");

        wbsFile = new File(teamDirectory, HIER_FILENAME);

        if (isTeam)
            initials = null;
        else {
            d = data.getSimpleValue(DataRepository.createDataName
                                    (projectRoot, INITIALS_DATA_NAME));
            if (d == null || !d.test() ||
                "tttt".equals(initials = d.format()))
                signalError("Team member initials unspecified");
        }

        d = data.getSimpleValue(DataRepository.createDataName
                                (projectRoot, FULLCOPY_DATA_NAME));
        fullCopyMode = (d != null && d.test());
    }

    private void signalError(String message) throws TinyCGIException {
        // fix this later
        throw new TinyCGIException(500, message);
    }

    private static final String TEAM_ROOT = "/TeamRoot";
    private static final String INDIV_ROOT = "/IndivRoot";
    private static final String TEAMDIR_DATA_NAME = "Team_Directory";
    private static final String INITIALS_DATA_NAME = "Indiv_Initials";
    private static final String FULLCOPY_DATA_NAME = "Sync_Full_WBS";
    private static final String HIER_FILENAME = "projDump.xml";

}
