package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.PreferencesUtils;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.ConcurrencyLock;
import teamdash.DirectoryBackup;
import teamdash.SaveListener;
import teamdash.TeamMemberListEditor;
import teamdash.wbs.columns.SizeAccountingColumnSet;

public class WBSEditor implements WindowListener, SaveListener,
        ConcurrencyLock.Listener {

    public static final String INTENT_WBS_EDITOR = "showWbsEditor";
    public static final String INTENT_TEAM_EDITOR = "showTeamListEditor";

    ConcurrencyLock concurrencyLock;
    TeamProject teamProject;
    JFrame frame;
    WBSTabPanel tabPanel;
    TeamTimePanel teamTimePanel;
    WBSDataWriter dataWriter;
    File dataDumpFile;
    WBSDataWriter workflowWriter;
    File workflowDumpFile;
    DirectoryBackup teamProjectBackup;
    private String owner;
    private int mode;
    boolean readOnly = false;
    boolean exitOnClose = false;
    String syncURL = null;
    boolean disposed = false;

    private TeamMemberListEditor teamListEditor = null;
    private WorkflowEditor workflowEditor = null;

    private static final int MODE_PLAIN = 1;
    private static final int MODE_HAS_MASTER = 2;
    private static final int MODE_MASTER = 4;
    private static final int MODE_BOTTOM_UP = 8;

    private static Preferences preferences = Preferences.userNodeForPackage(WBSEditor.class);
    private static final String EXPANDED_NODES_KEY_SUFFIX = "_EXPANDEDNODES";
    private static final String EXPANDED_NODES_DELIMITER = Character.toString('\u0001');

    public WBSEditor(TeamProject teamProject, File dumpFile, File workflowFile,
            String intent, String owner)
            throws ConcurrencyLock.FailureException {

        this.teamProject = teamProject;
        acquireLock(intent, owner);

        this.dataDumpFile = dumpFile;
        this.workflowDumpFile = workflowFile;
        this.readOnly = teamProject.isReadOnly();

        setMode(teamProject);

        WBSModel model = teamProject.getWBS();

        // set expanded nodes on model based on saved user preferences
        Set expandedNodes = getExpandedNodesPref(teamProject.getProjectID());
        if (expandedNodes != null) {
            model.setExpandedNodeIDs(expandedNodes);
        }

        TaskDependencySource taskDependencySource = getTaskDependencySource();
        DataTableModel data = new DataTableModel
            (model, teamProject.getTeamMemberList(),
             teamProject.getTeamProcess(), taskDependencySource);

        dataWriter = new WBSDataWriter(model, data,
                teamProject.getTeamProcess(), teamProject.getProjectID(),
                teamProject.getTeamMemberList());
        workflowWriter = new WBSDataWriter(teamProject.getWorkflows(), null,
                teamProject.getTeamProcess(), teamProject.getProjectID(), null);
        if (!readOnly) {
            teamProjectBackup = new DirectoryBackup(teamProject
                    .getStorageDirectory(), "backup", TeamProject.FILE_FILTER);
            teamProjectBackup.cleanupOldBackups(30);
            try {
                teamProjectBackup.backup("startup");
            } catch (IOException e) {}
        }
        this.owner = owner;

        tabPanel = new WBSTabPanel(model, data, teamProject.getTeamProcess(),
                taskDependencySource);
        tabPanel.setReadOnly(readOnly);
        teamProject.getTeamMemberList().addInitialsListener(tabPanel);

        String[] sizeMetrics = teamProject.getTeamProcess().getSizeMetrics();
        String[] sizeTabColIDs = new String[sizeMetrics.length+2];
        String[] sizeTabColNames = new String[sizeMetrics.length+2];
        sizeTabColIDs[0] = "Size";       sizeTabColNames[0] = "Size";
        sizeTabColIDs[1] = "Size-Units"; sizeTabColNames[1] = "Units";
        for (int i = 0; i < sizeMetrics.length; i++) {
            sizeTabColIDs[i+2] = SizeAccountingColumnSet.getNCID(sizeMetrics[i]);
            sizeTabColNames[i+2] = sizeMetrics[i];
        }
        tabPanel.addTab("Size", sizeTabColIDs, sizeTabColNames);

        tabPanel.addTab("Size Accounting",
                     new String[] { "Size-Units", "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" },
                     new String[] { "Units",  "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" });

        if (!isMode(MODE_MASTER))
            tabPanel.addTab("Time",
                     new String[] { "Time", WBSTabPanel.TEAM_MEMBER_TIMES_ID },
                     new String[] { "Team", "" });

        tabPanel.addTab("Task Time",
                new String[] { "Phase", "Task Size", "Task Size Units", "Rate",
                        ifMode(MODE_PLAIN, "Hrs/Indiv"),
                        ifMode(MODE_PLAIN, "# People"),
                        (isMode(MODE_MASTER) ? "TimeNoErr" : "Time"),
                        ifNotMode(MODE_MASTER, "Assigned To") },
                new String[] { "Phase/Type", "Task Size", "Units", "Rate",
                        "Hrs/Indiv", "# People", "Time", "Assigned To" });

        tabPanel.addTab("Task Details",
                new String[] { "Labels", "Dependencies" },
                new String[] { "Task Labels", "Task Dependencies" });

        //String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        //table.addTab("Defects", s, s);

        teamTimePanel =
            new TeamTimePanel(teamProject.getTeamMemberList(), data);
        teamTimePanel.setVisible(isMode(MODE_BOTTOM_UP));
        if (isMode(MODE_BOTTOM_UP))
            teamTimePanel.setShowBalancedBar(false);

        frame = new JFrame(teamProject.getProjectName()
                + " - Work Breakdown Structure"
                + (teamProject.isReadOnly() ? " (Read-Only)" : ""));
        frame.setJMenuBar(buildMenuBar(tabPanel, teamProject.getWorkflows()));
        frame.getContentPane().add(tabPanel);
        frame.getContentPane().add(teamTimePanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
    }

    private void acquireLock(String intent, String owner)
            throws ConcurrencyLock.FailureException {
        if (teamProject.isReadOnly())
            return;

        try {
            this.concurrencyLock = new ConcurrencyLock(teamProject
                    .getLockFile(), intent, this, owner);
        } catch (ConcurrencyLock.SentMessageException sme) {
            throw sme;
        } catch (ConcurrencyLock.FailureException e) {
            String otherOwner = null;
            if (e instanceof ConcurrencyLock.AlreadyLockedException)
                otherOwner = ((ConcurrencyLock.AlreadyLockedException) e)
                    .getExtraInfo();
            if (otherOwner == null)
                otherOwner = "someone on another machine";
            CONCURRENCY_MESSAGE[1] += (otherOwner + ".");

            int userResponse = JOptionPane.showConfirmDialog(null,
                    CONCURRENCY_MESSAGE, "Open Project in Read-Only Mode",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (userResponse == JOptionPane.YES_OPTION)
                teamProject.setReadOnly(true);
            else
                throw e;
        }
    }
    private static final String[] CONCURRENCY_MESSAGE = {
        "The Work Breakdown Structure for this project is currently",
        "open for editing by ",
        " ",
        "Would you like to open the project anyway, in read-only mode?"
    };

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

    private TaskDependencySource getTaskDependencySource() {
        if (isMode(MODE_PLAIN + MODE_HAS_MASTER))
            return new TaskDependencySourceMaster(teamProject);
        else
            return new TaskDependencySourceSimple(teamProject);
    }

    public void setExitOnClose(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    private void setSyncURL(String syncURL) {
        this.syncURL = syncURL;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void show() {
        frame.show();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public String handleMessage(String message) {
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
                    raiseWindow();
                }});
            return "OK";
        }
        if (ConcurrencyLock.LOCK_LOST_MESSAGE.equals(message)) {
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
        if (teamListEditor != null)
            teamListEditor.show();
        else {
            teamListEditor = new TeamMemberListEditor
                (teamProject.getProjectName(), teamProject.getTeamMemberList());
            teamListEditor.addSaveListener(this);
        }
    }

    public void showLostLockMessage() {
        readOnly = true;
        tabPanel.setReadOnly(true);
        saveAction.setEnabled(false);
        importFromCsvAction.setEnabled(false);

        JOptionPane.showMessageDialog(frame, LOST_LOCK_MESSAGE,
                "Network Connectivity Problems", JOptionPane.ERROR_MESSAGE);
    }
    private static final String[] LOST_LOCK_MESSAGE = {
        "Although you opened the work breakdown structure in read-write",
        "mode, your connection to the network was broken, and your lock",
        "on the WBS was lost.  In the meantime, another individual has",
        "opened the work breakdown structure for editing, so your lock",
        "could not be reclaimed.",
        " ",
        "As a result, you will no longer be able to save changes to the",
        "work breakdown structure.  You are strongly encouraged to close",
        "and reopen the work breakdown structure editor."
    };

    private void showWorkflowEditor() {
        if (workflowEditor != null)
            workflowEditor.show();
        else {
            workflowEditor = new WorkflowEditor(teamProject);
            //workflowEditor.addSaveListener(this);
        }
    }

    private JMenuBar buildMenuBar(WBSTabPanel tabPanel, WBSModel workflows) {
        JMenuBar result = new JMenuBar();

        result.add(buildFileMenu());
        result.add(buildEditMenu(tabPanel.getEditingActions()));
        result.add(buildTabMenu(tabPanel.getTabActions()));
        if (!isMode(MODE_BOTTOM_UP))
            result.add(buildWorkflowMenu
                (workflows, tabPanel.getInsertWorkflowAction(workflows)));
        if (isMode(MODE_HAS_MASTER))
            result.add(buildMasterMenu(tabPanel.getMasterActions(
                    teamProject.getMasterProjectDirectory())));
        if (!isMode(MODE_MASTER))
            result.add(buildTeamMenu());

        return result;
    }
    private Action saveAction, importFromCsvAction;
    private JMenu buildFileMenu() {
        JMenu result = new JMenu("File");
        result.setMnemonic('F');
        result.add(saveAction = new SaveAction());
        if (!isMode(MODE_BOTTOM_UP))
            result.add(importFromCsvAction = new ImportFromCsvAction());
        result.add(new CloseAction());
        return result;
    }
    private JMenu buildEditMenu(Action[] editingActions) {
        JMenu result = new JMenu("Edit");
        result.setMnemonic('E');
        for (int i = 0;   i < editingActions.length;   i++) {
            result.add(editingActions[i]);
            if (i == 1) result.addSeparator();
        }

        return result;
    }
    private JMenu buildTabMenu(Action[] tabActions) {
        JMenu result = new JMenu("Tabs");
        result.setMnemonic('A');
        for (int i = 0; i < tabActions.length; i++) {
            result.add(tabActions[i]);
            if (i == 2) result.addSeparator();
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
    private JMenu buildMasterMenu(Action[] masterActions) {
        JMenu result = new JMenu("Master");
        result.setMnemonic('M');
        for (int i = 0;   i < masterActions.length;   i++)
            result.add(masterActions[i]);
        return result;
    }
    private JMenu buildTeamMenu() {
        JMenu result = new JMenu("Team");
        result.setMnemonic('T');
        if (isMode(MODE_PLAIN))
            result.add(new ShowTeamMemberListEditorMenuItem());
        result.add(new ShowTeamTimePanelMenuItem());
        return result;
    }

    private boolean save() {
        if (!readOnly) {
            tabPanel.stopCellEditing();

            try {
                concurrencyLock.assertValidity();
            } catch (ConcurrencyLock.LockUncertainException lue) {
                showSaveErrorMessage();
                return false;
            } catch (ConcurrencyLock.FailureException fe) {
                showLostLockMessage();
                return false;
            }

            if (teamProject.save() == false || writeData() == false) {
                showSaveErrorMessage();
                return false;
            }

            maybeTriggerSyncOperation();
        }
        return true;
    }

    private boolean writeData() {
        if (!readOnly)
            try {
                dataWriter.write(dataDumpFile);
                workflowWriter.write(workflowDumpFile);

                String qualifier = "saved";
                if (owner != null && owner.trim().length() > 0)
                    qualifier = "saved_by_" + FileUtils.makeSafe(owner.trim());
                teamProjectBackup.backup(qualifier);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        return true;
    }

    private void showSaveErrorMessage() {
        SAVE_ERROR_MSG[4] = "      "
                + teamProject.getStorageDirectory().getAbsolutePath();
        JOptionPane.showMessageDialog(frame, SAVE_ERROR_MSG,
                "Unable to Save", JOptionPane.ERROR_MESSAGE);
    }

    private static final String[] SAVE_ERROR_MSG = {
        "The Work Breakdown Structure Editor encountered an unexpected error",
        "and was unable to save data. This problem might have been caused by",
        "poor network connectivity, or by read-only file permissions. Please",
        "check to ensure that you can write to the following location:",
        "",
        " ",
        "Then, try saving again. If you shut down the Work Breakdown Structure",
        "Editor without resolving this problem, any changes you have made will",
        "be lost."
    };

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
        if (maybeSave(true)) {
            // Set expanded nodes preference
            Set expandedNodes = teamProject.getWBS().getExpandedNodeIDs();
            setExpandedNodesPref(teamProject.getProjectID(), expandedNodes);

            if (exitOnClose)
                System.exit(0);
            else {
                if (teamListEditor != null) teamListEditor.hide();
                if (workflowEditor != null) workflowEditor.hide();
                frame.dispose();
                disposed = true;
                if (concurrencyLock != null)
                    concurrencyLock.unlock();
            }
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

    public static WBSEditor createAndShowEditor(String directory,
            boolean bottomUp, boolean showTeamList, String syncURL,
            boolean exitOnClose, boolean forceReadOnly, String owner) {
        File dir = new File(directory);
        File dumpFile = new File(dir, "projDump.xml");
        File workflowFile = new File(dir, "workflowDump.xml");
        TeamProject proj;
        if (bottomUp)
            proj = new TeamProjectBottomUp(dir, "Team Project");
        else
            proj = new TeamProject(dir, "Team Project");
        if (forceReadOnly)
            proj.setReadOnly(true);
        else if (checkProjectEditability(proj, dumpFile, workflowFile) == false)
            return null;

        String intent = showTeamList ? INTENT_TEAM_EDITOR : INTENT_WBS_EDITOR;
        if (owner == null && !forceReadOnly)
            owner = getOwnerName();

        try {
            WBSEditor w = new WBSEditor(proj, dumpFile, workflowFile, intent,
                    owner);
            w.setExitOnClose(exitOnClose);
            w.setSyncURL(syncURL);
            if (showTeamList)
                w.showTeamListEditor();
            else
                w.show();

            return w;
        } catch (ConcurrencyLock.FailureException e) {
            if (exitOnClose)
                System.exit(0);
            return null;
        }
    }

    private static boolean checkProjectEditability(TeamProject teamProject,
            File dumpFile, File workflowFile) {
        if (teamProject.filesAreReadOnly() == false
                && fileIsReadOnly(dumpFile) == false
                && fileIsReadOnly(workflowFile) == false)
            // all of the files for the project are editable.
            return true;

        READ_ONLY_FILES_MESSAGE[2] = "      "
            + teamProject.getStorageDirectory().getAbsolutePath();
        int userResponse = JOptionPane.showConfirmDialog(null,
                READ_ONLY_FILES_MESSAGE, "Open Project in Read-Only Mode",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (userResponse == JOptionPane.YES_OPTION) {
            teamProject.setReadOnly(true);
            return true;
        }

        return false;
    }
    private static boolean fileIsReadOnly(File file) {
        return (file.exists() && !file.canWrite());
    }
    private static final String[] READ_ONLY_FILES_MESSAGE = {
        "The Work Breakdown Structure Editor stores data for this project",
        "into XML files located in the following directory:",
        "",
        " ",
        "Unfortunately, the current filesystem permissions do not allow",
        "you to modify those files.  Would you like to open the project",
        "anyway, in read-only mode?"
    };

    private static String getOwnerName() {
        String result = preferences.get("ownerName", null);
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

    public static void main(String args[]) {
        String filename = ".";
        if (args.length > 0) {
            filename = args[0];
            if (new File(filename).isDirectory() == false) {
                showBadFilenameError(filename);
                return;
            }
        }

        boolean bottomUp = Boolean.getBoolean("teamdash.wbs.bottomUp");
        boolean showTeam = Boolean.getBoolean("teamdash.wbs.showTeamMemberList");
        boolean readOnly = Boolean.getBoolean("teamdash.wbs.readOnly");
        String syncURL = System.getProperty("teamdash.wbs.syncURL");
        String owner = System.getProperty("teamdash.wbs.owner");
        createAndShowEditor(filename, bottomUp, showTeam, syncURL, true,
                readOnly, owner);
    }

    private static void showBadFilenameError(String filename) {
        String[] message = new String[] {
                "The Work Breakdown Structure Editor attempted to open",
                "project data located in the directory:",
                "        " + filename,
                "Unfortunately, this directory could not be found.  You",
                "may need to map a network drive to edit this data." };
        JOptionPane.showMessageDialog(null, message,
                "Could not Open Project Files", JOptionPane.ERROR_MESSAGE);
    }

    private class SaveAction extends AbstractAction {
        public SaveAction() {
            super("Save");
            putValue(MNEMONIC_KEY, new Integer('S'));
            setEnabled(!readOnly);
        }
        public void actionPerformed(ActionEvent e) {
            save();
        }
    }

    private class ImportFromCsvAction extends AbstractAction {
        public ImportFromCsvAction() {
            super("Import from MS Project CSV file");
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





    private class ShowTeamMemberListEditorMenuItem extends AbstractAction {
        public ShowTeamMemberListEditorMenuItem() {
            super("Edit Team Member List");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }
        public void actionPerformed(ActionEvent e) {
            showTeamListEditor();
        }
    }


    private class ShowTeamTimePanelMenuItem extends JCheckBoxMenuItem
    implements ChangeListener {
        public ShowTeamTimePanelMenuItem() {
            super("Show Bottom Up Time Panel");
            setMnemonic('B');
            setSelected(teamTimePanel.isVisible());
            addChangeListener(this);
        }
        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setVisible(getState());
            frame.invalidate();
        }
    }

    public void itemSaved(Object item) {
        if (item == teamListEditor) {
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
                if (exitOnClose)
                    System.exit(0);
            }
        }
    }

    public void itemCancelled(Object item) {
        if (item == teamListEditor) {
            if (exitOnClose && !frame.isVisible())
                System.exit(0);
        }
    }

}
