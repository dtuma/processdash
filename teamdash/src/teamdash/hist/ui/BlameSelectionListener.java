// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.hist.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.FocusManager;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import teamdash.hist.BlameCaretPos;
import teamdash.hist.BlameData;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.columns.WBSNodeColumn;

public class BlameSelectionListener implements ListSelectionListener,
        MouseListener, ActionListener {

    private JTable table;

    private BlameData blameData;

    private Timer timer;

    private boolean inMousePress;

    public BlameSelectionListener(JTable table, BlameData blameData) {
        this.table = table;
        this.blameData = blameData;

        this.timer = new Timer(50, this);
        this.timer.setRepeats(false);

        table.addMouseListener(this);
        table.getSelectionModel().addListSelectionListener(this);
        table.getColumnModel().getSelectionModel()
                .addListSelectionListener(this);
    }

    public void columnModelChanging() {
        table.getColumnModel().getSelectionModel()
                .removeListSelectionListener(this);
    }

    public void columnModelChanged() {
        table.getColumnModel().getSelectionModel()
                .addListSelectionListener(this);
    }

    public void dispose() {
        table.removeMouseListener(this);
        table.getSelectionModel().removeListSelectionListener(this);
        table.getColumnModel().getSelectionModel()
                .removeListSelectionListener(this);
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        inMousePress = true;
        timer.stop();
    }

    public void mouseReleased(MouseEvent e) {
        inMousePress = false;
        timer.restart();
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!inMousePress)
            timer.restart();
    }


    public void actionPerformed(ActionEvent e) {
        if (!hasFocus())
            return;

        int[] selectedRows = table.getSelectedRows();
        int[] selectedColumns = table.getSelectedColumns();
        if (selectedRows.length == 0 || selectedColumns.length == 0)
            return;

        WBSModel wbsModel;
        List<String> columns = new ArrayList(selectedColumns.length);
        if (table.getModel() instanceof DataTableModel) {
            wbsModel = ((DataTableModel) table.getModel()).getWBSModel();
            for (int i = 0; i < selectedColumns.length; i++) {
                String identifier = (String) table.getColumnModel()
                        .getColumn(selectedColumns[i]).getIdentifier();
                columns.add(identifier);
            }
        } else {
            wbsModel = (WBSModel) table.getModel();
            columns.add(WBSNodeColumn.COLUMN_ID);
        }

        List<Integer> nodes = new ArrayList();
        for (int i = 0; i < selectedRows.length; i++) {
            WBSNode node = wbsModel.getNodeForRow(selectedRows[i]);
            nodes.add(node.getTreeNodeID());
        }

        BlameCaretPos caretPos = new BlameCaretPos(wbsModel.getModelType(),
                nodes, columns);
        if (SET_EMPTY_CARETS || blameData.countAnnotations(caretPos) > 0)
            blameData.setCaretPos(caretPos);
    }

    private boolean hasFocus() {
        if (table.isFocusOwner())
            return true;

        Component component = FocusManager.getCurrentManager().getFocusOwner();
        if (component == null)
            return false;

        return table.isAncestorOf(component);
    }

    private static final boolean SET_EMPTY_CARETS = false;

}
