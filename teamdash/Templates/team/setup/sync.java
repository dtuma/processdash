import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import pspdash.DashController;
import pspdash.HTMLUtils;
import pspdash.PSPProperties;
import pspdash.PropertyKey;
import pspdash.TinyCGIBase;
import pspdash.TinyCGIException;
import pspdash.HierarchyAlterer.HierarchyAlterationException;
import pspdash.data.DataRepository;
import pspdash.data.SimpleData;

/*

    Design:
        called without parameters:
            check for entry criteria (in a project, hierarchy editor closed, etc)
            display an error message if there is a problem.
            otherwise do a trial run and see if there is any work to do.  If no
                work, display an up to date message.
            if there is work to do, display a "please wait" page, with
                an HTTP REFRESH pointing to "sync.class?run"

        called with run parameter
            check for entry criteria (in a project, hierarchy editor closed, etc)
            display an error message if there is a problem.
            do work, display output.

 */

public class sync extends TinyCGIBase {


    private String projectRoot;
    private String processID;
    private File wbsFile;
    private String initials;
    private boolean isTeam;
    private boolean fullCopyMode;


    public void writeContents() throws IOException {

        try {
            // locate the root of the included project.
            findProject();

            // load data values from that project.
            loadValues();

            // create a synchronization object.
            HierarchySynchronizer synch = new HierarchySynchronizer
                (projectRoot, processID, wbsFile, initials, fullCopyMode,
                 getPSPProperties(), getDataRepository());

            // start the synchronization process.
            if (parameters.containsKey("run"))
                synchronize(synch);
            else
                maybeSynchronize(synch);

        } catch (TinyCGIException e) {
            showErrorPage(e.getTitle(), e.getText());
        } catch (HierarchyAlterationException h) {
            showErrorPage("generalError", h.getMessage());
        } catch (IOException ioe) {
            showErrorPage("generalError", ioe.getMessage());
        }
    }

    private void findProject() {
        PSPProperties hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(getPrefix());
        while (key != null) {
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
        }

        projectRoot = processID = null;
    }

    private void loadValues() throws IOException {
        if (projectRoot == null)
            signalError(NOT_TEAM_PROJECT);

        if (DashController.isHierarchyEditorOpen())
            signalError(HIER_EDITOR_OPEN);

        DataRepository data = getDataRepository();

        String teamDirectory = null;
        SimpleData d = data.getSimpleValue
            (DataRepository.createDataName(projectRoot, TEAMDIR_DATA_NAME));
        if (d == null || !d.test() ||
            "Enter network directory path".equals(teamDirectory = d.format()))
            signalError(TEAM_DIR_MISSING);
        File teamDir = new File(teamDirectory);
        if (!teamDir.isDirectory())
            signalError(TEAM_DIR_MISSING);

        wbsFile = new File(teamDirectory, HIER_FILENAME);
        if (!wbsFile.exists())
            signalError(WBS_FILE_MISSING + "&wbsFile", wbsFile.toString());
        if (!wbsFile.canRead())
            signalError(WBS_FILE_INACCESSIBLE + "&wbsFile",
                        wbsFile.toString());

        if (isTeam)
            initials = null;
        else {
            d = data.getSimpleValue(DataRepository.createDataName
                                    (projectRoot, INITIALS_DATA_NAME));
            if (d == null || !d.test() ||
                "tttt".equals(initials = d.format()))
                signalError(INITIALS_MISSING);
        }

        d = data.getSimpleValue(DataRepository.createDataName
                                (projectRoot, FULLCOPY_DATA_NAME));
        fullCopyMode = (d != null && d.test());
    }

    private void maybeSynchronize(HierarchySynchronizer synch)
        throws HierarchyAlterationException
    {
        synch.setWhatIfMode(true);
        synch.sync();
        if (synch.getChanges().isEmpty())
            printChanges(synch.getChanges());
        else
            printWaitPage();
    }

    private void printWaitPage() {
        out.print("<html><head>");
        out.print("<title>Synchronizing Work Breakdown Structure</title>");
        out.print("<meta http-equiv='Refresh' content='1;URL=sync.class?run'>");
        out.print("</head>");
        out.print("<body><h1>Synchronizing...</h1>");
        out.print("Please wait.");
        out.print("</body></html>");
    }

    private void synchronize(HierarchySynchronizer synch)
        throws HierarchyAlterationException
    {
        synch.setWhatIfMode(false);
        synch.sync();
        printChanges(synch.getChanges());
    }

    private void printChanges(List list) {
        out.print("<html><head><title>Synchronization Complete</title></head>");
        out.print("<body><h1>Synchronization Complete</h1>");
        if (list.isEmpty())
            out.print("<p>Your hierarchy is up to date - no changes "+
                      "were necessary.");
        else {
            out.print("<p>The following changes were made to your hierarchy:");
            out.print("<ul>");
            Iterator i = list.iterator();
            while (i.hasNext()) {
                out.print("<li>");
                out.print(HTMLUtils.escapeEntities(String.valueOf(i.next())));
            }
            out.print("</ul>");
        }
        out.print("</body></html>");
    }



    private void signalError(String reason) throws TinyCGIException {
        signalError(reason, null);
    }
    private void signalError(String reason, String value) throws TinyCGIException {
        throw new TinyCGIException(500, reason, value);
    }

    private void showErrorPage(String reason, String value) throws IOException {
        out.write("<html><head>");
        out.write("<meta http-equiv='Refresh' CONTENT='0;URL=syncError.shtm?");
        out.write(reason);
        if (value != null)
            out.write("=" + URLEncoder.encode(value));
        if (isTeam)
            out.write("&isTeam");
        out.write("'></head><body></body></html>");
    }

    private static final String TEAM_ROOT = "/TeamRoot";
    private static final String INDIV_ROOT = "/IndivRoot";
    private static final String TEAMDIR_DATA_NAME = "Team_Data_Directory";
    private static final String INITIALS_DATA_NAME = "Indiv_Initials";
    private static final String FULLCOPY_DATA_NAME = "Sync_Full_WBS";
    private static final String HIER_FILENAME = "projDump.xml";

    private static final String NOT_TEAM_PROJECT = "notTeamProject";
    private static final String TEAM_DIR_MISSING = "teamDirMissing";
    private static final String WBS_FILE_MISSING = "wbsFileMissing";
    private static final String WBS_FILE_INACCESSIBLE = "wbsFileInaccessible";
    private static final String INITIALS_MISSING = "initialsMissing";
    private static final String HIER_EDITOR_OPEN = "hierEditorOpen";
}
