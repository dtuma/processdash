// Copyright (C) 2002-2017 Tuma Solutions, LLC
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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import net.sourceforge.processdash.i18n.Resources;

import teamdash.merge.ui.MergeConflictHyperlinkHandler;
import teamdash.team.ColorCellEditor;
import teamdash.team.ColorCellRenderer;
import teamdash.wbs.columns.MilestoneColorColumn;
import teamdash.wbs.columns.MilestoneCommitDateColumn;
import teamdash.wbs.columns.MilestoneDeferredColumn;
import teamdash.wbs.columns.MilestoneVisibilityColumn;
import teamdash.wbs.columns.WBSNodeColumn;


public class MilestonesEditor implements MergeConflictHyperlinkHandler {

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

    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.Milestones");


    public MilestonesEditor(TeamProject teamProject,
            MilestonesDataModel milestonesModel) {
        this.teamProject = teamProject;
        this.milestonesModel = milestonesModel;
        this.milestonesModel.setEditingEnabled(isEditable(teamProject));
        table = createMilestonesJTable();
        table.setEditingEnabled(isEditable(teamProject));
        buildToolbar();
        frame = new JFrame(teamProject.getProjectName() + " - "
                + resources.getString("Window_Title"));
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
    }

    public static boolean isEditable(TeamProject teamProject) {
        return teamProject.isReadOnly() == false
                && WBSPermissionManager.hasPerm("wbs.milestones", "2.3.1.2");
    }

    public void show() {
        frame.setExtendedState(JFrame.NORMAL);
        frame.setVisible(true);
        frame.toFront();
    }

    public void hide() {
        frame.setVisible(false);
    }

    public void stopEditing() {
        table.stopCellEditing();
    }

    public boolean displayHyperlinkedItem(String item) {
        if (table.displayHyperlinkedItem(item) == false)
            return false;

        show();
        return true;
    }


    private WBSJTable createMilestonesJTable() {
        // create the WBSJTable for the milestones data model.
        WBSJTable table = new WBSJTable(milestonesModel, makeIconMap(),
                makeNodeTypeMenu());
        // the next line is necessary; WBSJTable sets this property and we need
        // it turned off.  Otherwise, date cell editor changes get canceled.
        table.putClientProperty("terminateEditOnFocusLost", null);
        table.setIndentationDisabled(true);

        TableColumn col;

        // customize the display of the "Name" column.
        table.renderer.setRootNodeName(resources.getString("Root_Name"));
        col = table.getColumnModel().getColumn(
            milestonesModel.findColumn(WBSNodeColumn.COLUMN_ID));
        col.setPreferredWidth(300);

        // customize the display and editing of the "Commit Date" column.
        col = table.getColumnModel().getColumn(
            milestonesModel.findColumn(MilestoneCommitDateColumn.COLUMN_ID));
        col.setCellEditor(MilestoneCommitDateColumn.CELL_EDITOR);
        col.setCellRenderer(MilestoneCommitDateColumn.CELL_RENDERER);
        col.setPreferredWidth(60);

        // customize the display and editing of the "Color" column.
        col = table.getColumnModel().getColumn(
            milestonesModel.findColumn(MilestoneColorColumn.COLUMN_ID));
        ColorCellEditor.setUpColorEditor(table);
        ColorCellRenderer.setUpColorRenderer(table);
        col.setPreferredWidth(40);

        // customize the display and editing of the "Hide" column.
        col = table.getColumnModel().getColumn(
            milestonesModel.findColumn(MilestoneVisibilityColumn.COLUMN_ID));
        col.setCellRenderer(MilestoneVisibilityColumn.CELL_RENDERER);
        col.setPreferredWidth(15);

        // customize the display and editing of the "Defer Sync" column.
        col = table.getColumnModel().getColumn(
            milestonesModel.findColumn(MilestoneDeferredColumn.COLUMN_ID));
        col.setCellRenderer(MilestoneDeferredColumn.CELL_RENDERER);
        col.setPreferredWidth(45);

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
        // no need to put anything in this menu, because node types are not
        // editable in the milestones model.
        return result;
    }

    private void buildToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0, 0, 0, 0));

        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());
        table.tweakClipboardActions(resources,
            IconFactory.getCopyMilestoneIcon(),
            IconFactory.getPasteMilestoneIcon());
        addToolbarButton(table.CUT_ACTION);
        addToolbarButton(table.COPY_ACTION);
        addToolbarButton(table.PASTE_ACTION);
        addToolbarButton(table.MOVEUP_ACTION);
        addToolbarButton(table.MOVEDOWN_ACTION);
        addToolbarButton(table.DELETE_ACTION);
        toolBar.addSeparator();
        addToolbarButton(new SortMilestonesAction());
    }

    /** Add a button to the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
        // button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setToolTipText((String) a.getValue(Action.NAME));
        button.setText(null);
        IconFactory.setDisabledIcon(button);
        toolBar.add(button);
    }

    public void addChangeListener(ChangeListener l) {
        undoList.addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        undoList.removeChangeListener(l);
    }

    private class SortMilestonesAction extends AbstractAction implements
            Comparator<WBSNode> {

        public SortMilestonesAction() {
            super(resources.getString("Commit_Date.Sort"),
                    IconFactory.getSortDatesIcon());
            if (!isEditable(teamProject))
                setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            MilestonesWBSModel model = milestonesModel.getWBSModel();
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
            WBSNode[] milestones = model.getMilestones();
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
