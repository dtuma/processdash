// Copyright (C) 2020 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.GuiPrefs;

import teamdash.merge.ui.MergeConflictHyperlinkHandler;
import teamdash.wbs.icons.WBSEditorIcon;

public class SizeMetricsEditor implements MergeConflictHyperlinkHandler {

    /** The team project that these size metrics belong to. */
    TeamProject teamProject;

    /** The data model for the size metrics */
    SizeMetricsDataModel sizeMetricsModel;

    /** The table to display the size metrics in */
    SizeMetricsJTable table;

    /** An object for tracking undo operations */
    UndoList undoList;

    /** The frame containing this editor */
    JFrame frame;

    /** A toolbar for editing the size metrics */
    JToolBar toolBar;

    static final Resources resources = Resources
            .getDashBundle("WBSEditor.SizeMetrics");

    public SizeMetricsEditor(TeamProject teamProject,
            SizeMetricsDataModel sizeMetricsModel, WBSWindowTitle title,
            GuiPrefs guiPrefs) {
        this.teamProject = teamProject;
        this.sizeMetricsModel = sizeMetricsModel;
        this.sizeMetricsModel.setEditingEnabled(isEditable(teamProject));

        table = createSizeMetricsJTable();
        table.setEditingEnabled(isEditable(teamProject));
        int tableHeight = table.getRowHeight() * table.getRowCount();
        guiPrefs.load("sizeMetricsTable", table);

        buildToolbar();

        frame = title.register(new JFrame(resources.getString("Window_Title")));
        WBSEditorIcon.setWindowIcon(frame);
        frame.getContentPane().add(new JScrollPane(table));
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, Math.min(400, 150 + tableHeight));
        WBSZoom.get().manage(frame, "size~");
        guiPrefs.load("sizeMetricsWindow", frame);
    }

    public static boolean isEditable(TeamProject teamProject) {
        return teamProject.isReadOnly() == false
                && WBSPermissionManager.hasPerm("wbs.sizeMetrics", "2.6.3");
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

    private SizeMetricsJTable createSizeMetricsJTable() {
        SizeMetricsJTable table = new SizeMetricsJTable(sizeMetricsModel);
        table.renderer.setRootNodeName(resources.getString("Root_Name"));

        undoList = new UndoList(sizeMetricsModel.getWBSModel());
        undoList.setForComponent(table);
        sizeMetricsModel.addTableModelListener(new UndoableEventRepeater());

        return table;
    }

    private void buildToolbar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(0, 0, 0, 0));

        addToolbarButton(undoList.getUndoAction());
        addToolbarButton(undoList.getRedoAction());
        addToolbarButton(table.ADD_METRIC_ACTION);
        addToolbarButton(table.RENAME_METRIC_ACTION);
        addToolbarButton(table.DELETE_METRIC_ACTION);
        addToolbarButton(table.MOVEUP_ACTION);
        addToolbarButton(table.MOVEDOWN_ACTION);
    }

    /** Add a button to the internal tool bar */
    private void addToolbarButton(Action a) {
        JButton button = new JButton(a);
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

    private class UndoableEventRepeater implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            if (e.getColumn() > 0 && e.getFirstRow() > 0
                    && e.getFirstRow() == e.getLastRow())
                undoList.madeChange("Edited value");
        }

    }

}
