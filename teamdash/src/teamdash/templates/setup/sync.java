package teamdash.templates.setup;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import teamdash.FilenameMapper;

import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

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
    /** The unique ID assigned to the enclosing team project */
    private String projectID;
    /** The processID used by the enclosing team project */
    private String processID;
    /** The wbs dump file, written by a WBSDataWriter */
    private File wbsFile;
    /** The workflow dump file, written by a WBSDataWriter */
    private File workflowFile;
    /** The templates directory for the project */
    private File templatesDir;
    /** The initials of the current team member, if applicable */
    private String initials;
    /** True if this is the team rollup side of the project */
    private boolean isTeam;
    /** True if this is a master project rollup */
    private boolean isMaster;
    /** true if the user wants us to copy all software component and document
     * nodes in the WBS, even if they aren't assigned to them */
    private boolean fullCopyMode;



    protected void doPost() throws IOException {
        parseFormData();
        super.doPost();
    }



    public void writeContents() throws IOException {
        try {
            if (Settings.getBool("READ_ONLY", false))
                signalError("generalError", READ_ONLY_MODE_ERR_MESSAGE);

            // locate the root of the included project.
            findProject();

            // load data values from that project.
            loadValues();

            // create a synchronization object.
            HierarchySynchronizer synch = new HierarchySynchronizer
                (projectRoot, processID, wbsFile, initials, fullCopyMode,
                 getPSPProperties(), getDataRepository());

            // start the synchronization process.
            if (parameters.containsKey(RUN_PARAM))
                synchronize(synch);
            else if (parameters.containsKey(SAVE_PERMS))
                savePermissionData();
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

            if (templateID != null && templateID.endsWith(MASTER_ROOT)) {
                projectRoot = key.path();
                processID = templateID.substring
                    (0, templateID.length() - MASTER_ROOT.length());
                isTeam = isMaster = true;
                return;
            }
             if (templateID != null && templateID.endsWith(TEAM_ROOT)) {
                projectRoot = key.path();
                processID = templateID.substring
                    (0, templateID.length() - TEAM_ROOT.length());
                isTeam = true;
                isMaster = false;
                return;
            }
            if (templateID != null && templateID.endsWith(INDIV_ROOT)) {
                projectRoot = key.path();
                processID =
                    templateID.substring(
                        0,
                        templateID.length() - INDIV_ROOT.length());
                isTeam = isMaster = false;
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
        teamDirectory = FilenameMapper.remap(teamDirectory);
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

        // locate the workflow file and the templates directory
        workflowFile = new File(teamDirectory, WORKFLOW_FILENAME);
        templatesDir = new File(teamDir.getParentFile().getParentFile(),
                "Templates");

        // look up the unique ID for this project.
        d = data.getSimpleValue(DataRepository.createDataName
                (projectRoot, PROJECT_ID_DATA_NAME));
        projectID = (d == null ? "" : d.format());

        if (isTeam) {
            initials = (isMaster ? HierarchySynchronizer.SYNC_MASTER
                    : HierarchySynchronizer.SYNC_TEAM);
        } else {
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
        syncTemplates(synch);
        if (synch.getChanges().isEmpty())
            printChanges(synch.getChanges());
        else if (isTeam == false
                && (synch.getTaskDeletions().isEmpty() == false ||
                    synch.getTaskCompletions().isEmpty() == false))
            printPermissionsPage(synch.getTaskDeletions(),
                    synch.getTaskCompletions());
        else
            printWaitPage();
    }



    /** Synchronize the hierarchy and display the results.
     */
    private void synchronize(HierarchySynchronizer synch)
        throws HierarchyAlterationException, IOException
    {
        if (!isTeam)
            loadPermissionData(synch);

        synch.setWhatIfMode(false);
        synch.sync();
        syncTemplates(synch);
        if (synch.isFollowOnWorkNeeded()) {
            saveChangeList(synch);
            parameters.remove(RUN_PARAM);
            parameters.remove(SAVE_PERMS);
            writeContents();

        } else {
            new AsyncExporter(projectRoot).start();
            printChanges(synch.getChanges());
        }
    }


    private void syncTemplates(HierarchySynchronizer sync) {
        if (isTeam) {
            String templateURI = "/" + processID + "-template.xml";
            TemplateSynchronizer tSync = new TemplateSynchronizer(projectRoot,
                    processID, projectID, templateURI, workflowFile,
                    templatesDir);
            tSync.setWhatIfMode(sync.isWhatIfMode());
            tSync.sync();
            sync.getChanges().addAll(tSync.getChanges());
        }
    }



    private void saveChangeList(HierarchySynchronizer synch) {
        ListData changes = (ListData) getDataContext().getSimpleValue(
                CHANGES_DATANAME);
        if (changes == null)
            changes = new ListData();

        for (Iterator i = synch.getChanges().iterator(); i.hasNext();)
            changes.add(String.valueOf(i.next()));

        getDataContext().putValue(CHANGES_DATANAME, changes);
    }



    /** Print a page that will ask the user for permission to make
     * destructive changes to their hierarchy.
     * @param taskDeletions a list of tasks we would like to delete
     * @param taskCompletions a list of tasks we would like to mark complete
     */
    private void printPermissionsPage(List taskDeletions, List taskCompletions) {
        out.print("<!-- SYNC-IS-NEEDED -->\n");
        out.print("<html><head>\n");
        out.print("<title>Synchronizing Work Breakdown Structure</title>\n");
        out.print("<style> .important { color: #800; font-weight: bold; }</style>\n");
        out.print("</head><body>\n");
        out.print("<h1>Synchronizing Work Breakdown Structure</h1>\n");
        out.print("<form action='sync.class' method='post'>\n");
        out.print("<input type='hidden' name='"+SAVE_PERMS+"' value='1'/>\n");
        out.print("<p>Several of the tasks in your hierarchy have been "
                + "deleted from the project's work breakdown structure, or "
                + "have been reassigned to other individuals.  These tasks "
                + "can be removed from your project automatically.</p>\n");

        if (!taskDeletions.isEmpty()) {
            out.print("<h2>Tasks to Delete</h2>\n");
            out.print("<p>You have not collected any actual metrics against "
                + "the following tasks, so the synchronization operation can "
                + "delete them from your hierarchy entirely.  If you wish to "
                + "keep any of these tasks, <b>uncheck the boxes</b> next to "
                + "them.  <span class='important'>Any tasks with checkmarks "
                + "next to them will be deleted when you press the OK "
                + "button.</span></p>");
            for (Iterator i = taskDeletions.iterator(); i.hasNext();) {
                String path = (String) i.next();
                printPermissionItem(DELETE_PREFIX, path);
            }
        }

        if (!taskCompletions.isEmpty()) {
            out.print("<h2>Tasks to Mark Complete</h2>");
            out.print("<p>Actual time and/or defects have been collected "
                + "against the following tasks, so they cannot be deleted "
                + "outright.  However, the synchronization operation can "
                + "mark the tasks complete so they no longer affect your "
                + "earned value.  If you wish to keep any of these tasks "
                + "open, <b>uncheck the boxes</b> next to them.  <span "
                + "class='important'>Any tasks with checkmarks next to them "
                + "will be marked complete when you press the OK "
                + "button.</span></p>");
            for (Iterator i = taskCompletions.iterator(); i.hasNext();) {
                String path = (String) i.next();
                printPermissionItem(COMPLETE_PREFIX, path);
            }
        }

        out.print("<p><input type='submit' name='OK' value='OK'/></p>");
        out.print("</form></body></html>\n");
    }

    private void printPermissionItem(String attr, String path) {
        path = XMLUtils.escapeAttribute(path);
        out.print("<input type='checkbox' checked='true' name='");
        out.print(attr);
        out.print(path);
        out.print("'/>&nbsp;");
        out.print(path);
        out.print("<br/>\n");
    }



    /** Parse data from the permissions form, and save it to the repository.
     */
    private void savePermissionData() {
        ListData delete = new ListData();
        ListData complete = new ListData();
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (e.getValue() == null  || "".equals(e.getValue()))
                continue;
            String name = (String) e.getKey();
            if (name.startsWith(DELETE_PREFIX))
                delete.add(name.substring(DELETE_PREFIX.length()));
            else if (name.startsWith(COMPLETE_PREFIX))
                complete.add(name.substring(COMPLETE_PREFIX.length()));
        }
        getDataRepository().putValue(getDataName(DELETE_DATANAME), delete);
        getDataRepository().putValue(getDataName(COMPLETE_DATANAME), complete);
        printWaitPage();
    }


    /** Load permission data from the repository, and configure the given
     * HierarchySynchronizer.
     */
    private void loadPermissionData(HierarchySynchronizer synch) {
        Object list = getDataRepository().getSimpleValue(
                getDataName(DELETE_DATANAME));
        if (list instanceof ListData)
            synch.setDeletionPermissions(((ListData) list).asList());
        list = getDataRepository().getSimpleValue(
                getDataName(COMPLETE_DATANAME));
        if (list instanceof ListData)
            synch.setCompletionPermissions(((ListData) list).asList());
    }

    private String getDataName(String name) {
        return DataRepository.createDataName(getPrefix(), name);
    }



    /** Print a page asking the user to wait. This page includes an
     * HTTP "refresh" instruction that will initiate the synchronization
     * operation.
     */
    private void printWaitPage() {
        out.print("<!-- SYNC-IS-NEEDED -->\n");
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
    private void printChanges(List changeList) {
        ListData oldChanges = (ListData) getDataContext().getSimpleValue(
                CHANGES_DATANAME);
        if (oldChanges != null) {
            changeList.addAll(0, oldChanges.asList());
            getDataContext().putValue(CHANGES_DATANAME, null);
        }

        out.print("<html><head><title>Synchronization Complete</title></head>");
        out.print("<body><h1>Synchronization Complete</h1>");
        if (changeList.isEmpty())
            out.print("<p>Your hierarchy is up to date - no changes "+
                      "were necessary.");
        else {
            out.print("<p>The following changes were made to your hierarchy:");
            out.print("<ul>");
            Iterator i = changeList.iterator();
            while (i.hasNext()) {
                out.print("<li>");
                out.print(HTMLUtils.escapeEntities(String.valueOf(i.next())));
            }
            out.print("</ul>");
        }
        out.print("</body></html>");
    }


    /** Asynchronously export the user's data.
     */
    private static class AsyncExporter extends Thread {

        private String projectRoot;

        public AsyncExporter(String projectRoot) {
            this.projectRoot = projectRoot;
        }

        public void run() {
            DashController.exportData(projectRoot);
        }

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
        if (isMaster)
            out.write("&isMaster");
        out.write("'></head><body></body></html>");
    }

    private static final String MASTER_ROOT = "/MasterRoot";
    private static final String TEAM_ROOT = "/TeamRoot";
    private static final String INDIV_ROOT = "/IndivRoot";
    private static final String TEAMDIR_DATA_NAME = "Team_Data_Directory";
    private static final String PROJECT_ID_DATA_NAME = "Project_ID";
    private static final String INITIALS_DATA_NAME = "Indiv_Initials";
    private static final String FULLCOPY_DATA_NAME = "Sync_Full_WBS";
    private static final String HIER_FILENAME = "projDump.xml";
    private static final String WORKFLOW_FILENAME = "workflowDump.xml";

    private static final String NOT_TEAM_PROJECT = "notTeamProject";
    private static final String TEAM_DIR_MISSING = "teamDirMissing";
    private static final String WBS_FILE_MISSING = "wbsFileMissing";
    private static final String WBS_FILE_INACCESSIBLE = "wbsFileInaccessible";
    private static final String INITIALS_MISSING = "initialsMissing";
    private static final String HIER_EDITOR_OPEN = "hierEditorOpen";

    private static final String RUN_PARAM = "run";
    private static final String SAVE_PERMS = "savePerms";
    private static final String COMPLETE_PREFIX = "COMPLETE:";
    private static final String DELETE_PREFIX = "DELETE:";
    private static final String COMPLETE_DATANAME = "complete_ //list";
    private static final String DELETE_DATANAME = "delete_ //list";
    private static final String CHANGES_DATANAME = "changes_ //list";

    private static final String READ_ONLY_MODE_ERR_MESSAGE =
        "You are currently running the dashboard in read-only mode, so " +
        "no changes can be made.";
}
