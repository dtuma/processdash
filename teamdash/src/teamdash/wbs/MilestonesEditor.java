package teamdash.wbs;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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

    /** An object for tracking undo operations */
    UndoList undoList;

    /** The frame containing this milestones editor */
    JFrame frame;

    /** A toolbar for editing the milestones */
    JToolBar toolBar;

    public MilestonesEditor(TeamProject teamProject,
            MilestonesDataModel milestonesModel) {
        this.teamProject = teamProject;
        this.milestonesModel = milestonesModel;
        table = createMilestonesJTable();
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


    private WBSJTable createMilestonesJTable() {
        // create the WBSJTable, then set its model to the milestones data model.
        WBSJTable table = new WBSJTable(milestonesModel.getWBSModel(),
                makeIconMap(), makeNodeTypeMenu());
        table.setModel(milestonesModel);
        // reset the row height, for proper display of wbs node icons.
        table.setRowHeight(19);
        // don't allow reordering, since the text displayed in several of the
        // columns is meant to be read from left to right.
        table.getTableHeader().setReorderingAllowed(false);
        // the next line is necessary; WBSJTable sets this property and we need
        // it turned off.  Otherwise, date cell editor changes get canceled.
        table.putClientProperty("terminateEditOnFocusLost", null);
        table.selfName = "project milestone list";
        table.setIndentationDisabled(true);

        TableColumn col;

        // customize the display of the "Name" column.
        col = table.getColumn("Name");
        col.setPreferredWidth(300);

        // customize the display and editing of the "Units" column.
        col = table.getColumn("Commit Date");
        col.setCellEditor(MilestoneCommitDateColumn.CELL_EDITOR);
        col.setCellRenderer(MilestoneCommitDateColumn.CELL_RENDERER);
        col.setPreferredWidth(60);

        col = table.getColumn("Color");
        ColorCellEditor.setUpColorEditor(table);
        ColorCellRenderer.setUpColorRenderer(table);
        col.setPreferredWidth(40);

        undoList = new UndoList(milestonesModel.getWBSModel());
        undoList.setForComponent(table);
        milestonesModel.addTableModelListener(new UndoableEventRepeater());

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

        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());
        addToolbarButtons(table.getEditingActions());
        toolBar.addSeparator();
        addToolbarButton(new SortMilestonesAction());
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

    private class SortMilestonesAction extends AbstractAction implements
            Comparator<WBSNode> {

        public SortMilestonesAction() {
            super("Sort by Commit Date", IconFactory.getSortDatesIcon());
        }

        public void actionPerformed(ActionEvent e) {
            MilestonesWBSModel model = (MilestonesWBSModel) milestonesModel
                    .getWBSModel();
            loadSortDates(model);
            model.sortMilestones(this);
            sortDates = null;
            undoList.madeChange("Sorted milestones");
        }

        // If we sort by the assigned date only, milestones with no commit date
        // could get sorted to the end of the list.  Instead, we'd like to
        // perform a more stable sort that mostly leaves uncommitted milestones
        // in their original order within the list.  So we use this map to
        // assign non-null dates to each milestone for sorting purposes.
        private Map<WBSNode,Date> sortDates;

        private void loadSortDates(MilestonesWBSModel model) {
            sortDates = new HashMap<WBSNode, Date>();
            WBSNode[] milestones = model.getDescendants(model.getRoot());
            Date useDate = new Date(Long.MAX_VALUE);
            for (int i = milestones.length;  i-- > 0; ) {
                WBSNode milestone = milestones[i];
                Date oneDate = MilestoneCommitDateColumn.getCommitDate(milestone);
                if (oneDate != null)
                    useDate = oneDate;
                sortDates.put(milestone, useDate);
            }
        }

        public int compare(WBSNode n1, WBSNode n2) {
            return sortDates.get(n1).compareTo(sortDates.get(n2));
        }

    }

    private class UndoableEventRepeater implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            if (e.getColumn() > 0 && e.getFirstRow() > 0
                    && e.getFirstRow() == e.getLastRow())
                undoList.madeChange("Edited value");
        }

    }

}
