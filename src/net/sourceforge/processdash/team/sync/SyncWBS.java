// Copyright (C) 2002-2025 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.sync;
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.json.simple.JSONObject;

import net.sourceforge.processdash.BackgroundTaskManager;
import net.sourceforge.processdash.DashController;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.HierarchyAlterer.HierarchyAlterationException;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.process.ui.TriggerURI;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.team.setup.RepairImportInstruction;
import net.sourceforge.processdash.team.ui.SelectPspRollup;
import net.sourceforge.processdash.tool.bridge.bundle.BundledImportDirectory;
import net.sourceforge.processdash.tool.bridge.bundle.BundledWorkingDirectorySync;
import net.sourceforge.processdash.tool.bridge.bundle.CloudStorageUtils;
import net.sourceforge.processdash.tool.bridge.bundle.FileBundleID;
import net.sourceforge.processdash.tool.bridge.bundle.ForkTracker;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.impl.HttpAuthenticator;
import net.sourceforge.processdash.tool.bridge.impl.TeamDataDirStrategy;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager.MappingException;
import net.sourceforge.processdash.ui.Browser;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.HttpException;
import net.sourceforge.processdash.util.StringUtils;
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
public class SyncWBS extends TinyCGIBase implements TeamDataConstants {

    /** The hierarchy path to the root of the enclosing team project */
    private String projectRoot;
    /** The unique ID assigned to the enclosing team project */
    private String projectID;
    /** The processID used by the enclosing team project */
    private String processID;
    /** The location of the wbs dump file */
    private URL wbsLocation;
    /** The location of the workflow dump file */
    private URL workflowLocation;
    /** Whether the WBS is in a bundled directory */
    private boolean wbsIsBundled;
    /** The working directory if this WBS uses sync bundle storage */
    private BundledWorkingDirectorySync syncBundleDir;
    /** The directory where the dashboard is storing data files */
    private String dataDir;
    /** The initials of the current team member, if applicable */
    private String initials;
    /** True if this is the team rollup side of the project */
    private boolean isTeam;
    /** True if this is a master project rollup */
    private boolean isMaster;
    /** True if this is a personal project */
    @SuppressWarnings("unused")
    private boolean isPersonal;
    /** true if this is an old-style individual project that needs to be
     * upgraded to the new-style format */
    private boolean migrationNeeded;
    /** true if this is an individual project that needs to be converted
     * to a different metrics collection framework */
    private boolean conversionNeeded;
    /** true if we can delete/complete tasks without asking for permission */
    private boolean noDeletePermissionNeeded;
    /** true if the user wants us to copy all software component and document
     * nodes in the WBS, even if they aren't assigned to them */
    private boolean fullCopyMode;
    /** The default to date subset to use for new PSP tasks */
    private String pspToDateSubset;
    /** Whether the user should be prompted to select the to date subset to
     * use for new PSP tasks */
    private boolean promptForPspToDateSubset;
    /** An HTML element that can be used to select a PSP subset */
    private String pspSubsetSelector;
    /** Flag determining whether we should log detailed debugging info */
    private String enableSyncLogging;


    public SyncWBS() {
        charset = "UTF-8";
    }


    protected void doPost() throws IOException {
        parseFormData();
        super.doPost();
    }



