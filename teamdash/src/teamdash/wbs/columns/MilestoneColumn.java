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

package teamdash.wbs.columns;

import java.awt.Component;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.ui.lib.autocomplete.AutocompletingDataTableCellEditor;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.ItalicCellRenderer;
import teamdash.wbs.MilestonesWBSModel;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;

public class MilestoneColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CustomEditedColumn, CalculatedDataColumn {

    public static final String COLUMN_ID = "Milestone";

    private static final String EXPLICIT_VALUE_ATTR = "Milestone";

    private static final String INHERITED_VALUE_ATTR = "Inherited_Milestone";

    private DataTableModel dataModel;

    private WBSModel wbsModel;

    private MilestonesWBSModel milestoneModel;

    public MilestoneColumn(DataTableModel dataModel,
            MilestonesWBSModel milestoneModel) {
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Milestone.Name");
        this.preferredWidth = 200;
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.milestoneModel = milestoneModel;
        setConflictAttributeName(EXPLICIT_VALUE_ATTR);

        milestoneModel.addTableModelListener(new MilestoneNameListener());
    }

    public boolean recalculate() {
        recalculate(wbsModel.getRoot(), NO_MILESTONE);
        return true;
    }

    private void recalculate(WBSNode node, Integer inheritedValue) {
        node.setAttribute(INHERITED_VALUE_ATTR, inheritedValue);
        Integer nodeValue = node.getIntegerAttribute(EXPLICIT_VALUE_ATTR);
        if (nodeValue != null)
            inheritedValue = nodeValue;

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0; i < children.length; i++)
            recalculate(children[i], inheritedValue);
    }


    public void storeDependentColumn(String ID, int columnNumber) {}

    public boolean isCellEditable(WBSNode node) {
        return true;
    }

    public Object getValueAt(WBSNode node) {
        Integer nodeValue = node.getIntegerAttribute(EXPLICIT_VALUE_ATTR);
        if (nodeValue != null)
            return milestoneModel.getNameForMilestone(nodeValue);

        Integer inheritedValue = node.getIntegerAttribute(INHERITED_VALUE_ATTR);
        if (inheritedValue != null)
            return new ErrorValue(
                    milestoneModel.getNameForMilestone(inheritedValue),
                    EFFECTIVE_MILESTONE_MESSAGE);

        return null;
    }

    public static int getMilestoneID(WBSNode node,
            MilestonesWBSModel milestones) {
        Integer result = getMilestoneIDAttr(node);
        if (result != -1 && milestones != null
                && !milestones.getMilestoneNameMap().containsKey(result)) {
            result = -1;
        }
        return result;
    }

    private static Integer getMilestoneIDAttr(WBSNode node) {
        Integer nodeValue = node.getIntegerAttribute(EXPLICIT_VALUE_ATTR);
        if (nodeValue != null)
            return nodeValue;
        Integer inheritedValue = node.getIntegerAttribute(INHERITED_VALUE_ATTR);
        if (inheritedValue != null)
            return inheritedValue;
        return -1;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        String name = (String) aValue;
        Integer id;

        if (name == null || name.trim().length() == 0) {
            // the user has deleted the value in the cell.  They want to
            // clear the milestone value for this node.
            id = NO_MILESTONE;
        } else {
            // look up the milestone with the given name.
            id = milestoneModel.getIdForMilestone(name);
            if (id == null)
                // the milestone name was not recognized.  Do nothing.
                return;
        }

        Integer inheritedValue = node.getIntegerAttribute(INHERITED_VALUE_ATTR);
        if (id != null && id.equals(inheritedValue))
            id = null;

        node.setAttribute(EXPLICIT_VALUE_ATTR, id);
    }

    @Override
    public void storeConflictResolutionValue(Object value, WBSNode node) {
        node.setAttribute(EXPLICIT_VALUE_ATTR, value);
    }


    public TableCellRenderer getCellRenderer() {
        return MILESTONE_RENDERER;
    }

    public TableCellEditor getCellEditor() {
        return new MilestoneCellEditor();
    }

    private class MilestoneCellEditor extends AutocompletingDataTableCellEditor {

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            // refresh the data model with the current set of known milestones.
            getComboBox().removeAllItems();
            for (String name : milestoneModel.getMilestoneNames())
                getComboBox().addItem(name);

            // now defer to the parent for the rest of the work.
            return super.getTableCellEditorComponent(table, ErrorValue
                    .unwrap(value), isSelected, row, column);
        }

    }


    private class MilestoneNameListener implements TableModelListener {

        public void tableChanged(TableModelEvent e) {
            dataModel.columnChanged(MilestoneColumn.this);
        }

    }


    public static void remapNodeIDs(WBSModel model, Map<Integer, Integer> idMap) {
        if (idMap != null && !idMap.isEmpty()) {
            remapNodeIDs(model.getRoot(), idMap);
            for (WBSNode node : model.getDescendants(model.getRoot()))
                remapNodeIDs(node, idMap);
        }
    }

    private static void remapNodeIDs(WBSNode node, Map<Integer, Integer> idMap) {
        Integer nodeValue = node.getIntegerAttribute(EXPLICIT_VALUE_ATTR);
        if (nodeValue != null) {
            Integer newValue = idMap.get(nodeValue);
            if (newValue != null)
                node.setAttribute(EXPLICIT_VALUE_ATTR, newValue);
        }
    }



    private static final Integer NO_MILESTONE = -1;
    private static final String EFFECTIVE_MILESTONE_MESSAGE =
            resources.getString("Inherited_Tooltip");
    private static final TableCellRenderer MILESTONE_RENDERER = new ItalicCellRenderer(
            EFFECTIVE_MILESTONE_MESSAGE);

}
