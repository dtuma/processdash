package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.table.TableColumn;

/** A graphical user interface for editing common workflows.
 */
public class WorkflowEditor {

    /** The team project that these workflows belong to. */
    TeamProject teamProject;
    /** The data model for the workflows */
    WorkflowModel workflowModel;
    /** The table to display the workflows in */
    WBSJTable table;
    /** The frame containing this workflow editor */
    JFrame frame;
    /** A toolbar for editing the workflows */
    JToolBar toolBar;

    public WorkflowEditor(TeamProject teamProject) {
        this.teamProject = teamProject;
        this.workflowModel = new WorkflowModel
            (teamProject.getWorkflows(), teamProject.getTeamProcess());
        buildTable();
        buildToolbar();
        frame = new JFrame(teamProject.getProjectName() +
                           " - Common Team Workflows");
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.show();
    }

    public void show() {
        frame.show();
        frame.toFront();
    }


    private void buildTable() {
        // create the WBSJTable, then set its model to the workflow data model.
        TeamProcess process = teamProject.getTeamProcess();
        table = new WBSJTable(teamProject.getWorkflows(),
                              process.getIconMap(), process.getNodeTypeMenu());
        table.setModel(workflowModel);
        // reset the row height, for proper display of wbs node icons.
        table.setRowHeight(19);
        // don't allow reordering, since the text displayed in several of the
        // columns is meant to be read from left to right.
        table.getTableHeader().setReorderingAllowed(false);
        // install the default editor for table data.
        table.setDefaultEditor(Object.class, new DataTableCellEditor());
        table.selfName = "team workflow structure";

        TableColumn col;

        // customize the display of the "Name" column.
        col = table.getColumn("Name");
        col.setPreferredWidth(300);

        // customize the display of the "%" column.
        col = table.getColumn("%");
        DataTableStringSuffixRenderer render =
            new DataTableStringSuffixRenderer("% of ");
        render.setHorizontalAlignment(JLabel.RIGHT);
        col.setCellRenderer(render);
        col.setPreferredWidth(60);

        // customize the display of the "Rate" column.
        col = table.getColumn("Rate");
        col.setPreferredWidth(50);

        // customize the display and editing of the "Units" column.
        col = table.getColumn("Units");
        col.setCellRenderer(new DataTableStringSuffixRenderer(" per Hour"));
        CustomEditedColumn edCol =
            (CustomEditedColumn) workflowModel.getColumn(col.getModelIndex());
        col.setCellEditor(edCol.getCellEditor());
        col.setPreferredWidth(130);

        // customize the display of the "# People" column.
        col = table.getColumn("# People");
        col.setPreferredWidth(60);
    }


    private void buildToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0,0,0,0));

        Action[] editingActions = table.getEditingActions();
        for (int i = 0; i < editingActions.length; i++)
            if (editingActions[i].getValue(Action.SMALL_ICON) != null)
                addToolbarButton(editingActions[i]);
    }

    /** Add a button to the beginning of the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        //button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);
        toolBar.add(button);
    }

    /*
    private Set saveListeners = null;
    public void addSaveListener(SaveListener l) {
        if (saveListeners == null)
            saveListeners = new HashSet();
        saveListeners.add(l);
    }
    public void removeSaveListener(SaveListener l) {
        if (saveListeners != null)
            saveListeners.remove(l);
    }
    */
}