    public void writeContents() throws IOException {
        try {
            if (Settings.isReadOnly()) {
                if (Settings.isTeamMode())
                    DataImporter.refreshPrefix("/");
                signalError("generalError", READ_ONLY_MODE_ERR_MESSAGE);
            }

            // locate the root of the included project.
            findProject();

            // check for dump callback mode
            if (maybeHandleDumpCallback(projectRoot))
                return;

            // load data values from that project.
            loadValues();

            // possibly redirect to the migration/conversion page.
            if (migrationNeeded || conversionNeeded) {
                printMigrationRedirect();
                return;
            }

            // synchronize data with external systems if needed
            maybePerformExtRefresh();

            // create a synchronization object.
            HierarchySynchronizer synch = new HierarchySynchronizer
                (projectRoot, processID, wbsLocation, workflowLocation,
                 initials, getOwner(), fullCopyMode, pspToDateSubset,
                 promptForPspToDateSubset, syncBundleDir, dataDir,
                 getPSPProperties(), getDataRepository());

            // double-check individual initials
            if (checkForMismatchedIndivInitials(synch))
                return;

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
        } catch (HttpException.Forbidden f) {
            showErrorPage("serverForbidden", HttpAuthenticator.getLastUsername());
        } catch (HierarchyAlterationException h) {
            showErrorPage("generalError", h.getMessage());
        } catch (IOException ioe) {
            if (wbsIsBundled)
                ioe.printStackTrace();
            showErrorPage("generalError", ioe.getMessage());
        }

        // certain operations (such as saving the WBS) trigger a sync to run
        // in the background.  For various reasons, that sync may require user
        // involvement to finish all the work needed.  After the code above
        // has finished as much work as possible, ask the SyncScanner to alert
        // the user if their involvement is needed to finish.
        if (projectRoot != null && parameters.containsKey(RUN_PARAM)
                && parameters.containsKey(BACKGROUND_PARAM))
            SyncScanner.requestScan(projectRoot);
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
                processID = templateID.substring
                    (0, templateID.length() - INDIV_ROOT.length());
                isTeam = isMaster = false;
                return;
            }
            if (templateID != null && templateID.endsWith(INDIV2_ROOT)) {
                projectRoot = key.path();
                processID = templateID.substring
                    (0, templateID.length() - INDIV2_ROOT.length());
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

        DataRepository data = getDataRepository();

        wbsLocation = getWBSLocation(data);
        workflowLocation = getWorkflowLocation(wbsLocation);
        dataDir = ((ProcessDashboard) getDashboardContext()).getDirectory();

        // look up the unique ID for this project.
        SimpleData d = data.getSimpleValue(DataRepository.createDataName
                (projectRoot, TeamDataConstants.PROJECT_ID));
        projectID = (d == null ? "" : d.format());
        if (!StringUtils.hasValue(projectID))
            signalError(NOT_TEAM_PROJECT);

        if (isTeam) {
            initials = (isMaster ? HierarchySynchronizer.SYNC_MASTER
                    : HierarchySynchronizer.SYNC_TEAM);
            migrationNeeded = conversionNeeded = isPersonal = false;
            pspToDateSubset = pspSubsetSelector = null;
            noDeletePermissionNeeded = true;
            promptForPspToDateSubset = false;

            d = data.getSimpleValue(DataRepository.createDataName
                (projectRoot, TeamDataConstants.SYNC_ROOT_ONLY));
            fullCopyMode = (d == null || !d.test());

            if (shouldRepairTeamImport())
                RepairImportInstruction.maybeRepairForTeam(getDataContext());

        } else {
            String initialsDataName = DataRepository.createDataName(
                projectRoot, TeamDataConstants.INDIV_INITIALS);

            // change the initials of the current team member, if requested.
            String newInitials = (String) parameters.get(CHANGE_INITIALS);
            if (StringUtils.hasValue(newInitials)) {
                if (UNLISTED_INITIALS.equals(newInitials)) {
                    signalError(UNLISTED_MEMBER);
                } else if (!newInitials.startsWith("-")) {
                    data.putValue(initialsDataName, StringData
                            .create(newInitials.trim()));
                }
            }

            // get the initials of the current team member.
            d = data.getSimpleValue(initialsDataName);
            if (d == null || !d.test() ||
                "tttt".equals(initials = d.format().trim()))
                signalError(INITIALS_MISSING);
 
            d = data.getSimpleValue(DataRepository.createDataName
                (projectRoot, TeamDataConstants.PERSONAL_PROJECT_FLAG));
            isPersonal = (d != null && d.test());

            d = data.getSimpleValue(DataRepository.createDataName
                (projectRoot, "Sync_Require_Delete_Permission"));
            noDeletePermissionNeeded = (d == null || !d.test()
                    || parameters.containsKey("force"));

            d = data.getSimpleValue(DataRepository.createDataName
                                    (projectRoot, MIGRATE_DATA_NAME));
            migrationNeeded = (d != null && d.test());

            d = data.getSimpleValue(DataRepository.createDataName
                                    (projectRoot, CONVERT_DATA_NAME));
            conversionNeeded = (d != null && d.test());

            RepairImportInstruction.maybeRepairForIndividual(getDataContext());

            // check to see if the user has registered a To Date rollup that
            // they wish to use for new PSP tasks in this team project
            d = data.getSimpleValue(DataRepository.createDataName
                (projectRoot, HierarchySynchronizer.PSP_SUBSET));
            pspToDateSubset = (d != null && d.test() ? d.format() : null);
            if (pspToDateSubset != null) {
                // make certain their selection points to a valid rollup.
                d = data.getSimpleValue(DataRepository.createDataName(
                    pspToDateSubset, "PSP Rollup Tag"));
                if (d == null || !d.test())
                    pspToDateSubset = null;
            }

            // check to see if the user wants to be prompted for the PSP rollup
            // that should be used for each new PSP task
            d = data.getSimpleValue(PROMPT_FOR_PSP_SUBSET);
            if (d != null && d.test()) {
                // if they want to be prompted, double-check to ensure that
                // multiple PSP rollups are still present in this dashboard
                pspSubsetSelector = SelectPspRollup.getRollupSelector(
                    getDataContext(), PSP_SUBSET_PREFIX + "#####",
                    pspToDateSubset);
                promptForPspToDateSubset = (pspSubsetSelector != null);
            } else {
                pspSubsetSelector = null;
                promptForPspToDateSubset = false;
            }

            // we no longer check to see if users want us to perform a full wbs
            // sync; this functionality is obsolete and only causes problems now.
            fullCopyMode = false;
        }

        // check to see if sync logging is desired
        enableSyncLogging = Settings.getVal("syncWBS.enableLogging", null);
    }



    private URL getWBSLocation(DataRepository data) throws IOException {
        // retrieve the latest wbs.xml file for the project
        ImportDirectory wbsDir = getWBSDirectory(
            data.getSubcontext(projectRoot));
        wbsIsBundled = BundledImportDirectory.isBundled(wbsDir, null);
        checkSyncBundleMerge(wbsDir);
        File wbsFile = new File(wbsDir.getDirectory(), HIER_FILENAME);

        // if the file does not exist, or cannot be read, abort
        if (!wbsFile.isFile())
            signalError(WBS_FILE_MISSING);
        if (!wbsFile.canRead())
            signalError(WBS_FILE_INACCESSIBLE + "&wbsFile", wbsFile.toString());

        // return the URL of the wbs.xml file
        return wbsFile.toURI().toURL();
    }

