package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.List;
import javax.swing.JFrame;

import teamdash.TeamMember;
import teamdash.TeamProject;
import teamdash.wbs.columns.TeamMemberTimeColumn;
import teamdash.wbs.columns.TeamTimeColumn;

public class WBSEditor implements WindowListener {

    TeamProject teamProject;
    JFrame frame;

    public WBSEditor(TeamProject teamProject) {
        this.teamProject = teamProject;

        WBSModel model = teamProject.getWBS();
        DataTableModel data = new DataTableModel(model);
        WBSTabPanel table = new WBSTabPanel(model, data, teamProject.getTeamProcess());

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

        List teamMembers = teamProject.getTeamMemberList().getTeamMembers();
        int teamSize = teamMembers.size();
        String[] teamColumnIDs = new String[teamSize+1];
        String[] teamColumnNames = new String[teamSize+1];
        DataTableModel dataModel = (DataTableModel) table.dataTable.getModel();
        dataModel.addDataColumn(new TeamTimeColumn(dataModel));
        for (int i = 0;   i < teamMembers.size();   i++) {
            TeamMember m = (TeamMember) teamMembers.get(i);
            TeamMemberTimeColumn col = new TeamMemberTimeColumn(dataModel, m);
            dataModel.addDataColumn(col);
            teamColumnIDs[i+1] = col.getColumnID();
            teamColumnNames[i+1] = m.getInitials();
        }
        teamColumnIDs[0] = "Time";
        teamColumnNames[0] = "Team";
        table.addTab("Time", teamColumnIDs, teamColumnNames);

        table.addTab("Time Calc",
                     new String[] { "Size", "Size-Units", "Rate", "Hrs/Indiv", "# People", "Time", "111-Time", "222-Time", "333-Time" },
                     new String[] { "Size", "Units", "Rate", "Hrs/Indiv", "# People",
                         "Time", "111", "222", "333" });

        String[] s = new String[] { "P", "O", "N", "M", "L", "K", "J", "I", "H", "G", "F" };
        table.addTab("Defects", s, s);


        TeamTimePanel teamTime =
            new TeamTimePanel(teamProject.getTeamMemberList(), dataModel);

        JFrame frame = new JFrame
            (teamProject.getProjectName() + " - Work Breakdown Structure");
        frame.getContentPane().add(table);
        frame.getContentPane().add(teamTime, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(this);
        frame.pack();
        frame.show();
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

}
