import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

/** CGI script which synchonizes a dashboard hierarchy with a WBS description.
 * 
 * This script works on both the team and the inidividual sides of a
 * dashboard team project.
 * 
 * When called without parameters, this script:<ul>
 * <li>Checks entry criteria and displays an error page if it discovers a
 * problem.
 * <li>If the entry criteria for the script have been met, this performs a
 * trial (aka "whatIf") synchronization run to see if there is any work to do.
 * If it discovers that the hierarchy is already up to date, it displays a page
 * announcing that fact.
 * <li>If synchonization work is required, this displays a page
 * containing the message "synchronizing...please wait"; this includes an
 * HTTP_REFRESH instruction pointing to "sync.class?run"
 * </ul>
 * 
 * When called with the "run" parameter, this script:<ul>
 * <li>Checks entry criteria and displays an error page if it discovers a
 * problem.
 * <li>Performs the requested synchronization, and displays the results.
 * </ul>
 */
public class sync extends TinyCGIBase {

    /** The hierarchy path to the root of the enclosing team project */
    private String projectRoot;
    /** The processID used by the enclosing team project */
    private String processID;
    /** The wbs dump file, written by a WBSDataWriter */
    private File wbsFile;
    /** The initials of the current team member, if applicable */
    private String initials;
    /** True if this is the team rollup side of the project */
    private boolean isTeam;
    /** true if the user wants us to copy all software component and document
     * nodes in the WBS, even if they aren't assigned to them */
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
            // the signalError() method uses a TinyCGIException to abort
            // processing;  that exception is caught here and used to draw
            // the error page.
            showErrorPage(e.getTitle(), e.getText());
        } catch (HierarchyAlterationException h) {
            showErrorPage("generalError", h.getMessage());
        } catch (IOException ioe) {
            showErrorPage("generalError", ioe.getMessage());
        }
    }



    /** Locates the enclosing team project, and sets the values of the
     * {@link #projectRoot} and {@link #processID} fields accordingly.
     * If there is no enclosing team project, both will be set to null.
         */
    private void findProject() {
        DashHierarchy hierarchy = getPSPProperties();
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



    /** Load values out of the data repository.  Redirect to an error page if
     * any of the required data is missing or invalid.
     */
    private void loadValues() throws IOException {
        // ensure we are within a team project.
        if (projectRoot == null)
            signalError(NOT_TEAM_PROJECT);

        // ensure that the hierarchy editor is not open.
        if (DashController.isHierarchyEditorOpen())
            signalError(HIER_EDITOR_OPEN);

        DataRepository data = getDataRepository();

        // find the data directory for this team project.
        String teamDirectory = null;
        SimpleData d = data.getSimpleValue
            (DataRepository.createDataName(projectRoot, TEAMDIR_DATA_NAME));
        if (d == null || !d.test() ||
            "Enter network directory path".equals(teamDirectory = d.format()))
            signalError(TEAM_DIR_MISSING);
        File teamDir = new File(teamDirectory);
        if (!teamDir.isDirectory())
            signalError(TEAM_DIR_MISSING);

        // locate the wbs file in the team data directory.
        wbsFile = new File(teamDirectory, HIER_FILENAME);
        if (!wbsFile.exists())
            signalError(WBS_FILE_MISSING + "&wbsFile", wbsFile.toString());
        if (!wbsFile.canRead())
            signalError(WBS_FILE_INACCESSIBLE + "&wbsFile",
                        wbsFile.toString());

        if (isTeam)
            initials = null;
        else {
            // get the initials of the current team member.
            d = data.getSimpleValue(DataRepository.createDataName
                                    (projectRoot, INITIALS_DATA_NAME));
            if (d == null || !d.test() ||
                "tttt".equals(initials = d.format()))
                signalError(INITIALS_MISSING);
        }

        // check to see whether the user wants us to perform a full wbs sync.
        d = data.getSimpleValue(DataRepository.createDataName
                                (projectRoot, FULLCOPY_DATA_NAME));
        fullCopyMode = (d != null && d.test());
    }



    /** Check to see if a synchronization operation is needed. If so,
     * display a "please wait" page, then initiate the operation.
     */
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



    /** Synchronize the hierarchy and display the results.
     */
    private void synchronize(HierarchySynchronizer synch)
        throws HierarchyAlterationException
    {
        synch.setWhatIfMode(false);
        synch.sync();
        DashController.exportData(projectRoot);
        printChanges(synch.getChanges());
    }



    /** Print a page asking the user to wait. This page includes an
     * HTTP "refresh" instruction that will initiate the synchronization
     * operation.
     */
    private void printWaitPage() {
        out.print("<html><head>");
        out.print("<title>Synchronizing Work Breakdown Structure</title>");
        out.print("<meta http-equiv='Refresh' content='1;URL=sync.class?run'>");
        out.print("</head>");
        out.print("<body><h1>Synchronizing...</h1>");
        out.print("Please wait.");
        out.print("</body></html>");
    }



    /** Print a list of changes made by a synchronization operation.
     */
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



    /** Throw an exception that will stop processing and redirect to an
     * error page.
     */
    private void signalError(String reason) throws TinyCGIException {
        signalError(reason, null);
    }
    /** Throw an exception that will stop processing and redirect to an
     * error page.
     */
    private void signalError(String reason, String value) throws TinyCGIException {
        throw new TinyCGIException(500, reason, value);
    }

    /** Redirect to an error page.
     */
    private void showErrorPage(String reason, String value) throws IOException {
        out.write("<html><head>");
        out.write("<meta http-equiv='Refresh' CONTENT='0;URL=syncError.shtm?");
        out.write(reason);
        if (value != null)
            out.write("=" + HTMLUtils.urlEncode(value));
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