    private ImportDirectory getWBSDirectory(DataContext data)
            throws TinyCGIException {
        // look in the repository for a team data directory URL
        SimpleData d = data.getSimpleValue(TEAM_DATA_DIRECTORY_URL);
        String serverUrl = (d == null ? null : d.format().trim());
        if (StringUtils.hasValue(serverUrl)) {
            try {
                // If a server URL is configured, try connecting
                ImportDirectory dir = ImportDirectoryFactory.getInstance()
                        .get(serverUrl);

                // ensure we have the latest files
                dir.validate();

                // if the server URL has changed, store the new URL
                String newUrl = dir.getRemoteLocation();
                if (newUrl != null && !newUrl.equals(serverUrl)
                        && TeamServerSelector.isUrlFormat(newUrl))
                    data.putValue(TEAM_DATA_DIRECTORY_URL,
                        StringData.create(newUrl));

                // return the directory containing the refreshed server files
                return dir;

            } catch (Exception e) {
                // if we failed to reach the configured server, abort
                signalError(SERVER_UNAVAILABLE, serverUrl);
            }
        }

        // read the team data directory path from the repository
        d = data.getSimpleValue(TEAM_DATA_DIRECTORY);
        String teamDirectoryPath = (d == null ? null : d.format().trim());
        if (!StringUtils.hasValue(teamDirectoryPath)
                || "Enter network directory path".equals(teamDirectoryPath))
            signalError(TEAM_DIR_MISSING);

        // look up an ImportDirectory for the given location. This will check
        // for external remappings
        ImportDirectory dir = ImportDirectoryFactory.getInstance()
                .get(teamDirectoryPath);

        // ensure we have the latest files
        boolean isValid;
        try {
            dir.validate();
            isValid = true;
        } catch (Exception e) {
            isValid = false;
        }

        // if the team data directory was not found and is using [Shared Folder]
        // encoding, try searching for it under the given folder root.
        if (!isValid && FolderMappingManager.isEncodedPath(teamDirectoryPath)
                && Settings.getBool("syncWBS.searchForDir", true)) {
            String cloudDirPath = null;
            try {
                // ask the folder mapping manager to search for the directory
                FolderMappingManager mgr = FolderMappingManager.getInstance();
                File newDir = mgr.searchForDirectory(teamDirectoryPath, 2);

                // if a dir is found, build an ImportDirectory
                ImportDirectory nid = ImportDirectoryFactory.getInstance()
                        .get(newDir.getAbsolutePath());

                // if the dir is using cloud storage, save the cloud dir path
                if (CloudStorageUtils.isCloudStorage(nid))
                    cloudDirPath = nid.getDescription();

                // validate the directory to ensure readability
                nid.validate();

                // if we successfully validate a newly found directory, adopt
                // it as the official team directory for this project.
                File newTeamDir = newDir.getParentFile().getParentFile();
                String newPath = mgr.encodePath(newTeamDir.getAbsolutePath());
                data.putValue(TEAM_DIRECTORY, StringData.create(newPath));
                data.putValue(TEAM_DIRECTORY_UNC, null);
                dir = nid;
                isValid = true;

            } catch (MappingException me) {
                // display a message if shared folder errors are encountered
                signalError(me.asQuery().substring(1) + "&SYNC-IS-NEEDED");

            } catch (Exception e) {
                // if a replacement directory could not be found and validated,
                // abort. Signal cloud storage errors here, other errors below
                if (cloudDirPath != null)
                    signalError("cloudFolderCannotRead&cloudStorageDir", cloudDirPath);
            }
        }

        if (!isValid) {
            signalError(TEAM_DIR_UNAVAILABLE, teamDirectoryPath);
        }

        // if the directory has been migrated to a server, update config
        String newUrl = dir.getRemoteLocation();
        if (newUrl != null) {
            data.putValue(TEAM_DATA_DIRECTORY_URL, StringData.create(newUrl));
            data.putValue(TEAM_DIRECTORY, null);
            data.putValue(TEAM_DIRECTORY_UNC, null);
        }

        // return the directory containing the refreshed server files
        return dir;
    }

    private void checkSyncBundleMerge(ImportDirectory importDir)
            throws IOException {
        // if this is not a sync bundle directory, do nothing
        syncBundleDir = BundledImportDirectory.getSyncBundleDir(importDir);
        if (syncBundleDir == null)
            return;

        // abort if WBS bundle merging is disabled
        if (Boolean.getBoolean("teamdash.wbs.disableBundleMerge"))
            return;

        // if we're currently running a sync operation, do nothing
        if (parameters.containsKey(RUN_PARAM))
            return;

        // check bundleMerge param. If it is "skip", do nothing
        String param = getParameter(MERGE_PARAM);
        if (MERGE_SKIP.equals(param))
            return;

        // if the bundleMerge param is "run", perform a sync bundle merge
        if (RUN_PARAM.equals(param)) {
            runSyncBundleMerge();
            return;
        }

        // the bundleMerge param is missing; we should check to see if a merge
        // is needed.

        // if the WBS Editor is currently open, do nothing. (We cannot launch
        // a merge operation while the WBS Editor has the directory locked.)
        if (wbsEditorIsOpen(importDir))
            return;

        // if a merge operation is needed, display a page asking the user if
        // they are ready to launch one.
        ForkTracker forkTracker = syncBundleDir.getForkTracker();
        if (isProjDumpNeeded(importDir) || isForkMergeNeeded(forkTracker))
            signalError(MERGE_CONFIRM);
    }

