package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import teamdash.TeamProject;

public class WBSEditor implements WindowListener {

    TeamProject teamProject;
    JFrame frame;
    TeamTimePanel teamTimePanel;


    public WBSEditor(TeamProject teamProject) {
        this.teamProject = teamProject;

        WBSModel model = teamProject.getWBS();
        DataTableModel data = new DataTableModel
            (model, teamProject.getTeamMemberList());
        WBSTabPanel table =
            new WBSTabPanel(model, data, teamProject.getTeamProcess());

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

        table.addTab("Time Calc",
                     new String[] { "Phase", "Size", "Size-Units", "Rate", "Hrs/Indiv", "# People", "Time" },
                     new String[] { "Phase", "Size", "Units", "Rate", "Hrs/Indiv", "# People",
                         "Time" });

        String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        table.addTab("Defects", s, s);

        teamTimePanel =
            new TeamTimePanel(teamProject.getTeamMemberList(), data);
        teamTimePanel.setVisible(false);

        frame = new JFrame
            (teamProject.getProjectName() + " - Work Breakdown Structure");
        frame.setJMenuBar(buildMenuBar(table));
        frame.getContentPane().add(table);
        frame.getContentPane().add(teamTimePanel, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
        frame.show();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private JMenuBar buildMenuBar(WBSTabPanel tabPanel) {
        JMenuBar result = new JMenuBar();

        result.add(buildFileMenu());
        result.add(buildEditMenu(tabPanel.getEditingActions()));
        result.add(buildWorkflowMenu());
        result.add(buildViewMenu());

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
    private JMenu buildWorkflowMenu() {
        JMenu result = new JMenu("Workflow");
        result.setMnemonic('W');
        result.add(new WorkflowEditorAction());
        return result;
    }
    private JMenu buildViewMenu() {
        JMenu result = new JMenu("View");
        result.setMnemonic('V');
        result.add(new ShowTeamTimePanelMenuItem());
        return result;
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {
        teamProject.save();
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

        new WBSEditor(new TeamProject(new File(filename), "Team Project"));
    }

    private class SaveAction extends AbstractAction {
        public SaveAction() {
            super("Save");
            putValue(MNEMONIC_KEY, new Integer('S'));
        }
        public void actionPerformed(ActionEvent e) {
            teamProject.save();
        }
    }

    private class CloseAction extends AbstractAction {
        public CloseAction() {
            super("Close");
            putValue(MNEMONIC_KEY, new Integer('C'));
        }
        public void actionPerformed(ActionEvent e) {
            teamProject.save();
            System.exit(0);
        }
    }

    private class WorkflowEditorAction extends AbstractAction {
        public WorkflowEditorAction() {
            super("Edit Workflows");
            putValue(MNEMONIC_KEY, new Integer('E'));
        }
        public void actionPerformed(ActionEvent e) {
            new WorkflowEditor(teamProject);
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

}
