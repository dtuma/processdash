package teamdash.wbs;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import teamdash.TeamProcess;
import teamdash.TeamProject;

public class WorkflowEditor {

    TeamProject teamProject;
    WorkflowModel workflowModel;
    JTable table;
    JFrame frame;

    public WorkflowEditor(TeamProject teamProject) {
        this.teamProject = teamProject;
        this.workflowModel = new WorkflowModel
            (teamProject.getWorkflows(), teamProject.getTeamProcess());
        buildTable();
        frame = new JFrame();
        frame.getContentPane().add(new JScrollPane(table));
        frame.pack();
        frame.show();
    }


    private void buildTable() {
        TeamProcess process = teamProject.getTeamProcess();
        table = new WBSJTable(teamProject.getWorkflows(),
                              process.getIconMap(), process.getNodeTypeMenu());
        table.setModel(workflowModel);
        table.setRowHeight(19);
        table.setDefaultEditor(Object.class, new DataTableCellEditor());

        TableColumn col = table.getColumn("%");
        DataTableStringSuffixRenderer render =
            new DataTableStringSuffixRenderer("% of ");
        render.setHorizontalAlignment(JLabel.RIGHT);
        col.setCellRenderer(render);

        col = table.getColumn("Units");
        col.setCellRenderer(new DataTableStringSuffixRenderer(" per Hour"));

        /*
        TableColumn col = table.getColumn("wbsNode");
        col.setCellEditor
            (new WBSNodeEditor(table, teamProject.getWorkflows(),
             teamProject.getTeamProcess().getIconMap());
        col.setCellRenderer
            (new WBSNodeRenderer(teamProject.getWorkflows(),
             teamProject.getTeamProcess().getIconMap());
                */
        // TODO
    }

}
