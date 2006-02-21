package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import teamdash.SaveListener;
import teamdash.TeamMemberListEditor;

public class WBSEditor implements WindowListener, SaveListener {

    TeamProject teamProject;
    JFrame frame;
    TeamTimePanel teamTimePanel;
    WBSDataWriter dataWriter;
    File dataDumpFile;
    boolean exitOnClose = false;
    boolean disposed = false;

    private TeamMemberListEditor teamListEditor = null;
    private WorkflowEditor workflowEditor = null;


    public WBSEditor(TeamProject teamProject, File dumpFile) {
        this.teamProject = teamProject;
        this.dataDumpFile = dumpFile;

        WBSModel model = teamProject.getWBS();
        DataTableModel data = new DataTableModel
            (model, teamProject.getTeamMemberList(),
             teamProject.getTeamProcess());
        dataWriter = new WBSDataWriter(model, data, teamProject.getTeamProcess());
        WBSTabPanel table =
            new WBSTabPanel(model, data, teamProject.getTeamProcess());
        teamProject.getTeamMemberList().addInitialsListener(table);

        table.addTab("Size",
                     new String[] { "Size", "Size-Units", "N&C-LOC", "N&C-Text Pages",
                                    "N&C-Reqts Pages", "N&C-HLD Pages", "N&C-DLD Lines" },
                     new String[] { "Size", "Units", "LOC","Text Pages",
                                    "Reqts Pages", "HLD Pages", "DLD Lines" });

        table.addTab("Size Accounting",
                     new String[] { "Size-Units", "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" },
                     new String[] { "Units",  "Base", "Deleted", "Modified", "Added",
                                    "Reused", "N&C", "Total" });

        table.addTab("Time",
                     new String[] { "Time", WBSTabPanel.TEAM_MEMBER_TIMES_ID },
                     new String[] { "Team", "" });

        table.addTab("Task Time",
                     new String[] { "Phase", "Task Size", "Task Size Units", "Rate", "Hrs/Indiv", "# People", "Time", "Assigned To" },
                     new String[] { "Phase", "Task Size", "Units", "Rate", "Hrs/Indiv", "# People",
                         "Time", "Assigned To" });

        //String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        //table.addTab("Defects", s, s);

        teamTimePanel =
            new TeamTimePanel(teamProject.getTeamMemberList(), data);
        teamTimePanel.setVisible(false);

        frame = new JFrame
            (teamProject.getProjectName() + " - Work Breakdown Structure");
        frame.setJMenuBar(buildMenuBar(table, teamProject.getWorkflows()));
        frame.getContentPane().add(table);
        frame.getContentPane().add(teamTimePanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
    }

    public void setExitOnClose(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void show() {
        frame.show();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
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
        result.add(buildWorkflowMenu
            (workflows, tabPanel.getInsertWorkflowAction(workflows)));
        if (teamProject.getMasterProjectID() != null)
            result.add(buildMasterMenu(tabPanel.getMasterActions(
                    teamProject.getMasterProjectDirectory(),
                    teamProject.getMasterProjectID())));
        result.add(buildTeamMenu());

        return result;
    }
    private JMenu buildFileMenu() {
        JMenu result = new JMenu("File");
        result.setMnemonic('F');
        result.add(new SaveAction());
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
        result.add(new ShowTeamMemberListEditorMenuItem());
        result.add(new ShowTeamTimePanelMenuItem());
        return result;
    }

    private void save() {
        teamProject.save();
        writeData();
    }

    private void writeData() {
        try {
            dataWriter.write(dataDumpFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Give the user a chance to save data before the window closes.
     * 
     * @return false if the user selects cancel, true otherwise
     */
    private boolean maybeSave(boolean showCancel) {
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
                save();
                break;

            default:
                disposed = true;
                break;
        }

        return true;
    }

    protected void maybeClose() {
        if (maybeSave(true)) {
            if (exitOnClose)
                System.exit(0);
            else {
                if (teamListEditor != null) teamListEditor.hide();
                if (workflowEditor != null) workflowEditor.hide();
                frame.dispose();
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


    public static void main(String args[]) {
        String filename = ".";
        if (args.length > 0)
            filename = args[0];

        File dir = new File(filename);
        File dumpFile = new File(dir, "projDump.xml");
        WBSEditor w = new WBSEditor(new TeamProject(dir, "Team Project"), dumpFile);
        w.setExitOnClose(true);
        w.show();
    }

    private class SaveAction extends AbstractAction {
        public SaveAction() {
            super("Save");
            putValue(MNEMONIC_KEY, new Integer('S'));
        }
        public void actionPerformed(ActionEvent e) {
            save();
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
            addChangeListener(this);
        }
        public void stateChanged(ChangeEvent e) {
            teamTimePanel.setVisible(getState());
            frame.invalidate();
        }
    }

    public void itemSaved(Object item) {
        if (item == teamListEditor)
            teamProject.saveTeamList();
        else if (item == workflowEditor)
            teamProject.saveWorkflows();
    }

}
