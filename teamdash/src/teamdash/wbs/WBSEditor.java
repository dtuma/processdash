// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.bridge.client.BridgedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.CompressedWorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectory;
import net.sourceforge.processdash.tool.bridge.client.WorkingDirectoryFactory;
import net.sourceforge.processdash.tool.export.mgr.ExternalLocationMapper;
import net.sourceforge.processdash.ui.lib.ExceptionDialog;
import net.sourceforge.processdash.ui.lib.GuiPrefs;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.LargeFontsHelper;
import net.sourceforge.processdash.ui.macosx.MacGUIUtils;
import net.sourceforge.processdash.util.DashboardBackupFactory;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.PreferencesUtils;
import net.sourceforge.processdash.util.RuntimeUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.UsageLogger;
import net.sourceforge.processdash.util.lock.AlreadyLockedException;
import net.sourceforge.processdash.util.lock.CannotCreateLockException;
import net.sourceforge.processdash.util.lock.LockFailureException;
import net.sourceforge.processdash.util.lock.LockMessage;
import net.sourceforge.processdash.util.lock.LockMessageHandler;
import net.sourceforge.processdash.util.lock.LockUncertainException;
import net.sourceforge.processdash.util.lock.ReadOnlyLockFailureException;
import net.sourceforge.processdash.util.lock.SentLockMessageException;

import teamdash.SaveListener;
import teamdash.merge.ui.MergeConflictDialog;
import teamdash.merge.ui.MergeConflictNotification.ModelType;
import teamdash.team.TeamMember;
import teamdash.team.TeamMemberList.InitialsListener;
import teamdash.team.TeamMemberListEditor;
import teamdash.wbs.ChangeHistory.Entry;
import teamdash.wbs.WBSTabPanel.LoadTabsException;
import teamdash.wbs.columns.CustomColumnManager;
import teamdash.wbs.columns.ErrorNotesColumn;
import teamdash.wbs.columns.PercentCompleteColumn;
import teamdash.wbs.columns.PercentSpentColumn;
import teamdash.wbs.columns.PlanTimeWatcher;
import teamdash.wbs.columns.PlanTimeWatcher.PlanTimeDiscrepancyEvent;
import teamdash.wbs.columns.PlanTimeWatcher.PlanTimeDiscrepancyListener;
import teamdash.wbs.columns.SizeAccountingColumnSet;
import teamdash.wbs.columns.SizeActualDataColumn;
import teamdash.wbs.columns.TeamActualTimeColumn;
import teamdash.wbs.columns.TeamCompletionDateColumn;
import teamdash.wbs.columns.TeamTimeColumn;
import teamdash.wbs.columns.UnassignedTimeColumn;

