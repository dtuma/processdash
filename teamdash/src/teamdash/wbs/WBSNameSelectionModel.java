// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


public class WBSNameSelectionModel extends DefaultListSelectionModel implements
        TableModelListener, PropertyChangeListener {

    WBSJTable sourceTable;
    WBSModel sourceModel;

    PropertyEditor selectedNamesManager;
    List selectedNames;
    private boolean isMakingChange = false;

    public WBSNameSelectionModel(WBSJTable sourceTable, PropertyEditor e) {
        this.sourceTable = sourceTable;
        this.selectedNamesManager = e;
        this.selectedNamesManager.addPropertyChangeListener(this);

        DataTableModel sourceTableModel = (DataTableModel) sourceTable
                .getModel();
        sourceModel = sourceTableModel.getWBSModel();
        sourceModel.addTableModelListener(this);
    }


    public void tableChanged(TableModelEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshSelectionFromNames();
            }});
    }


    public void propertyChange(PropertyChangeEvent evt) {
        refreshSelectionFromNames();
    }


    private void refreshNamesFromSelection() {
        if (!isMakingChange) {
            int[] rows = sourceTable.getSelectedRows();
            List newSelectedNames = sourceModel.getBaseNamesForRows(rows);

            if (!newSelectedNames.equals(selectedNames))
                selectedNamesManager.setValue(newSelectedNames);

            refreshSelectionFromNames();
        }
    }


    private void refreshSelectionFromNames() {
        isMakingChange = true;
        selectedNames = (List) selectedNamesManager.getValue();
        List nodesToSelect = sourceModel.getNodesForBaseNames(selectedNames);
        int[] rows = sourceModel.getRowsForNodes(nodesToSelect);
        Arrays.sort(rows);

        int anchor = super.getAnchorSelectionIndex();
        if (Arrays.binarySearch(rows, anchor) < 0)
            anchor = -1;

        super.clearSelection();
        for (int i = 0; i < rows.length; i++)
            super.addSelectionInterval(rows[i], rows[i]);

        if (anchor != -1)
            super.addSelectionInterval(anchor, anchor);
        isMakingChange = false;
    }


    public void addSelectionInterval(int index0, int index1) {
        super.addSelectionInterval(index0, index1);
        refreshNamesFromSelection();
    }
    public void clearSelection() {
        super.clearSelection();
        refreshNamesFromSelection();
    }
    public void removeSelectionInterval(int index0, int index1) {
        super.removeSelectionInterval(index0, index1);
        refreshNamesFromSelection();
    }
    public void setAnchorSelectionIndex(int anchorIndex) {
        super.setAnchorSelectionIndex(anchorIndex);
        refreshNamesFromSelection();
    }
    public void setLeadSelectionIndex(int leadIndex) {
        super.setLeadSelectionIndex(leadIndex);
        refreshNamesFromSelection();
    }
    public void setSelectionInterval(int index0, int index1) {
        super.setSelectionInterval(index0, index1);
        refreshNamesFromSelection();
    }


}