    private boolean wbsEditorIsOpen(ImportDirectory dir) {
        // the presence of a process lock file means the WBS Editor is open.
        // that would prevent a dump-and-exit process from doing any work.
        File processLockFile = new File(dir.getDirectory(),
                TeamDataDirStrategy.LOCK_FILE_NAME);
        return processLockFile.exists();
    }

    private boolean isProjDumpNeeded(ImportDirectory dir) throws IOException {
        // Resd the user-settings.ini file from the project
        Properties p = getUserSettings(dir);

        // return the status of the projDumpNeeded flag
        return "true".equals(p.getProperty("projDumpNeeded"));
    }

    private Properties getUserSettings(ImportDirectory dir) throws IOException {
        Properties p = new Properties();
        File settings = new File(dir.getDirectory(), "user-settings.ini");
        if (!settings.isFile())
            return p;

        // read the contents of the settings file
        InputStream in = new BufferedInputStream(new FileInputStream(settings));
        p.load(in);
        in.close();

        // return the properties
        return p;
    }

    private boolean isForkMergeNeeded(ForkTracker forkTracker)
            throws IOException {
        // retrieve the list of forks for the WBS bundle
        List<FileBundleID> wbsForks = forkTracker.getForkedBundles()
                .get(WBS_BUNDLE_NAME);
        if (wbsForks == null || wbsForks.size() < 2)
            return false;

        // a merge is needed if our ref disagrees with the accepted team value
        FileBundleID selfRef = forkTracker.getSelfHeadRefs()
                .getHeadRef(WBS_BUNDLE_NAME);
        return wbsForks.indexOf(selfRef) > 0;
    }
    private static final String WBS_BUNDLE_NAME = "wbs";

    private void runSyncBundleMerge() throws IOException {
        // prepare to collect parameters from the dump callback
        Future<Map> future = getDumpCallbackParams(projectRoot);

        // make a subrequest to start a dump-and-exit run of the WBS
        String dumpUri = resolveRelativeURI(WBS_MERGE_DUMP_URI);
        getRequest(dumpUri, false);

        // wait for the dump-and-exit run to finish, and get the result
        int syncDelay = Settings.getInt("syncWBS.mergeTimeout", 60);
        Map result = null;
        try {
            result = future.get(syncDelay, TimeUnit.SECONDS);
        } catch (Exception e) {
        }

        // if the operation timed out, result will be null
        if (result == null)
            result = Collections.singletonMap("dumpResult", "timeout");

        // get the result code from the bundle merge operation and take action
        String dumpResult = (String) result.get("dumpResult");
        String mergeErr = (String) result.get("mergeError");
        if ("mergeConflict".equals(mergeErr) || "mergeError".equals(mergeErr)) {
            // display an error page for merge conflicts / errors
            signalError(mergeErr);

        } else if (!"saved".equals(dumpResult)) {
            // process completed without a successful merge. Print a debug
            // message and tell user about the error
            System.out.println("Sync bundle merge completed without success. "
                    + "Result = " + dumpResult //
                    + ", exit code = " + result.get("exitCode"));
            signalError("mergeError");
        }
    }


    private URL getWorkflowLocation(URL wbsUrl) {
        if (wbsUrl != null) {
            try {
                String wbsUrlStr = wbsUrl.toString();
                String workflowUrlStr = StringUtils.findAndReplace(wbsUrlStr,
                    HIER_FILENAME, WORKFLOW_FILENAME);
                if (!workflowUrlStr.equals(wbsUrlStr))
                    return new URL(workflowUrlStr);
            } catch (MalformedURLException e) {}
        }
        return null;
    }

    private boolean shouldRepairTeamImport() {
        SimpleData d = getDataContext().getSimpleValue(
            DISABLE_TEAM_IMPORT_REPAIR_DATA_NAME);
        if (d != null && d.test())
            return false;

        return !Settings.getBool(DISABLE_TEAM_IMPORT_REPAIR_SETTING, false);
    }

    private void maybePerformExtRefresh() {
        try {
            String uri = WebServer.urlEncodePath(projectRoot)
                    + "//team/toolsB/runExtRefresh?timeout=10";
            getTinyWebServer().getRequest(uri, true);
        } catch (Exception e) {
        }
    }

    private boolean checkForMismatchedIndivInitials(HierarchySynchronizer synch)
            throws TinyCGIException {
        Map<String, String> mismatch = synch.checkIndivInitials();
        if (mismatch == null)
            return false;
        else if (mismatch.isEmpty())
            signalError(EMPTY_TEAM);

        StringBuilder select = new StringBuilder();
        select.append("<select name='" + CHANGE_INITIALS + "'>");
        select.append("<option value='-'>Select your name...</option>");
        for (Map.Entry<String, String> indiv : mismatch.entrySet()) {
            String initialsHtml = HTMLUtils.escapeEntities(indiv.getKey());
            String nameHtml = HTMLUtils.escapeEntities(indiv.getValue());
            select.append("<option value='").append(initialsHtml).append("'>")
                    .append(nameHtml).append("</option>");
        }
        select.append("<option value='" + UNLISTED_INITIALS
                + "'>My name is not listed...</option>");
        select.append("</select>");

        String dataName = DataRepository.createDataName(projectRoot,
            "setup//Member_List");
        getDataRepository().putValue(dataName,
            StringData.create(select.toString()));

        out.write("<!-- SYNC-IS-NEEDED -->");
        out.write("<html><head>");
        printRedirectInstruction("syncFixInitials.shtm", 0);
        out.write("</head><body></body></html>");
        return true;
    }