public class WBSEditor implements WindowListener, SaveListener,
        LockMessageHandler, WBSFilenameConstants {

    public static final String INTENT_WBS_EDITOR = "showWbsEditor";
    public static final String INTENT_TEAM_EDITOR = "showTeamListEditor";

    WorkingDirectory workingDirectory;
    TeamProject teamProject;
    MilestonesDataModel milestonesModel;
    JFrame frame;
    WBSTabPanel tabPanel;
    GuiPrefs guiPrefs;
    TeamTimePanel teamTimePanel;
    WBSDataWriter dataWriter;
    File dataDumpFile;
    WBSDataWriter workflowWriter;
    File workflowDumpFile;
    WBSSynchronizer reverseSynchronizer;
    MergeConflictDialog mergeConflictDialog;
    TeamProjectMergeCoordinator mergeCoordinator;
    TeamProjectMergeDebugger mergeDebugger;
    File customTabsFile;
    ChangeHistory changeHistory;
    File changeHistoryFile;
    String owner;
    private int mode;
    boolean showActualData = false;
    boolean showActualSize = false;
    boolean readOnly = false;
    boolean simultaneousEditing = false;
    boolean holdingStartupLock = false;
    boolean indivMode = false;
    boolean exitOnClose = false;
    String syncURL = null;
    boolean disposed = false;
    private boolean dirty = false;
    private DirtyListener dirtyListener;

    private TeamMemberListEditor teamListEditor = null;
    private WorkflowEditor workflowEditor = null;
    private MilestonesEditor milestonesEditor = null;

    private static final int MODE_PLAIN = 1;
    private static final int MODE_HAS_MASTER = 2;
    private static final int MODE_MASTER = 4;
    private static final int MODE_BOTTOM_UP = 8;

    private static Resources resources = Resources.getDashBundle("WBSEditor");
    private static Preferences preferences = Preferences.userNodeForPackage(WBSEditor.class);
    private static final String EXPANDED_NODES_KEY_SUFFIX = "_EXPANDEDNODES";
    private static final String EXPANDED_NODES_DELIMITER = Character.toString('\u0001');
    private static final String OPTIMIZE_FOR_INDIV_KEY = "optimizeForIndiv";
    private static final String PROMPT_READ_ONLY_SETTING = "promptForReadOnly";
    private static final String MEMBERS_CANNOT_EDIT_SETTING = "readOnlyForIndividuals";
    private static final String ALLOW_SIMULTANEOUS_EDIT_SETTING = "allowSimultaneousEditing";
    private static final String INITIALS_POLICY_SETTING = "initialsPolicy";
    public static final String PROJECT_CLOSED_SETTING = "projectClosed";

    public WBSEditor(WorkingDirectory workingDirectory,
            TeamProject teamProject, String owner, String initials)
            throws LockFailureException {

        this.workingDirectory = workingDirectory;
        this.teamProject = teamProject;
        acquireLock(owner);

        File storageDir = teamProject.getStorageDirectory();
        this.dataDumpFile = new File(storageDir, DATA_DUMP_FILE);
        this.workflowDumpFile = new File(storageDir, WORKFLOW_DUMP_FILE);
        this.customTabsFile = new File(storageDir, CUSTOM_TABS_FILE);
        this.changeHistoryFile = new File(storageDir, CHANGE_HISTORY_FILE);
        this.readOnly = teamProject.isReadOnly();
        this.dirtyListener = new DirtyListener();
        setDirty(false);

        setMode(teamProject);

        if (isMode(MODE_HAS_MASTER) && !readOnly) {
            MasterWBSUtil.mergeFromMaster(teamProject);
        }

        WBSModel model = teamProject.getWBS();

        TaskDependencySource taskDependencySource = getTaskDependencySource();
        DataTableModel data = new DataTableModel
            (model, teamProject.getTeamMemberList(),
             teamProject.getTeamProcess(), teamProject.getWorkflows(),
             teamProject.getMilestones(), taskDependencySource, owner);

        milestonesModel = new MilestonesDataModel(teamProject.getMilestones());

        if (isMode(MODE_PLAIN)) {
            reverseSynchronizer = new WBSSynchronizer(teamProject, data);
            if (Boolean.getBoolean("teamdash.wbs.reverseSyncNewMembers"))
                reverseSynchronizer.setCreateMissingTeamMembers(true);
            reverseSynchronizer.run();
            reverseSynchronizer.setCreateMissingTeamMembers(false);
            showActualData = reverseSynchronizer.getFoundActualData();
            showActualSize = reverseSynchronizer.getFoundActualSizeData();
        }

        // record the valid task types in this process.
        recordValidTaskTypes(model);
        recordValidTaskTypes(teamProject.getWorkflows());

        dataWriter = new WBSDataWriter(model, data,
                teamProject.getTeamProcess(), teamProject.getProjectID(),
                teamProject.getTeamMemberList(), teamProject.getMilestones(),
                teamProject.getUserSettings());
        workflowWriter = new WBSDataWriter(teamProject.getWorkflows(), null,
                teamProject.getTeamProcess(), teamProject.getProjectID(), null,
                null, null);
        if (!readOnly && workingDirectory != null) {
            try {
                workingDirectory.doBackup("startup");
            } catch (IOException e) {}
        }
        this.owner = owner;

        initializeChangeHistory();

        runStartupAutomaticMods(teamProject, data);
        maybePerformStartupSave();
        if (holdingStartupLock)
            workingDirectory.releaseWriteLock();

        /*
         * Build the items in the user interface
         */

        MacGUIUtils.tweakLookAndFeel();

        guiPrefs = new GuiPrefs(WBSEditor.class, teamProject.getProjectID());

        // set expanded nodes in model based on saved user preferences
        Set expandedNodes = getExpandedNodesPref(teamProject.getProjectID());
        if (expandedNodes != null) {
            model.setExpandedNodeIDs(expandedNodes);
        }

        tabPanel = new WBSTabPanel(model, data, teamProject.getTeamProcess(),
                teamProject.getWorkflows(), taskDependencySource);
        tabPanel.setReadOnly(readOnly);
        teamProject.getTeamMemberList().addInitialsListener(tabPanel);

        if (simultaneousEditing) {
            mergeConflictDialog = new MergeConflictDialog(teamProject);
            mergeConflictDialog.setDataModel(ModelType.Wbs, data);
            mergeConflictDialog.setHyperlinkHandler(ModelType.Wbs, tabPanel);
            mergeConflictDialog.setDataModel(ModelType.Milestones,
                milestonesModel);
        }

        String[] sizeMetrics = teamProject.getTeamProcess().getSizeMetrics();
        String[] sizeTabColIDs = new String[sizeMetrics.length+2];
        String[] sizeTabColNames = new String[sizeMetrics.length+2];
        String[] planSizeTabColIDs = new String[sizeMetrics.length];
        String[] actSizeTabColIDs = new String[sizeMetrics.length];
        String[] sizeDataColNames = new String[sizeMetrics.length];
        sizeTabColIDs[0] = "Size";       sizeTabColNames[0] = "Size";
        sizeTabColIDs[1] = "Size-Units"; sizeTabColNames[1] = "Units";
        for (int i = 0; i < sizeMetrics.length; i++) {
            String units = sizeMetrics[i];
            sizeTabColIDs[i+2] = SizeAccountingColumnSet.getNCID(units);
            sizeTabColNames[i+2] = units;
            planSizeTabColIDs[i] = SizeActualDataColumn.getColumnID(units, true);
            actSizeTabColIDs[i] = SizeActualDataColumn.getColumnID(units, false);
            sizeDataColNames[i] = units;
        }
        tabPanel.addTab(showActualSize ? "Launch Size" : "Size", sizeTabColIDs,
            sizeTabColNames);

        tabPanel.addTab(showActualSize ? "Launch Size Accounting" : "Size Accounting",
                     new String[] { "Size-Units", "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" },
                     new String[] { "Units",  "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" });

        if (showActualSize) {
            tabPanel.addTab("Plan Size", planSizeTabColIDs, sizeDataColNames);
            tabPanel.addTab("Actual Size", actSizeTabColIDs, sizeDataColNames);
        }

        if (!isMode(MODE_MASTER))
            tabPanel.addTab((showActualData ? "Planned Time" : "Time"),
                     new String[] { TeamTimeColumn.COLUMN_ID,
                                    WBSTabPanel.TEAM_MEMBER_PLAN_TIMES_ID,
                                    UnassignedTimeColumn.COLUMN_ID },
                     new String[] { "Team", "", "Unassigned" });

        tabPanel.addTab("Task Time",
                new String[] { "Phase", "Task Size", "Task Size Units", "Rate",
                        ifMode(MODE_PLAIN, "Hrs/Indiv"),
                        ifMode(MODE_PLAIN, "# People"),
                        (isMode(MODE_MASTER) ? "TimeNoErr" : "Time"),
                        ifNotMode(MODE_MASTER, "Assigned To"),
                        (showActualData ? TeamCompletionDateColumn.COLUMN_ID : null),
                        (showActualData ? PercentCompleteColumn.COLUMN_ID : null),
                        (showActualData ? PercentSpentColumn.COLUMN_ID : null),
                        (showActualData ? TeamActualTimeColumn.COLUMN_ID : null) },
                new String[] { "Phase/Type", "Task Size", "Units", "Rate",
                        "Hrs/Indiv", "# People", "Time", "Assigned To",
                        "Completed", "%C", "%S", "Actual Time" });

        tabPanel.addTab("Task Details",
                new String[] { "Milestone", "Labels", WBSTabPanel.CUSTOM_COLUMNS_ID,
                               "Dependencies", "Notes", ErrorNotesColumn.COLUMN_ID },
                new String[] { "Milestone", "Task Labels", "", "Task Dependencies",
                               "Notes", null });

        if (showActualData)
            tabPanel.addTab("Actual Time",
                new String[] { TeamActualTimeColumn.COLUMN_ID,
                               WBSTabPanel.TEAM_MEMBER_ACTUAL_TIMES_ID },
                new String[] { "Team", "" });

        //String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        //table.addTab("Defects", s, s);

        // read in custom tabs file
        try {
            tabPanel.loadTabs(customTabsFile);
        } catch (LoadTabsException e) {
        }
        tabPanel.wbsTable.setEnterInsertsLine(getInsertOnEnterPref(teamProject
                .getProjectID()));

        tabPanel.addChangeListener(this.dirtyListener);

        teamTimePanel =
            new TeamTimePanel(teamProject.getTeamMemberList(), data,
                milestonesModel);
        teamTimePanel.setVisible(isMode(MODE_BOTTOM_UP));
        if (isMode(MODE_BOTTOM_UP))
            teamTimePanel.setShowBalancedBar(false);
        teamTimePanel.setShowRemainingWork(showActualData == true);

        try {
            new MacOSXWBSHelper(this);
        } catch (Throwable t) {}

        String windowTitle;
        if (workingDirectory instanceof CompressedWorkingDirectory)
            windowTitle = workingDirectory.getDescription();
        else
            windowTitle = teamProject.getProjectName();
        windowTitle = windowTitle
                + " - Work Breakdown Structure"
                + (teamProject.isReadOnly() ? " (Read-Only)" : "");

        frame = new JFrame(windowTitle);
        frame.setJMenuBar(buildMenuBar(tabPanel, teamProject.getWorkflows(),
            teamProject.getMilestones(), data, initials));
        frame.getContentPane().add(tabPanel);
        frame.getContentPane().add(teamTimePanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
    }

    private void acquireLock(String owner) throws LockFailureException {
        if (teamProject.isReadOnly() || workingDirectory == null)
            return;

        if (maybePromptForReadOnly())
            return;

        maybeSetupSimultaneousEditing();

        try {
            workingDirectory.acquireWriteLock(this, owner);
        } catch (ReadOnlyLockFailureException e) {
            if (!showOpenReadOnlyMessage("Errors.Read_Only_Files.Message_FMT"))
                throw e;
        } catch (CannotCreateLockException e) {
            if (!showOpenReadOnlyMessage("Errors.Cannot_Create_Lock.Message_FMT"))
                throw e;
        } catch (LockFailureException e) {
            if (!simultaneousEditing
                    && !showOpenReadOnlyMessage(
                            "Errors.Concurrent_Use.Message_FMT",
                            getOtherLockHolder(e)))
                throw e;
        }

        if (simultaneousEditing) {
            if (needsStartupLock())
                holdingStartupLock = true;
            else
                workingDirectory.releaseWriteLock();
        }
    }

    private boolean maybePromptForReadOnly() {
        if (teamProject.getBoolUserSetting(PROMPT_READ_ONLY_SETTING) == false)
            return false;

        if (isDumpAndExitMode())
            return false;

        if (isZipWorkingDirectory())
            return false;

        JRadioButton readWriteOption = new JRadioButton(resources
                .getString("Recommend_Read_Only.Option_Read_Write"));
        JRadioButton readOnlyOption = new JRadioButton(resources
                .getString("Recommend_Read_Only.Option_Read_Only"));
        ButtonGroup group = new ButtonGroup();
        group.add(readWriteOption);
        group.add(readOnlyOption);
        readWriteOption.setSelected(true);

        String title = resources.getString("Recommend_Read_Only.Title");
        String explanationKey;
        if (teamProject.getBoolUserSetting(ALLOW_SIMULTANEOUS_EDIT_SETTING, true))
            explanationKey = "Recommend_Read_Only.Simultaneous_Prompt";
        else
            explanationKey = "Recommend_Read_Only.Exclusive_Prompt";
        Object[] message = new Object[] { resources.getStrings(explanationKey),
                readOnlyOption, readWriteOption };
        JOptionPane.showMessageDialog(null, message, title,
            JOptionPane.QUESTION_MESSAGE);

        if (readWriteOption.isSelected())
            return false;

        teamProject.setReadOnly(true);
        return true;
    }

    private void maybeSetupSimultaneousEditing() {
        // if simultaneous editing is disabled, do nothing.
        if (!teamProject.getBoolUserSetting(ALLOW_SIMULTANEOUS_EDIT_SETTING, true))
            return;

        // no need for simultaneous editing in dump-and-exit mode.
        if (isDumpAndExitMode())
            return;

        // simultaneous editing does not make sense for a ZIP file.
        if (isZipWorkingDirectory())
            return;

        try {
            mergeCoordinator = new TeamProjectMergeCoordinator(teamProject,
                workingDirectory);
            if (Boolean.getBoolean("teamdash.wbs.mergeDebugging"))
                mergeDebugger = new TeamProjectMergeDebugger();
            else
                mergeDebugger = new TeamProjectMergeDebuggerSimple();
            mergeCoordinator.setMergeListener(mergeDebugger);
            simultaneousEditing = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean showOpenReadOnlyMessage(String resKey) {
        String location = workingDirectory.getDescription();
        return showOpenReadOnlyMessage(resKey, location);
    }

    private boolean showOpenReadOnlyMessage(String resKey, String formatArg) {
        String title = resources.getString("Errors.Open_Read_Only.Title");
        Object[] message = resources.formatStrings(resKey, formatArg);
        maybeDumpStartupError(title, message);
        message = new Object[] { message, " ",
                resources.getString("Errors.Open_Read_Only.Prompt") };
        int userResponse = JOptionPane.showConfirmDialog(null, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (userResponse == JOptionPane.YES_OPTION) {
            teamProject.setReadOnly(true);
            return true;
        }
        return false;
    }

    private static String getOtherLockHolder(Exception e) {
        String otherOwner = null;
        if (e instanceof AlreadyLockedException)
            otherOwner = ((AlreadyLockedException) e).getExtraInfo();
        if (otherOwner == null || otherOwner.length() == 0)
            otherOwner = resources
                    .getString("Errors.Concurrent_Use.Anonymous_User");
        return otherOwner;
    }

    private boolean needsStartupLock() {
        if (isDumpAndExitMode())
            return true;

        if (teamProject.getBoolUserSetting(RelaunchWorker.RELAUNCH_PROJECT_SETTING))
            return true;

        return false;
    }

    private void runStartupAutomaticMods(TeamProject teamProject,
            DataTableModel data) {
        if (readOnly)
            return;

        if (teamProject.getBoolUserSetting(RelaunchWorker.RELAUNCH_PROJECT_SETTING)) {
            setDirty(true);
            new RelaunchWorker(teamProject, data).run();
        }
    }

    private void maybePerformStartupSave() {
        if (isDirty() || isDumpAndExitMode()) {
            boolean dataSaved = false;
            try {
                if (saveData()) {
                    dataSaved = true;
                    setDirty(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (isDumpAndExitMode()) {
                workingDirectory.releaseLocks();
                if (dataSaved) {
                    System.out.println("Saved updated WBS data at " + new Date());
                    System.exit(0);
                } else {
                    maybeDumpStartupError("Cannot Save Data", new Object[] { //
                        "This process was unable to save WBS data." });
                }
            }
        }
    }

    public void showApplicableStartupMessages() {
        maybeShowProjectClosedMessage();
    }

    private void maybeShowProjectClosedMessage() {
        if (teamProject.getBoolUserSetting(PROJECT_CLOSED_SETTING) != true)
            return;

        if (workingDirectory instanceof CompressedWorkingDirectory)
            return;

        JOptionPane.showMessageDialog(frame,
            resources.getStrings("Project_Closed.Message"),
            resources.getString("Project_Closed.Title"),
            JOptionPane.WARNING_MESSAGE);
    }


    private void setMode(TeamProject teamProject) {
        if (teamProject instanceof TeamProjectBottomUp)
            this.mode = MODE_BOTTOM_UP;
        else if (teamProject.isMasterProject())
            this.mode = MODE_MASTER;
        else {
            this.mode = MODE_PLAIN;
            if (teamProject.getMasterProjectDirectory() != null)
                this.mode |= MODE_HAS_MASTER;
        }
    }

    private String ifMode(int m, String id) {
        return (isMode(m) ? id : null);
    }
    private String ifNotMode(int m, String id) {
        return (isMode(m) ? null : id);
    }
    private boolean isMode(int m) {
        return ((mode & m) == m);
    }

    private boolean isNotZipWorkingDirectory() {
        return !isZipWorkingDirectory();
    }

    private boolean isZipWorkingDirectory() {
        return workingDirectory instanceof CompressedWorkingDirectory;
    }

    private TaskDependencySource getTaskDependencySource() {
        if (isMode(MODE_PLAIN + MODE_HAS_MASTER))
            return new TaskDependencySourceMaster(teamProject);
        else
            return new TaskDependencySourceSimple(teamProject);
    }

    private void recordValidTaskTypes(WBSModel model) {
        Set taskTypes = new HashSet();
        for (Object phase : teamProject.getTeamProcess().getPhases())
            taskTypes.add(phase + " Task");
        taskTypes.add("PSP Task");

        model.getValidator().setValidTaskTypes(taskTypes);
        model.fireTableCellUpdated(0, 0);
    }

    public void setExitOnClose(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    public void setIndivMode(boolean indivMode) {
        this.indivMode = indivMode;

        if (replaceAction != null)
            replaceAction.setEnabled(!indivMode && !readOnly);
    }

    private void setSyncURL(String syncURL) {
        this.syncURL = syncURL;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void show() {
        frame.setVisible(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public String handleMessage(LockMessage lockMessage) {
        String message = lockMessage.getMessage();
        if (INTENT_TEAM_EDITOR.equals(message)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    showTeamListEditor();
                }});
            return "OK";
        }
        if (INTENT_WBS_EDITOR.equals(message)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (teamListEditor != null)
                        teamListEditor.setCommitButtonIsSave(false);
                    raiseWindow();
                }});
            return "OK";
        }
        if (LockMessage.LOCK_LOST_MESSAGE.equals(message)) {
            if (readOnly == false) {
                readOnly = true;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        showLostLockMessage();
                    }});
            }
            return "OK";
        }
        throw new IllegalArgumentException();
    }

    public void raiseWindow() {
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
        frame.toFront();
    }

    public void showTeamListEditor() {
        maybeCreateTeamListEditor();
        teamListEditor.show();
    }

    public void showTeamListEditorWithSaveButton() {
        maybeCreateTeamListEditor();
        teamListEditor.setCommitButtonIsSave(true);
        teamListEditor.show();
    }

    private void maybeCreateTeamListEditor() {
        if (teamListEditor == null) {
            teamListEditor = new TeamMemberListEditor(
                    teamProject.getProjectName(),
                    teamProject.getTeamMemberList(),
                    teamProject.getUserSetting(INITIALS_POLICY_SETTING));
            teamListEditor.addSaveListener(this);
            if (mergeConflictDialog != null)
                mergeConflictDialog.setHyperlinkHandler(ModelType.TeamList,
                    teamListEditor);
        }
    }

    public void showLostLockMessage() {
        readOnly = true;
        tabPanel.setReadOnly(true);
        saveAction.setEnabled(false);
        if (replaceAction != null) replaceAction.setEnabled(false);
        if (importFromCsvAction != null) importFromCsvAction.setEnabled(false);

        String title = resources.getString("Errors.Lost_Lock.Title");
        Object[] message = resources.getStrings("Errors.Lost_Lock.Message");
        if (isDirty())
            message = new Object[] { message, " ",
                    resources.getStrings("Errors.Lost_Lock.Save_Advice") };
        JOptionPane.showMessageDialog(frame, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private void showEditPreferencesDialog() {
        JCheckBox readOnlyPrompt = new JCheckBox(
                "On startup, offer to open in read-only mode");
        boolean readOnlyPromptSetting = teamProject
                .getBoolUserSetting(PROMPT_READ_ONLY_SETTING);
        readOnlyPrompt.setSelected(readOnlyPromptSetting);

        JCheckBox membersCanEdit = new JCheckBox(
                "Allow team members to edit the WBS");
        boolean membersCanEditSetting = !teamProject
                .getBoolUserSetting(MEMBERS_CANNOT_EDIT_SETTING);
        membersCanEdit.setSelected(membersCanEditSetting);
        membersCanEdit.setEnabled(!indivMode);

        JCheckBox allowSimulEdit = new JCheckBox(
                "Allow multiple people to edit the WBS simultaneously");
        boolean simulEditSetting = teamProject
                .getBoolUserSetting(ALLOW_SIMULTANEOUS_EDIT_SETTING, true);
        allowSimulEdit.setSelected(simulEditSetting);
        allowSimulEdit.setEnabled(!indivMode);

        JCheckBox initialsPolicy = null;
        String globalInitialsPolicy = System
                .getProperty("teamdash.wbs.globalInitialsPolicy");
        boolean initialsPolicySetting = false;
        if ("username".equals(globalInitialsPolicy)) {
            initialsPolicy = new JCheckBox(
                    "Identify team members by username, not initials");
            String localInitialsPolicy = teamProject
                    .getUserSetting(INITIALS_POLICY_SETTING);
            initialsPolicySetting = "username".equals(localInitialsPolicy);
            initialsPolicy.setSelected(initialsPolicySetting);
            initialsPolicy.setEnabled(!indivMode);
        }

        JCheckBox projectClosed = null;
        String projectClosedSettingStr = teamProject
                .getUserSetting(PROJECT_CLOSED_SETTING);
        boolean projectClosedSetting = false;
        if (projectClosedSettingStr != null) {
            projectClosed = new JCheckBox(
                    "This project/iteration is closed for ongoing work");
            projectClosedSetting = "true".equals(projectClosedSettingStr);
            projectClosed.setSelected(projectClosedSetting);
            projectClosed.setEnabled(!indivMode);
        }

        Object[] message = new Object[] { readOnlyPrompt, membersCanEdit,
                allowSimulEdit, initialsPolicy, projectClosed };
        int userChoice = JOptionPane.showConfirmDialog(frame, message,
            "Edit Preferences", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (userChoice != JOptionPane.OK_OPTION)
            return;

        boolean madeChange = false;
        boolean restartRequired = false;

        boolean newReadOnlyPromptSetting = readOnlyPrompt.isSelected();
        if (newReadOnlyPromptSetting != readOnlyPromptSetting) {
            teamProject.putUserSetting(PROMPT_READ_ONLY_SETTING,
                newReadOnlyPromptSetting);
            madeChange = true;
        }

        boolean newMembersCanEditSetting = membersCanEdit.isSelected();
        if (newMembersCanEditSetting != membersCanEditSetting) {
            teamProject.putUserSetting(MEMBERS_CANNOT_EDIT_SETTING,
                !newMembersCanEditSetting);
            madeChange = true;
        }

        boolean newSimulEditSetting = allowSimulEdit.isSelected();
        if (newSimulEditSetting != simulEditSetting) {
            teamProject.putUserSetting(ALLOW_SIMULTANEOUS_EDIT_SETTING,
                newSimulEditSetting);
            madeChange = true;
            restartRequired = true;
        }

        if (initialsPolicy != null) {
            boolean newInitialsPolicySetting = initialsPolicy.isSelected();
            if (initialsPolicySetting != newInitialsPolicySetting) {
                teamProject.putUserSetting(INITIALS_POLICY_SETTING,
                    newInitialsPolicySetting ? "username" : "initials");
                madeChange = true;
                restartRequired = true;
            }
        }

        if (projectClosed != null) {
            boolean newProjectClosedSetting = projectClosed.isSelected();
            if (projectClosedSetting != newProjectClosedSetting) {
                teamProject.putUserSetting(PROJECT_CLOSED_SETTING,
                    newProjectClosedSetting);
                madeChange = true;
                setDirty(true);
            }
        }

        if (madeChange) {
            try {
                workingDirectory.flushData();
            } catch (Exception ex) {
                setDirty(true);
            }
        }

        if (restartRequired)
            JOptionPane.showMessageDialog(frame, "You will need to close and "
                    + "reopen the WBS Editor for your changes to take effect.",
                "Restart Required", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWorkflowEditor() {
        if (workflowEditor != null)
            workflowEditor.show();
        else {
            workflowEditor = new WorkflowEditor(teamProject);
            workflowEditor.addChangeListener(this.dirtyListener);
            if (mergeConflictDialog != null) {
                mergeConflictDialog.setHyperlinkHandler(ModelType.Workflows,
                    workflowEditor);
                mergeConflictDialog.setDataModel(ModelType.Workflows,
                    workflowEditor.workflowModel);
            }
        }
    }

    private void showMilestonesEditor() {
        if (milestonesEditor != null)
            milestonesEditor.show();
        else {
            milestonesEditor = new MilestonesEditor(teamProject, milestonesModel);
            milestonesEditor.addChangeListener(this.dirtyListener);
            if (mergeConflictDialog != null)
                mergeConflictDialog.setHyperlinkHandler(ModelType.Milestones,
                    milestonesEditor);
        }
        milestonesEditor.show();
    }

    private JMenuBar buildMenuBar(WBSTabPanel tabPanel, WBSModel workflows,
            WBSModel milestones, DataTableModel dataModel, String initials) {
        JMenuBar result = new JMenuBar();

        result.add(buildFileMenu(tabPanel.getFileActions()));
        result.add(buildEditMenu(tabPanel.getEditingActions()));
        result.add(buildTabMenu(tabPanel.getTabActions()));
        if (!isMode(MODE_BOTTOM_UP))
            result.add(buildWorkflowMenu
                (workflows, tabPanel.getInsertWorkflowAction(workflows)));
        result.add(buildMilestonesMenu(milestones));
        if (isMode(MODE_HAS_MASTER)
                && "true".equals(teamProject.getUserSetting("showMasterMenu")))
            result.add(buildMasterMenu(tabPanel.getMasterActions(teamProject)));
        if (!isMode(MODE_MASTER))
            result.add(buildTeamMenu(initials, dataModel));

        return result;
    }
    private Action saveAction, replaceAction, importFromCsvAction;
    private JMenu buildFileMenu(Action[] fileActions) {
        JMenu result = new JMenu("File");
        result.setMnemonic('F');
        result.add(saveAction = new SaveAction());
        if (mergeCoordinator != null)
            result.add(new RefreshAction());
        result.addSeparator();
        if (mergeDebugger != null && mergeDebugger.supportsZipOfAllMerges()) {
            result.add(new SaveMergeDebugZipAction());
            result.addSeparator();
        }
        if (!isMode(MODE_BOTTOM_UP)) {
            WBSOpenFileAction openAction = new WBSOpenFileAction(frame);
            result.add(new WBSSaveAsAction(this, openAction));
            result.add(openAction);
            result.add(replaceAction = new WBSReplaceAction(this, openAction));
            result.addSeparator();
            result.add(importFromCsvAction = new ImportFromCsvAction());
        }
        for (int i = 0; i < fileActions.length; i++) {
            result.add(fileActions[i]);
        }
        result.addSeparator();
        result.add(new CloseAction());
        return result;
    }
    private JMenu buildEditMenu(Action[] editingActions) {
        JMenu result = new JMenu("Edit");
        result.setMnemonic('E');
        for (int i = 0;   i < editingActions.length;   i++) {
            if (editingActions[i].getValue(Action.NAME) != null)
                result.add(editingActions[i]);
            if (i == 1) result.addSeparator();
        }

        if (!readOnly && isNotZipWorkingDirectory()) {
            result.addSeparator();
            result.add(new EditPreferencesAction());
        }

        return result;
    }
    private JMenu buildTabMenu(Action[] tabActions) {
        JMenu result = new JMenu("Tabs");
        result.setMnemonic('A');
        for (int i = 0; i < tabActions.length; i++) {
            result.add(tabActions[i]);
            if (i == 2 || i == 4) result.addSeparator();
        }

        return result;
    }
    private JMenu buildWorkflowMenu(WBSModel workflows,
                                    Action insertWorkflowAction) {
        JMenu result = new JMenu("Workflow");
        result.setMnemonic('W');
        result.add(new WorkflowEditorAction());
        // result.add(new DefineWorkflowAction());
        result.addSeparator();
        new WorkflowMenuBuilder(result, workflows, insertWorkflowAction);
        return result;
    }
    private JMenu buildMilestonesMenu(WBSModel milestones) {
        JMenu result = new JMenu("Milestones");
        result.setMnemonic('M');
        result.add(new MilestonesEditorAction());
        if (!isMode(MODE_MASTER)) {
            result.addSeparator();
            result.add(new ShowCommitDatesMenuItem());
            result.add(new ShowMilestoneMarksMenuItem());
            new BalanceMilestoneMenuBuilder(result, milestones);
        }
        return result;
    }
    private JMenu buildMasterMenu(Action[] masterActions) {
        JMenu result = new JMenu("Master");
        result.setMnemonic('S');
        for (int i = 0;   i < masterActions.length;   i++)
            result.add(masterActions[i]);
        return result;
    }
    private JMenu buildTeamMenu(String initials, DataTableModel dataModel) {
        JMenu result = new JMenu("Team");
        result.setMnemonic('T');
        if (isMode(MODE_PLAIN))
            result.add(new ShowTeamMemberListEditorMenuItem());

        TeamMember m = null;
        if (initials != null)
            m = teamProject.getTeamMemberList().findTeamMember(initials);
        if (m != null) {
            if (!readOnly) {
                WatchCoworkerTimesMenuItem watchMenu =
                    new WatchCoworkerTimesMenuItem(dataModel);
                result.add(new OptimizeEditingForIndivMenuItem(m, watchMenu));
                result.add(watchMenu);
            }

            // make nodes visible if they have a data problem and they are
            // assigned to this user
            ErrorNotesColumn.showNodesWithErrors(teamProject.getWBS(),
                null, new AssignedToMemberTest(dataModel, m));
        }

        result.add(new ShowTeamTimePanelMenuItem());
        if (showActualData) {
            ButtonGroup g = new ButtonGroup();
            result.add(new BottomUpShowReplanMenuItem(g));
            result.add(new BottomUpShowPlanMenuItem(g));
        }
        if (isMode(MODE_PLAIN)) {
            result.add(new BottomUpShowHoursPerWeekMenuItem());
            result.add(new BottomUpIncludeUnassignedMenuItem());
        }
        return result;
    }

    synchronized boolean mergeExternalChanges() throws IOException {
        if (mergeCoordinator == null || holdingStartupLock)
            return false;

        TeamProjectMerger merger = mergeCoordinator.doMerge();
        if (merger == null)
            return false; // no merge needed

        replaceDataFrom(merger.getMerged());
        mergeConflictDialog.addNotifications(merger
                .getConflicts(mergeConflictDialog));

        // reload the change history file to reflect external edits
        changeHistory = new ChangeHistory(changeHistoryFile);

        return true;
    }

    void replaceDataFrom(final TeamProject srcProject) {

        if (SwingUtilities.isEventDispatchThread()) {
            replaceDataFromImpl(srcProject);

        } else {
            // The data replacement actions will fire many events that trigger
            // GUI repaints.  As a result, we must execute this logic on the
            // Swing event dispatch thread to avoid thread safety issues.
            // Hopefully it can finish quickly.
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        replaceDataFromImpl(srcProject);
                    }});
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void replaceDataFromImpl(TeamProject srcProject) {
        File srcDir = srcProject.getStorageDirectory();

        replaceTeamMemberList(srcProject);
        replaceWorkflows(srcProject);
        replaceMilestones(srcProject);
        replaceWBS(srcProject);
        replaceTabs(srcDir);
        replaceUserSettings(srcProject);
        appendReplacementChange(srcDir);

        if (isMode(MODE_HAS_MASTER))
            MasterWBSUtil.mergeFromMaster(teamProject);
        if (reverseSynchronizer != null)
            reverseSynchronizer.run();

        setDirty(true);
    }

    private void replaceTeamMemberList(TeamProject src) {
        teamProject.getTeamMemberList().copyFrom(src.getTeamMemberList());
        if (teamListEditor != null)
            teamListEditor.origListWasReplaced();
    }

    private void replaceWorkflows(TeamProject src) {
        WBSJTable table = null;
        if (workflowEditor != null) {
            workflowEditor.stopEditing();
            table = workflowEditor.table;
        }
        replaceWBSModel(teamProject.getWorkflows(), src.getWorkflows(), table);
        if (workflowEditor != null)
            workflowEditor.undoList.clear();
    }

    private void replaceMilestones(TeamProject src) {
        WBSJTable table = null;
        if (milestonesEditor != null) {
            milestonesEditor.stopEditing();
            table = milestonesEditor.table;
        }
        replaceWBSModel(teamProject.getMilestones(), src.getMilestones(), table);
        if (milestonesEditor != null)
            milestonesEditor.undoList.clear();
    }

    private void replaceWBS(TeamProject src) {
        tabPanel.stopCellEditing();
        replaceWBSModel(teamProject.getWBS(), src.getWBS(), tabPanel.wbsTable);
        tabPanel.undoList.clear();
    }

    private void replaceWBSModel(WBSModel dest, WBSModel src, WBSJTable table) {
        // record the WBS nodes that are currently selected, and arrange
        // for them to be restored later
        if (table != null)
            new WBSTableSelectionRestorer(dest, table);

        // check to see if the source and target WBS are the same. If so, we
        // don't need to perform any model replacement and we can return
        // immediately. Fire an event though, in case earlier steps in a
        // merging operation have made internal changes to this model.
        if (src == dest) {
            dest.fireTableDataChanged();
            return;
        }

        // copy the set of expanded nodes from this project to the new data
        Set expandedNodes = dest.getExpandedNodeIDs();
        src.setExpandedNodeIDs(expandedNodes);

        // copy the WBS from the source to this dest
        String rootNodeName = dest.getRoot().getName();
        dest.copyFrom(src);
        dest.getRoot().setName(rootNodeName);
    }

    private class WBSTableSelectionRestorer implements Runnable {
        private WBSModel wbsModel;
        private WBSJTable table;
        private List<Integer> selectedNodeIDs;
        private WBSTableSelectionRestorer(WBSModel model, WBSJTable table) {
            this.wbsModel = model;
            this.table = table;

            int[] selectedRows = table.getSelectedRows();
            if (selectedRows == null || selectedRows.length == 0)
                return;
            this.selectedNodeIDs = new ArrayList();
            for (WBSNode node : wbsModel.getNodesForRows(selectedRows, true))
                selectedNodeIDs.add(node.getUniqueID());

            SwingUtilities.invokeLater(this);
        }
        public void run() {
            Map<Integer, WBSNode> nodeMap = wbsModel.getNodeMap();
            table.clearSelection();
            for (int nodeId : selectedNodeIDs) {
                WBSNode node = nodeMap.get(nodeId);
                if (node != null) {
                    int row = wbsModel.getRowForNode(node);
                    if (row != -1)
                        table.getSelectionModel().addSelectionInterval(row, row);
                }
            }
        }
    }

    private void replaceTabs(File srcDir) {
        try {
            File tabsFile = new File(srcDir, CUSTOM_TABS_FILE);
            if (tabsFile.isFile())
                tabPanel.replaceCustomTabs(tabsFile);
        } catch (LoadTabsException e) {}
    }

    private void replaceUserSettings(TeamProject srcProject) {
        Properties thisProjectSettings = teamProject.getUserSettings();
        thisProjectSettings.clear();
        thisProjectSettings.putAll(srcProject.getUserSettings());
    }

    private void appendReplacementChange(File srcDir) {
        // in the change history for this WBS, add an entry to reflect that
        // we set the state equal to the last change in the replacement data.
        ChangeHistory hist = new ChangeHistory(srcDir);
        Entry e = hist.getLastEntry();
        if (e != null)
            changeHistory.addEntry(e.getUid(), owner);
    }

    private boolean save() {
        if (readOnly)
            return true;

        // finish all editing sessions in progress
        stopAllCellEditingSessions();
        if (maybeSaveTeamMemberList() == false)
            return false;

        JDialog dialog = createWaitDialog(frame, "Saving Data...");
        SaveThread saver = new SaveThread(dialog);
        saver.start();
        dialog.setVisible(true);
        setDirty(!saver.saveResult);
        return saver.saveResult;
    }

    private void stopAllCellEditingSessions() {
        tabPanel.stopCellEditing();
        if (teamListEditor != null) teamListEditor.stopEditing();
        if (workflowEditor != null) workflowEditor.stopEditing();
        if (milestonesEditor != null) milestonesEditor.stopEditing();
    }

    private boolean maybeSaveTeamMemberList() {
        // If the WBS Editor frame and the Team Member List are both showing,
        // save changes to the team member list before we save the WBS.
        if (teamListEditor != null && teamListEditor.isVisible()
                && frame.isVisible()) {
            teamListEditor.stopEditing();
            if (teamListEditor.isDirty() && teamListEditor.save() == false)
                return false;
        }
        return true;
    }

    static JDialog createWaitDialog(JFrame frame, String message) {
        JDialog dialog = new JDialog(frame, "Please Wait", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.getContentPane().add(buildWaitContents(message));
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        return dialog;
    }

    private static JFrame createWaitFrame(String message) {
        JFrame result = new JFrame("Please Wait");
        result.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        result.getContentPane().add(buildWaitContents(message));
        result.pack();
        result.setLocationRelativeTo(null);
        return result;
    }

    private static JPanel buildWaitContents(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        JLabel label = new JLabel(message);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 10));
        panel.add(label, BorderLayout.NORTH);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        panel.add(bar, BorderLayout.CENTER);

        panel.add(Box.createHorizontalStrut(200), BorderLayout.SOUTH);
        return panel;
    }

    private class SaveThread extends Thread {
        JDialog saveDialog;
        boolean saveResult;

        public SaveThread(JDialog saveDialog) {
            this.saveDialog = saveDialog;
        }

        public void run() {
            saveResult = saveImpl();
            if (saveResult) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            JPanel panel = (JPanel) saveDialog.getContentPane()
                                    .getComponent(0);
                            for (int i = 0;  i < panel.getComponentCount(); i++) {
                                Component c = panel.getComponent(i);
                                if (c instanceof JLabel) {
                                    JLabel label = (JLabel) c;
                                    label.setText("Data Saved.");
                                } else if (c instanceof JProgressBar) {
                                    JProgressBar bar = (JProgressBar) c;
                                    bar.setIndeterminate(false);
                                    bar.setValue(bar.getMaximum());
                                }
                            }
                            saveDialog.setTitle("Data Saved");
                        }});
                    Thread.sleep(750);
                } catch (Exception e) {}
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    saveDialog.dispose();
                    showMergeFollowUpInfo("File_Refresh.Save_Title", false);
                }});
        }

    }

    private boolean saveImpl() {
        if (readOnly)
            return true;

        try {
            if (saveData()) {
                maybeTriggerSyncOperation();
                return true;
            }

        } catch (LockUncertainException lue) {
        } catch (LockFailureException fe) {
            if (simultaneousEditing == false)
                showLostLockMessage();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            showSaveInternalErrorMessage(e);
            return false;
        }

        showSaveErrorMessage();
        return false;
    }

    private boolean saveData() throws LockFailureException, IOException {
        if (readOnly || workingDirectory == null)
            return true;

        // merge in any changes that have been saved by other individuals.
        mergeExternalChanges();

        // ensure that we have a lock for editing the data.
        if (simultaneousEditing && !holdingStartupLock) {
            acquireSimultaneousEditWriteLock();
        } else {
            workingDirectory.assertWriteLock();
        }

        try {
            // Doublecheck to see if someone else saved changes while we were
            // waiting for our simultaneous editing write lock.
            if (simultaneousEditing)
                mergeExternalChanges();

            if (teamProject.save() == false)
                return false;

            dataWriter.write(dataDumpFile);
            workflowWriter.write(workflowDumpFile);

            // write out custom tabs file, if the tabs have changed
            if (tabPanel != null)
                tabPanel.saveCustomTabsIfChanged(customTabsFile);

            // write out the change history file
            changeHistory.addEntry(owner);
            changeHistory.write(changeHistoryFile);

            if (workingDirectory.flushData() == false)
                return false;

            if (mergeCoordinator != null)
                mergeCoordinator.acceptChangesInMain();

            String qualifier = "saved";
            if (owner != null && owner.trim().length() > 0)
                qualifier = "saved_by_" + FileUtils.makeSafe(owner.trim());
            workingDirectory.doBackup(qualifier);

            return true;

        } finally {
            if (simultaneousEditing) {
                workingDirectory.releaseWriteLock();
                holdingStartupLock = false;
            }
        }
    }

    private void acquireSimultaneousEditWriteLock() throws LockFailureException {
        int timeoutSeconds = Integer.getInteger(
            "teamdash.wbs.acquireLockTimeout", 60);
        long timeoutTimestamp = System.currentTimeMillis()
                + (timeoutSeconds * 1000);
        Random r = null;
        AlreadyLockedException ale = null;

        while (System.currentTimeMillis() < timeoutTimestamp) {
            try {
                workingDirectory.acquireWriteLock(this, owner);
                return;

            } catch (AlreadyLockedException e) {
                // if someone else is holding the lock, wait for a moment to see
                // if they release it.  Then try again.
                ale = e;
                try {
                    // wait a randomly selected amount of time between 0.5 and
                    // 1.5 seconds.  Randomness is included in case several
                    // processes are attempting to get the lock at the same time
                    if (r == null)
                        r = new Random();
                    Thread.sleep(500 + r.nextInt(1000));
                } catch (InterruptedException e1) {}

            } catch (ReadOnlyLockFailureException e) {
                showSaveErrorMessage("Errors.Read_Only_Files.Message_FMT");
                throw e;
            } catch (LockFailureException e) {
                showSaveErrorMessage("Errors.Cannot_Create_Lock.Message_FMT");
                throw e;
            }
        }

        // we were unable to secure a lock within a reasonable amount of time.
        // display an error message stating who has the file locked.
        showSaveErrorMessage("Errors.Concurrent_Use.Message_FMT",
            getOtherLockHolder(ale));
        throw (ale != null ? ale : new LockFailureException());
    }

    private void showSaveErrorMessage() {
        String resKey = "Errors.Cannot_Save." + workingDirResKey() + "_FMT";
        showSaveErrorMessage(resKey);
    }

    private void showSaveErrorMessage(String resKey) {
        showSaveErrorMessage(resKey, workingDirectory.getDescription());
    }

    private void showSaveErrorMessage(String resKey, String formatArg) {
        String title = resources.getString("Errors.Cannot_Save.Title");
        Object[] message = resources.formatStrings(resKey, formatArg);
        if (isDirty())
            message = new Object[] { message, " ",
                    resources.getStrings("Errors.Cannot_Save.Save_Advice") };

        JOptionPane.showMessageDialog(frame, message, title,
            JOptionPane.ERROR_MESSAGE);
    }

    private void showSaveInternalErrorMessage(Exception e) {
        String title = resources.getString("Errors.Cannot_Save.Title");
        String[] message = resources
                .getStrings("Errors.Cannot_Save.Internal_Error");

        Object debugZipMsg = getDebugZipMessage(
            "Errors.Cannot_Save.Internal_Error_File_FMT", e);

        Object saveAdvice = null;
        if (isDirty())
            saveAdvice = new Object[] { " ",
                    resources.getStrings("Errors.Cannot_Save.Save_Advice") };

        ExceptionDialog.show(frame, title, message, e, debugZipMsg, saveAdvice);
    }

    private Object getDebugZipMessage(String resKey, Exception e) {
        if (mergeDebugger == null)
            return null;

        mergeDebugger.mergeException(e);
        File debugZipFile = mergeDebugger.makeZipOfCurrentMerge();
        if (debugZipFile == null)
            return null;

        return resources.formatStrings(resKey, debugZipFile.getPath());
    }

    private void showMergeFollowUpInfo(String titleKey,
            boolean showEvenIfNameListIsEmpty) {
        if (mergeConflictDialog == null)
            return;

        Object message = getUserNamesForMergedChanges();
        if (mergeConflictDialog.maybeShow(frame))
            return;

        if (message == null) {
            if (showEvenIfNameListIsEmpty == false)
                return;
            message = resources.getStrings("File_Refresh.Merge_Message");
        } else {
            message = new Object[] {
                resources.getStrings("File_Refresh.Merge_Message_Names"),
                message };
        }
        JOptionPane.showMessageDialog(frame, message,
                resources.getString(titleKey), JOptionPane.PLAIN_MESSAGE);
    }

    private String[] getUserNamesForMergedChanges() {
        if (mergeCoordinator == null)
            return null;

        Set<String> names = new TreeSet<String>();
        for (Entry change : mergeCoordinator.getMergedChanges(true))
            names.add(change.getUser());
        if (names.isEmpty())
            return null;

        String[] result = new String[names.size()];
        int i = 0;
        for (String name : names)
            result[i++] = "        \u2022 " + name;
        return result;
    }

    private void maybeTriggerSyncOperation() {
        if (syncURL != null)
            new SyncTriggerThread().start();
    }

    private class SyncTriggerThread extends Thread {
        public void run() {
            try {
                URL u = new URL(syncURL);
                URLConnection conn = u.openConnection();
                InputStream in = new BufferedInputStream(conn
                        .getInputStream());
                while (in.read() != -1)
                    ;
                in.close();
            } catch (Exception e) {}
        }
    }

    /** Give the user a chance to save data before the window closes.
     * 
     * @return false if the user selects cancel, true otherwise
     */
    private boolean maybeSave(boolean showCancel) {
        if (readOnly)
            return true;

        int buttons =
            (showCancel
                ? JOptionPane.YES_NO_CANCEL_OPTION
                : JOptionPane.YES_NO_OPTION);
        int result = JOptionPane.showConfirmDialog
            (frame, "Would you like to save changes?",
             "Save Changes?", buttons);
        switch (result) {
            case JOptionPane.CANCEL_OPTION:
            case JOptionPane.CLOSED_OPTION:
                return false;

            case JOptionPane.YES_OPTION:
                if (save() == false)
                    return false;
                break;
        }

        return true;
    }

    protected void maybeClose() {
        tabPanel.stopCellEditing();
        if (!isDirty() || maybeSave(true)) {
            // Set expanded nodes preference
            Set expandedNodes = teamProject.getWBS().getExpandedNodeIDs();
            setExpandedNodesPref(teamProject.getProjectID(), expandedNodes);
            setInsertOnEnterPref(teamProject.getProjectID(),
                tabPanel.wbsTable.getEnterInsertsLine());
            guiPrefs.saveAll();

            shutDown();
        }
    }

    private void shutDown() {
        if (workingDirectory != null)
            workingDirectory.releaseLocks();

        if (exitOnClose)
            System.exit(0);
        else {
            if (teamListEditor != null) teamListEditor.hide();
            if (workflowEditor != null) workflowEditor.hide();
            if (milestonesEditor != null) milestonesEditor.hide();
            frame.dispose();
            disposed = true;
        }
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {
        maybeClose();
    }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    public static WBSEditor createAndShowEditor(String[] locations,
            boolean bottomUp, boolean indivMode, String initials,
            boolean showTeamList, String syncURL, boolean exitOnClose,
            boolean forceReadOnly, String owner) {

        LargeFontsHelper.maybeInitialize();

        String message = (showTeamList
                ? "Opening Team Member List..."
                : "Opening Work Breakdown Structure...");
        JFrame waitFrame = null;
        if (!isDumpAndExitMode()) {
            waitFrame = createWaitFrame(message);
            waitFrame.setVisible(true);
        }

        LockMessageDispatcher dispatch;
        WorkingDirectory workingDirectory;
        File dir;
        TeamProject proj;

        if (bottomUp)
        {
            proj = new TeamProjectBottomUp(locations, "Team Project");
            dir = proj.getStorageDirectory();
            if (!dir.isDirectory()) {
                waitFrame.dispose();
                showCannotOpenError(locations[locations.length-1]);
                return null;
            }
            workingDirectory = null;
            dispatch = null;
        }
        else // if not bottom up
        {
            String intent = showTeamList ? INTENT_TEAM_EDITOR : INTENT_WBS_EDITOR;
            dispatch = new LockMessageDispatcher();
            workingDirectory = configureWorkingDirectory(locations, intent,
                dispatch);
            if (workingDirectory == null) {
                waitFrame.dispose();
                return null;
            }
            dir = workingDirectory.getDirectory();
            proj = new TeamProject(dir, "Team Project");

            if (workingDirectory instanceof CompressedWorkingDirectory) {
                if (!((CompressedWorkingDirectory) workingDirectory)
                        .getTargetZipFile().canWrite())
                    forceReadOnly = true;
            }
        }

        if (indivMode && proj.getBoolUserSetting(MEMBERS_CANNOT_EDIT_SETTING))
            forceReadOnly = true;

        if (forceReadOnly)
            proj.setReadOnly(true);

        if (owner == null && !forceReadOnly)
            owner = getOwnerName();

        if (!indivMode)
            initials = null;

        try {
            WBSEditor w = new WBSEditor(workingDirectory, proj, owner, initials);
            w.setExitOnClose(exitOnClose);
            w.setSyncURL(syncURL);
            w.setIndivMode(indivMode);
            if (showTeamList) {
                w.showTeamListEditorWithSaveButton();
            } else {
                w.show();
                w.showApplicableStartupMessages();
            }

            if (dispatch != null)
                dispatch.setEditor(w);
            waitFrame.dispose();
            return w;
        } catch (LockFailureException e) {
            workingDirectory.releaseLocks();
            if (exitOnClose)
                System.exit(0);
            waitFrame.dispose();
            return null;
        }
    }

    private static WorkingDirectory configureWorkingDirectory(String[] locations,
            String intent, LockMessageHandler handler) {
        DashboardBackupFactory.setKeepBackupsNumDays(30);
        WorkingDirectory workingDirectory = WorkingDirectoryFactory
                .getInstance().get(WorkingDirectoryFactory.PURPOSE_WBS,
                    locations);

        try {
            workingDirectory.acquireProcessLock(intent, handler);
        } catch (SentLockMessageException s) {
            // another WBS Editor is running, and it handled the request for us.
            maybeDumpStartupError("Process Conflict", new Object[] { //
                "Another process on this computer has already locked",
                "the WBS Editor for this team project." });
            return null;
        } catch (LockFailureException e) {
            e.printStackTrace();
            showLockFailureError();
            return null;
        }

        boolean workingDirIsGood = false;
        try {
            workingDirectory.prepare();
            File dir = workingDirectory.getDirectory();
            if (workingDirectory instanceof CompressedWorkingDirectory) {
                workingDirIsGood = new File(dir, WBS_FILENAME).isFile();
            } else {
                workingDirIsGood = dir.isDirectory();
            }
        } catch (IOException e) {
            // do nothing.  An exception means that "workingDirIsGood" will
            // remain false, so we will display an error message below.
        }

        if (workingDirIsGood) {
            return workingDirectory;
        }
        else {
            showCannotOpenError(workingDirectory);
            return null;
        }
    }

    private static void showLockFailureError() {
        showCannotOpenError("Lock", null);
    }
    private static void showCannotOpenError(WorkingDirectory workingDirectory) {
        showCannotOpenError(workingDirResKey(workingDirectory),
            workingDirectory.getDescription());
    }
    private static void showCannotOpenError(String location) {
        showCannotOpenError(workingDirResKey(location), location);
    }
    private static void showCannotOpenError(String resKey, String description) {
        String title = resources.getString("Errors.Cannot_Open.Title");
        String[] message = resources.formatStrings("Errors.Cannot_Open."
                + resKey + "_FMT", description);
        maybeDumpStartupError(title, message);
        JOptionPane.showMessageDialog(null, message, title,
            JOptionPane.ERROR_MESSAGE);
    }
    private static void maybeDumpStartupError(String title, Object[] message) {
        if (isDumpAndExitMode()) {
            System.err.println(title);
            for (Object line : message)
                System.err.println(line);
            System.exit(1);
        }
    }

    String workingDirResKey() {
        return workingDirResKey(workingDirectory);
    }

    private static String workingDirResKey(String location) {
        if (location.startsWith("http")) return "Server";
        else if (location.endsWith(".zip")) return "Zip";
        else return "Dir";
    }

    private static String workingDirResKey(Object workingDirectory) {
        if (workingDirectory instanceof BridgedWorkingDirectory)
            return "Server";
        else if (workingDirectory instanceof CompressedWorkingDirectory)
            return "Zip";
        else
            return "Dir";
    }

    private static class LockMessageDispatcher implements LockMessageHandler {

        private List<LockMessage> missedMessages = new ArrayList<LockMessage>();

        private WBSEditor editor = null;

        public synchronized void setEditor(WBSEditor editor) {
            this.editor = editor;
            for (LockMessage msg : missedMessages) {
                editor.handleMessage(msg);
            }
        }

        public synchronized String handleMessage(LockMessage e) throws Exception {
            if (editor != null) {
                return editor.handleMessage(e);
            } else {
                missedMessages.add(e);
                return "OK";
            }
        }

    }

    private static String getOwnerName() {
        String result = preferences.get("ownerName", null);
        if (result == null && isDumpAndExitMode())
            result = "(Batch Process)";
        if (result == null) {
            result = JOptionPane.showInputDialog(null, INPUT_NAME_MESSAGE,
                    "Enter Your Name", JOptionPane.PLAIN_MESSAGE);
            if (result != null)
                preferences.put("ownerName", result);
        }
        return result;
    }
    private static final String INPUT_NAME_MESSAGE =
        "To open the Work Breakdown Structure, please enter your name:";

    private String getExpandedNodesKey(String projectId) {
        return projectId + EXPANDED_NODES_KEY_SUFFIX;
    }

    private Set getExpandedNodesPref(String projectId) {
        String value = PreferencesUtils.getCLOB(preferences,
                getExpandedNodesKey(projectId), null);
        if (value == null)
            return null;

        String[] nodesArray = value.split(EXPANDED_NODES_DELIMITER);
        Set nodesToExpand = new HashSet(Arrays.asList(nodesArray));

        return nodesToExpand;
    }

    private void setExpandedNodesPref(String projectId, Set value) {
        PreferencesUtils.putCLOB(preferences, getExpandedNodesKey(projectId),
                StringUtils.join(value, EXPANDED_NODES_DELIMITER));
    }

    private String getInsertOnEnterKey(String projectId) {
        return projectId + "_InsertOnEnter";
    }

    private boolean getInsertOnEnterPref(String projectId) {
        return preferences.getBoolean(getInsertOnEnterKey(projectId), true);
    }

    private void setInsertOnEnterPref(String projectId, boolean value) {
        preferences.putBoolean(getInsertOnEnterKey(projectId), value);
    }

    private boolean getOptimizeForIndivPref() {
        return preferences.getBoolean(OPTIMIZE_FOR_INDIV_KEY, true);
    }

    private void setOptimizeForIndivPref(boolean value) {
        preferences.putBoolean(OPTIMIZE_FOR_INDIV_KEY, value);
    }

    private static boolean isDumpAndExitMode() {
        return Boolean.getBoolean("teamdash.wbs.dumpAndExit");
    }

    private void setDirty(boolean isDirty) {
        this.dirty = isDirty;

        if (frame != null) {
            MacGUIUtils.setDirty(frame, isDirty);
        }

        if (this.saveAction != null) {
            this.saveAction.setEnabled(!readOnly && isDirty());
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    private void initializeChangeHistory() {
        changeHistory = new ChangeHistory(changeHistoryFile);

        if (isNotZipWorkingDirectory()) {
            // If some team members open the WBS with an older version of the
            // WBS Editor, change history entries will not be generated when
            // they save. On startup, check for this condition and assign a new
            // change history UID to the current state of the WBS.
            //
            // (Note that we don't need to test for this with a
            // CompressedWorkingDirectory because only the new version of the
            // WBS Editor can open those;  and because the nature of the
            // "Save As" operation often creates file modification timestamps
            // that are out of order.)
            Entry lastChangeHistoryEntry = changeHistory.getLastEntry();
            long lastChangeHistoryTime = (lastChangeHistoryEntry == null ? 0
                    : lastChangeHistoryEntry.getTimestamp().getTime());
            long lastFileModTime = teamProject.getFileModificationTime();
            long diff = lastFileModTime - lastChangeHistoryTime;
            if (diff > 6000)
                changeHistory.addEntry("Various individuals");
        }
    }


    public static void main(String args[]) {
        if (isDumpAndExitMode())
            System.setProperty("java.awt.headless", "true");

        ExternalLocationMapper.getInstance().loadDefaultMappings();
        RuntimeUtils.autoregisterPropagatedSystemProperties();
        for (String prop : PROPS_TO_PROPAGATE)
            RuntimeUtils.addPropagatedSystemProperty(prop, null);

        String[] locations = args;

        boolean bottomUp = Boolean.getBoolean("teamdash.wbs.bottomUp");
        boolean indivMode = Boolean.getBoolean("teamdash.wbs.indiv");
        String indivInitials = System.getProperty("teamdash.wbs.indivInitials");
        boolean showTeam = Boolean.getBoolean("teamdash.wbs.showTeamMemberList");
        boolean readOnly = Boolean.getBoolean("teamdash.wbs.readOnly");
        String syncURL = System.getProperty("teamdash.wbs.syncURL");
        String owner = System.getProperty("teamdash.wbs.owner");
        try {
            createAndShowEditor(locations, bottomUp, indivMode, indivInitials,
                showTeam, syncURL, true, readOnly, owner);
        } catch (HeadlessException he) {
            he.printStackTrace();
            System.exit(1);
        }

        new Timer(DAY_MILLIS, new UsageLogAction()).start();
        maybeNotifyOpened();
    }

    private static final String[] PROPS_TO_PROPAGATE = {
        TeamServerSelector.DEFAULT_TEAM_SERVER_PROPERTY,
        CustomColumnManager.SYS_PROP_NAME,
        "teamdash.wbs.owner"
    };

    private static void maybeNotifyOpened() {
        Integer portToNotify = Integer.getInteger(NOTIFY_ON_OPEN_PORT_PROPERTY);
        if (portToNotify != null) {
            try {
                Socket s = new Socket(InetAddress.getLocalHost(), portToNotify
                        .intValue());
                Writer out = new OutputStreamWriter(s.getOutputStream(),
                        "UTF-8");
                out.write("<?xml version='1.0' encoding='UTF-8'?>");
                out.write("<pdashNotification");
                out.write(" instanceId='"
                        + System.getProperty(NOTIFY_ON_OPEN_ID_PROPERTY));
                out.write("'>");
                out.write("<event type='opened'/>");
                out.write("</pdashNotification>");
                out.close();
                s.close();
            } catch (Exception e) {}
        }
    }
    private static final String NOTIFY_ON_OPEN_PREFIX = "net.sourceforge.processdash.ProcessDashboard.notifyOnOpen.";
    private static final String NOTIFY_ON_OPEN_PORT_PROPERTY = NOTIFY_ON_OPEN_PREFIX + "port";
    private static final String NOTIFY_ON_OPEN_ID_PROPERTY = NOTIFY_ON_OPEN_PREFIX + "id";

    private static class UsageLogAction implements ActionListener {
        private UsageLogger logger;
        private UsageLogAction() {
            logger = new UsageLogger();
            logger.run();
        }
        public void actionPerformed(ActionEvent e) {
            logger.run();
        }
    }

    private static final int DAY_MILLIS = 24 /*hours*/ * 60 /*minutes*/
            * 60 /*seconds*/ * 1000 /*millis*/;

    private class SaveAction extends AbstractAction {
        private boolean firstSave;
        public SaveAction() {
            super("Save");
            putValue(MNEMONIC_KEY, new Integer('S'));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, //
                MacGUIUtils.getCtrlModifier()));
            setEnabled(!readOnly && isDirty());
            firstSave = true;
        }
        public void actionPerformed(ActionEvent e) {
            save();

            if (firstSave) {
                maybeShowProjectClosedMessage();
                firstSave = false;
            }
        }
    }

    private class RefreshAction extends AbstractAction {
        public RefreshAction() {
            super(resources.getString("File_Refresh.Menu"));
            putValue(MNEMONIC_KEY, new Integer('R'));
            putValue(SHORT_DESCRIPTION, resources
                    .getString("File_Refresh.Tooltip"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, //
                MacGUIUtils.getCtrlModifier()));
        }
        public synchronized void actionPerformed(ActionEvent e) {
            new RefreshWorker().doRefresh();
        }
    }

    private class RefreshWorker implements Runnable {

        boolean done;
        JDialog dialog;
        boolean dataWasMerged;
        Exception mergeException;

        public void doRefresh() {
            stopAllCellEditingSessions();

            synchronized (this) {
                // start the work.
                done = false;
                new Thread(this).start();

                // if no merge is needed, the work could possibly complete in a
                // fraction of a second. Pause a moment to see if that happens.
                // (Swing paint ops will freeze for this duration, so we choose
                // not to wait very long.)
                try {
                    wait(200);
                } catch (InterruptedException ie) {}

                // if the work isn't done yet, create a "please wait" dialog.
                if (!done)
                    dialog = createWaitDialog(frame, resources
                        .getString("File_Refresh.Wait_Message"));
            }

            if (dialog != null)
                // this will block until the work is done
                dialog.setVisible(true);

            showResults();
        }

        public void run() {
            long start = System.currentTimeMillis();
            try {
                // do the work.
                dataWasMerged = mergeExternalChanges();
            } catch (Exception e) {
                mergeException = e;
            }
            synchronized (this) {
                done = true;
                notifyAll();
            }

            // flashing windows are annoying to the user. Pause for a moment to
            // ensure that the wait dialog is displayed for at least a second.
            long elapsed = System.currentTimeMillis() - start;
            long sleep = Math.max(100, 1000 - elapsed);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {}

            synchronized (this) {
                if (dialog != null)
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { dialog.dispose(); }});
            }
        }

        private void showResults() {
            dialog = null;

            if (mergeException instanceof IOException) {
                String title = resources.getString("Errors.Cannot_Refresh.Title");
                String[] message = resources.formatStrings(
                    "Errors.Cannot_Refresh." + workingDirResKey() + "_FMT",
                    workingDirectory.getDescription());
                JOptionPane.showMessageDialog(frame, message, title,
                    JOptionPane.ERROR_MESSAGE);

            } else if (mergeException != null) {
                String title = resources.getString("Errors.Cannot_Refresh.Title");
                String[] message = resources
                        .getStrings("Errors.Cannot_Refresh.Internal_Error");
                Object debugZipMsg = getDebugZipMessage(
                    "Errors.Cannot_Refresh.Internal_Error_File_FMT",
                    mergeException);
                ExceptionDialog.show(frame, title, message, mergeException,
                    debugZipMsg);

            } else if (dataWasMerged == false) {
                JOptionPane.showMessageDialog(frame,
                    resources.getStrings("File_Refresh.No_Merge_Message"),
                    resources.getString("File_Refresh.Title"),
                    JOptionPane.PLAIN_MESSAGE);

            } else {
                showMergeFollowUpInfo("File_Refresh.Title", true);
            }
        }
    }

    private class SaveMergeDebugZipAction extends AbstractAction {
        public SaveMergeDebugZipAction() {
            super("Save Simultaneous Editing Debug File");
        }
        public void actionPerformed(ActionEvent e) {
            File zip = mergeDebugger.makeZipOfAllMerges();
            Object message = new Object[] {
                    "The following debug file has been created, recording the",
                    "contents of your team plan and the history of all changes",
                    "made by you and others during the current editing session:",
                    "        " + zip,
                    " ",
                    "If the simultaneous editing feature is not behaving as",
                    "you would expect, please send this file to the Process",
                    "Dashboard development team along with a description of",
                    "the unusual behavior you observed."
            };
            JOptionPane.showMessageDialog(frame, message, "Debug File Saved",
                JOptionPane.PLAIN_MESSAGE);
        }
    }

    private class ImportFromCsvAction extends AbstractAction {
        public ImportFromCsvAction() {
            super("Import from MS Project CSV file...");
            putValue(MNEMONIC_KEY, new Integer('I'));
            setEnabled(readOnly == false);
        }

        public void actionPerformed(ActionEvent e) {
            CsvNodeDataImporterUI ui = new CsvNodeDataImporterUI();
            ui.run(tabPanel.wbsTable, teamProject.getTeamMemberList());
        }
    }

    private class CloseAction extends AbstractAction {
        public CloseAction() {
            super("Close");
            putValue(MNEMONIC_KEY, new Integer('C'));
        }

        public void actionPerformed(ActionEvent e) {
            maybeClose();
        }
    }

    private class EditPreferencesAction extends AbstractAction {
        public EditPreferencesAction() {
            super("Preferences");
        }

        public void actionPerformed(ActionEvent e) {
            showEditPreferencesDialog();
        }
    }

    private class WorkflowEditorAction extends AbstractAction {
        public WorkflowEditorAction() {
            super("Edit Workflows");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }
        public void actionPerformed(ActionEvent e) {
            showWorkflowEditor();
        }
    }

    private class WorkflowMenuBuilder implements TableModelListener {
        private JMenu menu;
        private int initialMenuLength;
        private WBSModel workflows;
        private Action insertWorkflowAction;
        private ArrayList itemList;

        public WorkflowMenuBuilder(JMenu menu, WBSModel workflows,
                                   Action insertWorkflowAction) {
            this.menu = menu;
            this.initialMenuLength = menu.getItemCount();
            this.workflows = workflows;
            this.insertWorkflowAction = insertWorkflowAction;
            this.itemList = new ArrayList();
            rebuildMenu();
            workflows.addTableModelListener(this);
        }

        private void rebuildMenu() {
            ArrayList newList = new ArrayList();
            WBSNode[] workflowItems =
                workflows.getChildren(workflows.getRoot());
            for (int i = 0;   i < workflowItems.length;   i++) {
                String workflowName = workflowItems[i].getName();
                if (!newList.contains(workflowName))
                    newList.add(workflowName);
            }

            synchronized (menu) {
                if (newList.equals(itemList)) return;

                while (menu.getItemCount() > initialMenuLength)
                    menu.remove(initialMenuLength);
                Iterator i = newList.iterator();
                while (i.hasNext()) {
                    String workflowItemName = (String) i.next();
                    JMenuItem menuItem = new JMenuItem(insertWorkflowAction);
                    menuItem.setActionCommand(workflowItemName);
                    menuItem.setText(workflowItemName);
                    menu.add(menuItem);
                }

                itemList = newList;
            }
        }

        public void tableChanged(TableModelEvent e) {
            rebuildMenu();
        }
    }


    private class MilestonesEditorAction extends AbstractAction {
        public MilestonesEditorAction() {
            super("Edit Milestones");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }

        public void actionPerformed(ActionEvent e) {
            showMilestonesEditor();
        }
    }

    private class ShowCommitDatesMenuItem extends CheckBoxMenuItem implements
            ChangeListener {
        public ShowCommitDatesMenuItem() {
            super("Show Commit Dates on Balancing Panel");
            setSelected(true);
            addChangeListener(this);
            load("teamTimePanel.showCommitDates");
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setShowCommitDates(getState());
        }
    }

    private class ShowMilestoneMarksMenuItem extends CheckBoxMenuItem
            implements ChangeListener {
        public ShowMilestoneMarksMenuItem() {
            super("Show Milestone Marks for Team Members");
            setSelected(true);
            addChangeListener(this);
            load("teamTimePanel.showMilestoneMarks");
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setShowMilestoneMarks(getState());
        }
    }

    private class BalanceMilestoneMenuBuilder implements TableModelListener,
            ActionListener {
        private JMenu menu;
        private int initialMenuLength;
        private WBSModel milestonesWbs;
        private int selectedMilestoneID;
        private ButtonGroup group;
        private Border indentBorder;

        public BalanceMilestoneMenuBuilder(JMenu menu, WBSModel milestones) {
            this.menu = menu;
            menu.add(new JMenuItem("Balance Work Through:"));
            this.initialMenuLength = menu.getItemCount();
            this.milestonesWbs = milestones;
            this.selectedMilestoneID = -1;
            rebuildMenu();
            milestones.addTableModelListener(this);

        }

        private void rebuildMenu() {
            WBSNode[] milestones =
                milestonesWbs.getChildren(milestonesWbs.getRoot());

            synchronized (menu) {
                while (menu.getItemCount() > initialMenuLength)
                    menu.remove(initialMenuLength);

                group = new ButtonGroup();
                addMenuItem("( Entire WBS )", -1);
                for (WBSNode milestone : milestones) {
                    String name = milestone.getName();
                    int uniqueID = milestone.getUniqueID();
                    if (name != null && name.trim().length() > 0)
                        addMenuItem(name, uniqueID);
                }
            }
        }

        private void addMenuItem(String name, int uniqueID) {
            JMenuItem menuItem = new JRadioButtonMenuItem(name);
            menuItem.setActionCommand(Integer.toString(uniqueID));
            group.add(menuItem);
            if (uniqueID == selectedMilestoneID || uniqueID == -1)
                menuItem.setSelected(true);

            if (indentBorder == null)
                indentBorder = BorderFactory.createCompoundBorder(
                    menuItem.getBorder(), new EmptyBorder(0, 15, 0, 0));
            menuItem.setBorder(indentBorder);

            if (uniqueID == -1)
                menuItem.setFont(menuItem.getFont().deriveFont(
                    Font.ITALIC + Font.BOLD));

            menuItem.addActionListener(this);
            menu.add(menuItem);
        }


        public void tableChanged(TableModelEvent e) {
            rebuildMenu();
        }

        public void actionPerformed(ActionEvent e) {
            try {
                String newSelection = e.getActionCommand();
                selectedMilestoneID = Integer.parseInt(newSelection);
                teamTimePanel.setBalanceThroughMilestone(selectedMilestoneID);
                if (showTeamTimePanelMenuItem != null)
                    showTeamTimePanelMenuItem.setSelected(true);
            } catch (Exception ex) {}
        }
    }



    private class ShowTeamMemberListEditorMenuItem extends AbstractAction {
        public ShowTeamMemberListEditorMenuItem() {
            super("Edit Team Member List");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }
        public void actionPerformed(ActionEvent e) {
            showTeamListEditor();
        }
    }


    private class OptimizeEditingForIndivMenuItem extends JCheckBoxMenuItem
            implements ChangeListener, InitialsListener {
        private String initials;
        private WatchCoworkerTimesMenuItem watchMenu;
        public OptimizeEditingForIndivMenuItem(TeamMember t,
                WatchCoworkerTimesMenuItem watchMenu) {
            super("Optimize Edit Operations for: " + t.getName());
            this.initials = t.getInitials();
            this.watchMenu = watchMenu;
            setSelected(getOptimizeForIndivPref());
            updateDependentObjects();
            addChangeListener(this);
            teamProject.getTeamMemberList().addInitialsListener(this);
        }
        public void stateChanged(ChangeEvent e) {
            // this method is called when the user selects this menu option
            // and toggles the state of our checkbox.
            updateDependentObjects();
            setOptimizeForIndivPref(isSelected());
        }
        public void initialsChanged(String oldInitials, String newInitials) {
            // this method is called when someone alters the initials of an
            // individual in the team member list.
            if (this.initials.equalsIgnoreCase(oldInitials)) {
                this.initials = newInitials;
                updateDependentObjects();
            }
        }
        private void updateDependentObjects() {
            String optimizeFor = (isSelected() ? this.initials : null);
            tabPanel.wbsTable.setOptimizeForIndiv(optimizeFor);
            teamProject.getTeamMemberList().setOnlyEditableFor(optimizeFor);
            if (teamListEditor != null)
                teamListEditor.setOnlyEditableFor(optimizeFor);
            watchMenu.update(this.initials, isSelected());
        }
    }


    private class WatchCoworkerTimesMenuItem extends JCheckBoxMenuItem
            implements ChangeListener, PlanTimeDiscrepancyListener, Runnable {
        PlanTimeWatcher watcher;
        String initials;
        List<String> coworkerDiscrepancies;
        int invokeLaterCount;
        public WatchCoworkerTimesMenuItem(DataTableModel dataModel) {
            super(resources.getString("PlanWatcher.Menu"));
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));

            int watcherCol = dataModel.findColumn(PlanTimeWatcher.COLUMN_ID);
            watcher = (PlanTimeWatcher) dataModel.getColumn(watcherCol);
            watcher.addPlanTimeDiscrepancyListener(this);

            setSelected(true);
            addChangeListener(this);
        }
        public void stateChanged(ChangeEvent e) {
            updateDependentObjects();
        }
        void update(String initials, boolean enabled) {
            this.initials = initials;
            setEnabled(enabled);
            updateDependentObjects();
        }
        private void updateDependentObjects() {
            boolean active = isSelected() && isEnabled();
            String restriction = (active ? initials : null);
            watcher.setRestrictTo(restriction);
        }
        public void discrepancyNoted(PlanTimeDiscrepancyEvent e) {
            this.coworkerDiscrepancies = e.getDiscrepantInitials();
            this.invokeLaterCount = 3;
            SwingUtilities.invokeLater(this);
        }
        public void run() {
            if (--invokeLaterCount > 0) {
                SwingUtilities.invokeLater(this);
            } else {
                showWarning();
            }
        }
        private void showWarning() {
            List message = new ArrayList();
            if (coworkerDiscrepancies.size() == 1) {
                String oneInitial = coworkerDiscrepancies.get(0);
                message.add(resources.formatStrings(
                    "PlanWatcher.Warn_Single_FMT", getCoworkerName(oneInitial)));
            } else {
                message.add(resources.getStrings("PlanWatcher.Warn_Multiple"));
                for (String oneInitial : coworkerDiscrepancies) {
                    message.add("      " + getCoworkerName(oneInitial));
                }
            }
            message.add(" ");
            message.add(resources.getString("PlanWatcher.Prompt"));
            message.add(" ");
            message.add(createHelpLabel());

            int userChoice = JOptionPane.showOptionDialog(frame, message
                    .toArray(), resources.getString("PlanWatcher.Title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[] { UNDO_CHANGE, KEEP_CHANGE }, UNDO_CHANGE);
            if (userChoice == 0) {
                tabPanel.undoList.undo();
            }
        }
        private String getCoworkerName(String initials) {
            TeamMember coworker = teamProject.getTeamMemberList()
                .findTeamMember(initials);
            if (coworker != null)
                return coworker.getName();
            else
                // shouldn't happen!
                return resources.format("Initials_FMT", initials);
        }
        private String UNDO_CHANGE = resources.getString("PlanWatcher.Undo");
        private String KEEP_CHANGE = resources.getString("PlanWatcher.Keep");

        private Component createHelpLabel() {
            final JLabel helpLabel = new JLabel(
                    resources.getString("PlanWatcher.Help.Prompt"),
                    IconFactory.getHelpIcon(), SwingConstants.CENTER);
            Font f = helpLabel.getFont();
            helpLabel.setFont(f.deriveFont(f.getSize2D() * 0.8f));
            helpLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            helpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            helpLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    showExplanation();
                }});

            return new JOptionPaneTweaker() {
                public void doTweak(JDialog dialog) {
                    dialog.getContentPane().add(helpLabel, BorderLayout.SOUTH);
                }};
        }
        protected void showExplanation() {
            JOptionPane.showMessageDialog(frame,
                resources.getStrings("PlanWatcher.Help.Explanation"),
                resources.getString("PlanWatcher.Help.Title"),
                JOptionPane.INFORMATION_MESSAGE);
        }
    }


    private class ShowTeamTimePanelMenuItem extends CheckBoxMenuItem
    implements ChangeListener {
        public ShowTeamTimePanelMenuItem() {
            super("Show Bottom Up Time Panel");
            setMnemonic('B');
            setSelected(teamTimePanel.isVisible());
            addChangeListener(this);
            showTeamTimePanelMenuItem = this;
            load("teamTimePanel.showPanel");
        }
        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setVisible(getState());
            frame.invalidate();
        }
    }
    private ShowTeamTimePanelMenuItem showTeamTimePanelMenuItem;

    private class BottomUpShowReplanMenuItem extends JRadioButtonMenuItem
    implements ChangeListener {
        public BottomUpShowReplanMenuItem (ButtonGroup buttonGroup) {
            super("Colored Bars Show Remaining Work (Replan)");
            setMnemonic('R');
            setDisplayedMnemonicIndex(getText().indexOf('R'));
            setSelected(true);
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
            buttonGroup.add(this);
            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setShowRemainingWork(isSelected());
        }
    }

    private class BottomUpShowPlanMenuItem extends JRadioButtonMenuItem {
        public BottomUpShowPlanMenuItem (ButtonGroup buttonGroup) {
            super("Colored Bars Show End-to-End Plan");
            setMnemonic('P');
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
            buttonGroup.add(this);
        }
    }

    private class BottomUpShowHoursPerWeekMenuItem extends CheckBoxMenuItem
            implements ChangeListener {
        public BottomUpShowHoursPerWeekMenuItem() {
            super("Show Individual Hours Per Week");
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
            addChangeListener(this);
            load("teamTimePanel.showHoursPerWeek");
        }
        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setShowHoursPerWeek(getState());
        }
    }

    private class BottomUpIncludeUnassignedMenuItem extends CheckBoxMenuItem
            implements ChangeListener {
        public BottomUpIncludeUnassignedMenuItem() {
            super("Include Unassigned Effort in Balanced Team Calculation");
            setSelected(true);
            setBorder(BorderFactory.createCompoundBorder(getBorder(),
                new EmptyBorder(0, 15, 0, 0)));
            addChangeListener(this);
            load("teamTimePanel.includeUnassignedTime");
        }

        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setIncludeUnassigned(isSelected());
        }
    }

    private abstract class CheckBoxMenuItem extends JCheckBoxMenuItem {
        public CheckBoxMenuItem(String name) {
            super(name);
        }
        protected void load(String prefsKey) {
            guiPrefs.load(prefsKey, getModel());
        }
    }

    private class DirtyListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            setDirty(true);
        }
    }

    public void itemSaved(Object item) {
        if (item == teamListEditor) {
            setDirty(true);

            if (!frame.isVisible()) {
                // The frame containing the WBSEditor itself is not visible.
                // Thus, the user must have just opened the team member list
                // only, edited, and clicked the save button. It will fall to us
                // to save files on the team member list's behalf.  Since they
                // might have edited initials (which alters the data model),
                // we need to save the entire team project, not just the team
                // member list.  It is safe to do this without asking, because
                // the WBS and workflows have never been displayed, so they must
                // not have been otherwise altered. Finally, since no GUI
                // windows will be visible anymore, we should exit.
                save();
                shutDown();
            }
        }
    }

    public void itemCancelled(Object item) {
        if (item == teamListEditor) {
            if (!frame.isVisible())
                shutDown();
        }
    }

}
