// Copyright (C) 2002-2010 Tuma Solutions, LLC
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

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.TableCellEditor;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ReadOnlyValue;
import teamdash.wbs.TeamProcess;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WrappedValue;

/** A column for displaying the units of size measurement for individual tasks.
 *
 * This column defers much of its behavior to the main "Units" column
 * (implemented by the {@link SizeTypeColumn} class). It differs in the
 * following ways:<ul>
 * <li>This column displays values provided by the "Units" column, but
 *   marks them as read-only.
 * <li>This column displays effective units for "leaf tasks", and allows
 *   those units to be edited.
 * </ul>
 */
public class TaskSizeUnitsColumn extends AbstractDataColumn
implements CalculatedDataColumn, CustomEditedColumn {

    /** our data model */
    private DataTableModel dataModel;
    /** our work breakdown structure */
    private WBSModel wbsModel;
    /** our team process */
    private TeamProcess teamProcess;
    /** the list of size units to use */
    private String[] sizeMetrics;
    /** the integer ID of the "Units" column in the data model */
    private int mainSizeUnitsColumn;


    public TaskSizeUnitsColumn(DataTableModel dataModel,
                               TeamProcess teamProcess) {
        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.teamProcess = teamProcess;
        this.sizeMetrics = teamProcess.getSizeMetrics();
        this.columnName = "Units";
        this.columnID = COLUMN_ID;
        this.preferredWidth = 80;
        this.dependentColumns = new String[] { SizeTypeColumn.COLUMN_ID };
        this.mainSizeUnitsColumn = -1;
    }

    public void storeDependentColumn(String ID, int columnNumber) {
        if (SizeTypeColumn.COLUMN_ID.equals(ID))
            // remember the ID of the "Units" column.
            mainSizeUnitsColumn = columnNumber;
    }

    public void resetDependentColumns() {
        mainSizeUnitsColumn = -1;
    }

    public boolean isCellEditable(WBSNode node) {
        // only allow leaf tasks to be edited.
        return TeamTimeColumn.isLeafTask(wbsModel, node);
    }

    private boolean valueIsEmpty(Object aValue) {
        if (aValue == null) return true;
        if (aValue instanceof String &&
            ((String) aValue).trim().length() == 0) return true;
        return false;
    }

    protected String getDefaultValue(WBSNode node) {
        // what units would this node use, by default? (ask the team process)
        return teamProcess.getPhaseSizeMetric(node.getType());
    }

    public Object getValueAt(WBSNode node) {
        if (isCellEditable(node)) {
            // for editable cells (i.e. leaf tasks), use either:
            // (1) the user-edited value of this node, or
            // (2) the default value, if there is no user-edited value.
            String result = (String) node.getAttribute(ATTR_NAME);
            if (valueIsEmpty(result))
                result = getDefaultValue(node);
            return result;

        } else if (mainSizeUnitsColumn != -1) {
            // for non-leaf tasks, defer to the wisdom of the main
            // "Units" column.  If it comes up blank, try the default value.
            Object result = WrappedValue.unwrap(dataModel.getValueAt(node,
                mainSizeUnitsColumn));
            if (valueIsEmpty(result))
                result = getDefaultValue(node);
            return new ReadOnlyValue(result);

        } else {
            // return null if we haven't been told the id of the
            // "Units" column yet.
            return null;
        }
    }

    public void setValueAt(Object aValue, WBSNode node) {
        if (valueIsEmpty(aValue) || aValue.equals(getDefaultValue(node)))
            node.setAttribute(ATTR_NAME, null);
        else
            node.setAttribute(ATTR_NAME, String.valueOf(aValue));
    }

    public boolean recalculate() { return true; }

    /** Install a custom cell editor */
    public TableCellEditor getCellEditor() {
        JComboBox sizeUnits = new JComboBox(sizeMetrics);
        sizeUnits.setEditable(true);
        return new DefaultCellEditor(sizeUnits);
    }

    /** This is the attribute we will use to store our data on WBS nodes */
    private static final String ATTR_NAME = "Task Size Units";
    /** This is the ID we will give our column */
    static final String COLUMN_ID = ATTR_NAME;

}