    /** Redirect to the team project migration page */
    private void printMigrationRedirect() {
        out.write("<!-- SYNC-IS-NEEDED -->");
        out.write("<html><head>");
        printRedirectInstruction("migrate", 0);
        out.write("</head><body></body></html>");
    }



    /** Check to see if a synchronization operation is needed. If so,
     * display a "please wait" page, then initiate the operation.
     */
    private void maybeSynchronize(HierarchySynchronizer synch)
        throws HierarchyAlterationException, IOException
    {
        synch.setWhatIfMode(true);
        if (parameters.containsKey(TriggerURI.IS_TRIGGERING)
                || parameters.containsKey(BRIEF_PARAM))
            synch.setWhatIfBrief(true);
        else if (enableSyncLogging != null)
            synch.enableDebugLogging();

        synch.sync();

        // if only miscellaneous changes were made, start the sync over in "run"
        // mode to apply them. Let that operation display the final output.
        if (synch.isMiscChangeOnly()) {
            parameters.put(RUN_PARAM, "t");
            writeContents();
            return;
        }

        if (synch.getDebugLogInfo() != null)
            maybeDumpDebugLog(synch);

        // if we don't need confirmation to complete/delete tasks, discard
        // notes about any permissions that might be needed.
        if (noDeletePermissionNeeded) {
            synch.getTaskDeletions().clear();
            synch.getTaskCompletions().clear();
        }

        if (isJsonRequest()) {
            printJsonResponse(synch.getChanges().isEmpty(), false);

        } else if (synch.getChanges().isEmpty()) {
            printChanges(synch.getChanges());

        } else if (isTeam == false
                && (synch.getTaskDeletions().isEmpty() == false ||
                    synch.getTaskCompletions().isEmpty() == false ||
                    synch.getPspTasksNeedingSubsetPrompt().isEmpty() == false)) {

            printPermissionsPage(synch.getTaskDeletions(),
                    synch.getTaskCompletions(),
                    synch.getPspTasksNeedingSubsetPrompt());

        } else {
            printWaitPage();
        }
    }



