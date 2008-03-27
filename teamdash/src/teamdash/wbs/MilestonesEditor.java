package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.table.TableColumn;

import teamdash.team.ColorCellEditor;
import teamdash.team.ColorCellRenderer;
import teamdash.wbs.columns.MilestoneCommitDateColumn;


public class MilestonesEditor {

    /** The team project that these milestones belong to. */
    TeamProject teamProject;

    /** The data model for the milestones */
    MilestonesDataModel milestonesModel;

    /** The table to display the milestones in */
    WBSJTable table;

    /** The frame containing this milestones editor */
    JFrame frame;

    /** A toolbar for editing the milestones */
    JToolBar toolBar;

    public MilestonesEditor(TeamProject teamProject,
            MilestonesDataModel milestonesModel) {
        this.teamProject = teamProject;
        this.milestonesModel = milestonesModel;
        table = createWorkflowJTable(milestonesModel);
        table.setEditingEnabled(teamProject.isReadOnly() == false);
        buildToolbar();
        frame = new JFrame(teamProject.getProjectName()
                + " - Project Milestones");
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
    }

    public void show() {
        frame.setVisible(true);
        frame.toFront();
    }

    public void hide() {
        frame.setVisible(false);
    }


    public static WBSJTable createWorkflowJTable(
            MilestonesDataModel workflowModel) {
        // create the WBSJTable, then set its model to the workflow data model.
        WBSJTable table = new WBSJTable(workflowModel.getWBSModel(),
                makeIconMap(), makeNodeTypeMenu());
        table.setModel(workflowModel);
        // reset the row height, for proper display of wbs node icons.
        table.setRowHeight(19);
        // don't allow reordering, since the text displayed in several of the
        // columns is meant to be read from left to right.
        table.getTableHeader().setReorderingAllowed(false);
        // the next line is necessary; WBSJTable sets this property and we need
        // it turned off.  Otherwise, date cell editor changes get canceled.
        table.putClientProperty("terminateEditOnFocusLost", null);
        // install the default editor for table data.
//        table.setDefaultEditor(Object.class, new WorkflowCellEditor());
        table.selfName = "project milestone list";
        table.setIndentationDisabled(true);

        TableColumn col;

        // customize the display of the "Name" column.
        col = table.getColumn("Name");
        col.setPreferredWidth(300);

        // customize the display and editing of the "Units" column.
        col = table.getColumn("Commit Date");
        col.setCellEditor(MilestoneCommitDateColumn.CELL_EDITOR);
        col.setPreferredWidth(60);

        col = table.getColumn("Color");
        ColorCellEditor.setUpColorEditor(table);
        ColorCellRenderer.setUpColorRenderer(table);
        col.setPreferredWidth(40);

        return table;
    }

    private static Map makeIconMap() {
        Map result = new HashMap();
        result.put(null, IconFactory.getMilestoneIcon());
        return result;
    }

    private static JMenu makeNodeTypeMenu() {
        JMenu result = new JMenu();
        result.add(new JMenuItem("FIXME")); // FIXME
        return result;
    }

    private void buildToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0, 0, 0, 0));

        addToolbarButtons(table.getEditingActions());
        toolBar.addSeparator();
        addToolbarButtons(getMilestoneActions());
    }

    /** Add one or more buttons to the internal tool bar */
    private void addToolbarButtons(Action[] actions) {
        for (int i = 0; i < actions.length; i++)
            if (actions[i].getValue(Action.SMALL_ICON) != null
                    && actionIsApplicable(actions[i]))
                addToolbarButton(actions[i]);
    }

    private boolean actionIsApplicable(Action a) {
        String category = (String) a.getValue(WBSJTable.WBS_ACTION_CATEGORY);
        if (category == WBSJTable.WBS_ACTION_CATEGORY_CLIPBOARD
                || category == WBSJTable.WBS_ACTION_CATEGORY_STRUCTURE)
            return true;
        else
            return false;
    }

    /** Add a button to the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        // button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);

        Icon icon = button.getIcon();
        if (icon != null && !(icon instanceof ImageIcon))
            button.setDisabledIcon(IconFactory.getModifiedIcon(icon,
                IconFactory.DISABLED_ICON));

        toolBar.add(button);
    }

//    private static class WorkflowCellEditor extends DefaultCellEditor {
//
//        public WorkflowCellEditor() {
//            super(new JTextField());
//        }
//
//        @Override
//        public Component getTableCellEditorComponent(JTable table,
//                Object value, boolean isSelected, int row, int column) {
//            Component result = super.getTableCellEditorComponent(table,
//                ErrorValue.unwrap(value), isSelected, row, column);
//
//            if (result instanceof JTextField)
//                ((JTextField) result).selectAll();
//
//            return result;
//        }
//
//    }



    public Action[] getMilestoneActions() {
        return new Action[] {};
    }


}