    /** Synchronize the hierarchy and display the results.
     */
    private void synchronize(HierarchySynchronizer synch)
        throws HierarchyAlterationException, IOException
    {
        // ensure that the hierarchy editor is not open.
        if (DashController.isHierarchyEditorOpen())
            signalError(HIER_EDITOR_OPEN);

        if (!isTeam)
            loadPermissionData(synch);

        synch.setWhatIfMode(false);
        synch.setBackgroundMode(parameters.containsKey(BACKGROUND_PARAM));
        synch.sync();

        if (synch.isFollowOnWorkNeeded()) {
            saveChangeList(synch);
            parameters.remove(RUN_PARAM);
            parameters.remove(SAVE_PERMS);
            writeContents();

        } else {
            if (printChanges(synch.getChanges())
                    && !parameters.containsKey("noExport")
                    && !Settings.getBool("export.disableAutoExport", false))
                startBackgroundExport(projectRoot);

            UserNotificationManager.getInstance().removeNotification(
                    SyncScanner.getScanTaskID(projectRoot));
        }

        if (isTeam)
            DataImporter.refreshPrefix("/");
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
     * @param pspTasksNeedingSubset a list of PSP tasks whose To Date subset
     *    needs configuring
     */
    private void printPermissionsPage(List taskDeletions, List taskCompletions,
            List pspTasksNeedingSubset) {
        out.print("<!-- SYNC-IS-NEEDED -->\n");
        out.print("<html><head>\n");
        out.print("<title>Synchronizing Work Breakdown Structure</title>\n");
        out.print("<style> .important { color: #800; font-weight: bold; }</style>\n");
        out.print("</head><body>\n");
        out.print("<h1>Synchronizing Work Breakdown Structure</h1>\n");
        out.print("<form action='sync.class' method='post'>\n");
        out.print("<input type='hidden' name='"+SAVE_PERMS+"' value='1'/>\n");
        out.print(MERGE_SKIP_HIDDEN);

        if (!taskDeletions.isEmpty() || !taskCompletions.isEmpty()) {
            out.print("<p>Several of the tasks in your personal plan have been "
                + "deleted from the project's work breakdown structure, "
                + "have been reassigned to other individuals, or have "
                + "been deferred until future iterations.  These tasks "
                + "can be removed from your project automatically.</p>\n");
        }

        if (!taskDeletions.isEmpty()) {
            out.print("<h2>Tasks to Delete</h2>\n");
            out.print("<p>You have not collected any actual metrics against "
                + "the following tasks, so the synchronization operation can "
                + "delete them from your personal plan entirely.  If you wish to "
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

        if (!pspTasksNeedingSubset.isEmpty()) {
            out.print("<h2>PSP Tasks to Configure</h2>");
            out.print("<p>The following PSP tasks have been added to your "
                + "personal plan.  Please select the \"To Date\" rollup that "
                + "each task should use as the basis for the \"Plan\" column "
                + "and PROBE calculations:</p>\n"
                + "<div style='margin-left:1cm'>\n");
            for (int i = 0;  i < pspTasksNeedingSubset.size();  i++) {
                String path = (String) pspTasksNeedingSubset.get(i);
                printSubsetItem(i, path);
            }
            out.print("</div>\n");
        }

        out.print("<p><input type='submit' name='OK' value='OK'/></p>");
        out.print("</form></body></html>\n");
    }

    private void printPermissionItem(String attr, String path) {
        path = HTMLUtils.escapeEntities(path);
        out.print("<input type=\"checkbox\" checked=\"true\" name=\"");
        out.print(attr);
        out.print(path);
        out.print("\"/>&nbsp;");
        out.print(path);
        out.print("<br/>\n");
    }

    private void printSubsetItem(int i, String path) {
        path = HTMLUtils.escapeEntities(path);
        String selector = StringUtils.findAndReplace(pspSubsetSelector,
            "#####", path);
        out.print(path);
        out.print("&nbsp;&nbsp;&nbsp;");
        out.print(selector);
        out.print("<br/>\n");
    }



    /** Parse data from the permissions form, and save it to the repository.
     */
    private void savePermissionData() {
        ListData delete = new ListData();
        ListData complete = new ListData();
        ListData pspSubsets = new ListData();
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (e.getValue() == null  || "".equals(e.getValue()))
                continue;
            String name = (String) e.getKey();
            if (name.endsWith("_ALL"))
                ;
            else if (name.startsWith(DELETE_PREFIX))
                delete.add(name.substring(DELETE_PREFIX.length()));
            else if (name.startsWith(COMPLETE_PREFIX))
                complete.add(name.substring(COMPLETE_PREFIX.length()));
            else if (name.startsWith(PSP_SUBSET_PREFIX)) {
                pspSubsets.add(name.substring(PSP_SUBSET_PREFIX.length()));
                pspSubsets.add(e.getValue());
            }
        }
        getDataRepository().putValue(getDataName(DELETE_DATANAME), delete);
        getDataRepository().putValue(getDataName(COMPLETE_DATANAME), complete);
        getDataRepository().putValue(getDataName(PSP_SUBSET_DATANAME), pspSubsets);
        printWaitPage();
    }


    /** Load permission data from the repository, and configure the given
     * HierarchySynchronizer.
     */
    private void loadPermissionData(HierarchySynchronizer synch) {
        Object list;
        if (noDeletePermissionNeeded) {
            // if we don't need user confirmation to delete/complete anything,
            // record that decision
            synch.setDeletionPermissions(null);
            synch.setCompletionPermissions(null);

        } else {
            list = getDataRepository().getSimpleValue(
                    getDataName(DELETE_DATANAME));
            if (list instanceof ListData)
                synch.setDeletionPermissions(((ListData) list).asList());
            list = getDataRepository().getSimpleValue(
                    getDataName(COMPLETE_DATANAME));
            if (list instanceof ListData)
                synch.setCompletionPermissions(((ListData) list).asList());
        }
        list = getDataRepository().getSimpleValue(
            getDataName(PSP_SUBSET_DATANAME));
        if (list instanceof ListData)
            synch.setPspSubsetSelections(listToMap((ListData) list));
    }

    private Map listToMap(ListData l) {
        Map result = new HashMap();
        for (int i = 1;  i < l.size();  i += 2) {
            Object key = l.get(i - 1);
            Object value = l.get(i);
            result.put(key, value);
        }
        return result;
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
        printRedirectInstruction("sync.class?run", 1);
        out.print("</head>");
        out.print("<body><h1>Synchronizing...</h1>");
        out.print("Please wait.");
        out.print("</body></html>");
    }



    private void printJsonResponse(boolean upToDate, boolean isMiscOnly) {
        JSONObject response = new JSONObject();

        if (upToDate || isMiscOnly) {
            JSONObject message = new JSONObject();
            message.put("title", "Synchronization Complete");
            message.put("body", getUpToDateMessage(isMiscOnly));
            response.put("message", message);

        } else {
            String uri = (String) env.get("SCRIPT_PATH");
            String url = Browser.mapURL(uri);
            response.put("redirect", url);
        }

        response.put("stat", "ok");
        out.print(response.toString());
    }

    private String getUpToDateMessage(boolean isMiscOnly) {
        return (isTeam || isMaster ? "Project data" : "Your personal plan")
                + " is up to date - " + (isMiscOnly ? "only minor" : "no")
                + " changes were necessary.";
    }


    /** Print a list of changes made by a synchronization operation.
     */
    private boolean printChanges(List changeList) {
        ListData oldChanges = (ListData) getDataContext().getSimpleValue(
                CHANGES_DATANAME);
        if (oldChanges != null) {
            changeList.addAll(0, oldChanges.asList());
            getDataContext().putValue(CHANGES_DATANAME, null);
        }

        boolean isMiscOnly = HierarchySynchronizer.isMiscChangeOnly(changeList);
        if (isMiscOnly && isJsonRequest()) {
            printJsonResponse(true, true);
            return true;
        }

        out.print("<html><head><title>Synchronization Complete</title></head>");
        out.print("<body><h1>Synchronization Complete</h1>");
        if (changeList.isEmpty() || isMiscOnly) {
            String message = getUpToDateMessage(isMiscOnly);
            out.print("<p>" + message + "</p>");

            if (parameters.containsKey(TriggerURI.IS_TRIGGERING)) {
                out.print(TriggerURI.NULL_DOCUMENT_MARKER);
                JOptionPane.showMessageDialog(getParentComponent(),
                        message, "Synchronization Complete",
                        JOptionPane.PLAIN_MESSAGE);
            }

        } else {
            out.print("<p>The following changes were made");
            if (!isTeam && !isMaster)
                out.print(" to your personal plan");
            out.print(":</p>\n<ul>");
            Iterator i = changeList.iterator();
            while (i.hasNext()) {
                out.print("<li>");
                out.print(HTMLUtils.escapeEntities(String.valueOf(i.next())));
            }
            out.print("</ul>");
        }
        out.print("</body></html>");

        return !changeList.isEmpty();
    }

    private Component getParentComponent() {
        Object result = getDashboardContext();
        if (result instanceof Component)
            return (Component) result;
        else
            return null;
    }


    private void maybeDumpDebugLog(HierarchySynchronizer synch) {
        String prefix = null;

        if ("always".equalsIgnoreCase(enableSyncLogging)) {
            prefix = "sync-debug-";

        } else if ("forDelete".equalsIgnoreCase(enableSyncLogging)) {
            int delTaskCount = synch.getTaskDeletions().size()
                    + synch.getTaskCompletions().size();
            if (delTaskCount > 5)
                prefix = "sync-delete-";
        }

        if (prefix != null) {
            try {
                dumpDebugLog(synch, prefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    private void dumpDebugLog(HierarchySynchronizer synch, String dumpPrefix)
            throws Exception {
        // determine the location of the data directory
        File dataDir = null;
        String settingsFile = DashController.getSettingsFileName();
        if (StringUtils.hasValue(settingsFile))
            dataDir = new File(settingsFile).getParentFile();
        if (dataDir == null || !dataDir.isDirectory())
            dataDir = new File(".");

        // save debug info to the data directory
        List<String> debugInfo = synch.getDebugLogInfo();
        File debugInfoFile = new File(dataDir, "syncDebugInfo.dat");
        if (debugInfo != null) {
            PrintWriter out = new PrintWriter(debugInfoFile);
            for (String line : debugInfo)
                out.println(line);
            out.close();
        }

        // copy the wbs dump file into the data directory
        InputStream wbsIn = wbsLocation.openStream();
        File wbsDumpFile = new File(dataDir, "wbsDump.dat");
        FileUtils.copyFile(wbsIn, wbsDumpFile);
        wbsIn.close();

        // copy the wbs xml file into the data directory
        wbsIn = new URL(wbsLocation, "wbs.xml").openStream();
        File wbsXmlFile = new File(dataDir, "wbsXml.dat");
        FileUtils.copyFile(wbsIn, wbsXmlFile);
        wbsIn.close();

        // copy the modified wbs xml data into the data directory
        File wbsModFile = new File(dataDir, "wbsDumpPruned.dat");
        OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(
                wbsModFile), "UTF-8");
        w.write(XMLUtils.getAsText(synch.getProjectXML()));
        w.close();

        // perform a backup of the data directory (which will include the
        // two files created above)
        File backup = DashController.backupData();
        File backupDir = new File(dataDir, "backup");
        if (!backupDir.isDirectory())
            backupDir = dataDir;
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-HHmmss'.zip'");
        String newFilename = dumpPrefix + fmt.format(new Date());
        File saved = new File(backupDir, newFilename);
        FileUtils.copyFile(backup, saved);

        // cleanup the temporary dump files we created
        debugInfoFile.delete();
        wbsDumpFile.delete();
        wbsXmlFile.delete();
        wbsModFile.delete();

        // cleanup older dump files
        Date cutoffDate = new Date(System.currentTimeMillis() - DUMP_CLEANUP_AGE);
        String oldName = dumpPrefix + fmt.format(cutoffDate);
        File[] files = backupDir.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (name.startsWith(dumpPrefix) && name.compareTo(oldName) < 0)
                    f.delete();
            }
        }
    }
    private static final long DUMP_CLEANUP_AGE = 14L /*days*/ * 24 /*hours*/
            * 60 /*minutes*/ * 60 /*seconds*/ * 1000 /*milliseconds*/;



    /** Asynchronously export the user's data.
     */
    private static class AsyncExporter implements Runnable {

        private String projectRoot;

        public AsyncExporter(String projectRoot) {
            this.projectRoot = projectRoot;
        }

        public void run() {
            DashController.exportData(projectRoot);
        }

        public String toString() {
            return "AsyncExporter:" + projectRoot;
        }

    }

    public static void startBackgroundExport(String projectRoot) {
        BackgroundTaskManager.getInstance().addTask(
                new AsyncExporter(projectRoot));
    }



    private static class WbsDumpTracker implements Future<Map> {

        private Map params = null;

        public synchronized void callbackReceived(Map params) {
            this.params = new HashMap(params);
            notifyAll();
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public synchronized boolean isDone() {
            return params != null;
        }

        public Map get() {
            return get(0, TimeUnit.SECONDS);
        }

        public synchronized Map get(long timeout, TimeUnit unit) {
            if (params == null) {
                try {
                    unit.timedWait(this, timeout);
                } catch (Exception e) {
                }
            }
            return params;
        }
    }

    private static final Map<String, WbsDumpTracker> DUMP_TRACKERS = //
            Collections.synchronizedMap(new HashMap());

    private boolean maybeHandleDumpCallback(String path) {
        boolean isDumpCallback = parameters.containsKey("dumpResult");

        if (isDumpCallback && path != null) {
            WbsDumpTracker tracker = DUMP_TRACKERS.remove(path);
            if (tracker != null)
                tracker.callbackReceived(parameters);
        }

        return isDumpCallback;
    }

    private Future<Map> getDumpCallbackParams(String path) {
        WbsDumpTracker tracker;
        synchronized (DUMP_TRACKERS) {
            tracker = DUMP_TRACKERS.get(path);
            if (tracker == null) {
                tracker = new WbsDumpTracker();
                DUMP_TRACKERS.put(path, tracker);
            }
        }
        return tracker;
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
        if (reason.contains(WBS_FILE_MISSING)) {
            // if the team data area was found but the WBS file does not exist,
            // this generally means that the team hasn't opened the WBS Editor
            // and performed any planning. In the past, a special message was
            // displayed for this case; but in the interest of simplicity there
            // is no need to confuse the user with this detail. Just let them
            // know that no changes are needed at this time.
            printChanges(Collections.EMPTY_LIST);
            return;
        }

        out.write("<html><head>");
        StringBuffer uri = new StringBuffer("syncError.shtm?").append(reason);
        if (value != null)
            uri.append("=").append(HTMLUtils.urlEncode(value));
        if (isTeam)
            uri.append("&isTeam");
        if (isMaster)
            uri.append("&isMaster");
        printRedirectInstruction(uri.toString(), 0);
        out.write("</head><body></body></html>");
    }

    private void printRedirectInstruction(String url, int delay) {
        writeRedirectInstruction(url, delay);
    }

    public static final String MASTER_ROOT = "/MasterRoot";
    public static final String TEAM_ROOT = "/TeamRoot";
    private static final String INDIV_ROOT = "/IndivRoot";
    private static final String INDIV2_ROOT = "/Indiv2Root";
    private static final String DISABLE_TEAM_IMPORT_REPAIR_DATA_NAME = "Disable_Team_Import_Repairs";
    private static final String DISABLE_TEAM_IMPORT_REPAIR_SETTING = "disableTeamImportRepairs";
    private static final String MIGRATE_DATA_NAME = "Team_Project_Migration_Needed";
    private static final String CONVERT_DATA_NAME = "Team_Project_Conversion_Needed";
    private static final String PROMPT_FOR_PSP_SUBSET = "/Prompt_for_PSP_Subset_On_WBS_Sync";
    public static final String HIER_FILENAME = "projDump.xml";
    private static final String WORKFLOW_FILENAME = "workflowDump.xml";

    private static final String NOT_TEAM_PROJECT = "notTeamProject";
    private static final String TEAM_DIR_MISSING = "teamDirMissing";
    private static final String TEAM_DIR_UNAVAILABLE = "teamDirUnavailable&teamDir";
    private static final String SERVER_UNAVAILABLE = "serverUnavailable&serverUrl";
    static final String WBS_FILE_MISSING = "wbsFileMissing";
    private static final String WBS_FILE_INACCESSIBLE = "wbsFileInaccessible";
    private static final String INITIALS_MISSING = "initialsMissing";
    private static final String EMPTY_TEAM = "emptyTeam";
    private static final String UNLISTED_MEMBER = "unlistedMember";
    private static final String HIER_EDITOR_OPEN = "hierEditorOpen";

    private static final String BACKGROUND_PARAM = "bg";
    private static final String BRIEF_PARAM = "brief";
    private static final String RUN_PARAM = "run";
    private static final String SAVE_PERMS = "savePerms";
    private static final String CHANGE_INITIALS = "changeInitials";
    private static final String UNLISTED_INITIALS = "-NL";
    private static final String COMPLETE_PREFIX = "COMPLETE:";
    private static final String DELETE_PREFIX = "DELETE:";
    private static final String PSP_SUBSET_PREFIX = "PSPSUBSET:";
    private static final String COMPLETE_DATANAME = "complete_ //list";
    private static final String DELETE_DATANAME = "delete_ //list";
    private static final String PSP_SUBSET_DATANAME = "pspsubset_ //list";
    private static final String CHANGES_DATANAME = "changes_ //list";
    private static final String MERGE_PARAM = "bundleMerge";
    private static final String MERGE_SKIP = "skip";
    private static final String MERGE_SKIP_HIDDEN = "<input type='hidden' "
            + "name='" + MERGE_PARAM + "' value='" + MERGE_SKIP + "'/>\n";
    private static final String MERGE_CONFIRM = "mergeConfirm";
    private static final String WBS_MERGE_DUMP_URI = //
            "openWBS.shtm?dumpAndExit&dumpCallback";

    private static final String READ_ONLY_MODE_ERR_MESSAGE =
        "You are currently running the dashboard in read-only mode, so " +
        "no changes can be made.";
}
