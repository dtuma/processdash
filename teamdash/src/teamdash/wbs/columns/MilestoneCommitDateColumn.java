// Copyright (C) 2002-2012 Tuma Solutions, LLC
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.ui.lib.JDateTimeChooserCellEditor;

import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableCellRenderer;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.MilestonesWBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WrappedValue;

public class MilestoneCommitDateColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CustomEditedColumn {

    public static final String COLUMN_ID = "Commit Date";

    public static final String VALUE_ATTR = "Commit Date";
    public static final String MASTER_VALUE_ATTR = "Master Commit Date";

    private static final String CACHED_DATE_ATTR = "Commit_Date_Object";
    private static final String MASTER_CACHED_DATE_ATTR = "Master_Commit_Date_Object";
    private static final Date NO_DATE_VALUE = new Date(0);


    public MilestoneCommitDateColumn() {
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Milestones.Commit_Date.Name");
        this.preferredWidth = 65;
        setConflictAttributeName(VALUE_ATTR);
    }

    public Class getColumnClass() {
        return Date.class;
    }

    public Object getValueAt(WBSNode node) {
        Date projectDate = getProjectCommitDate(node);
        Date masterDate = getMasterCommitDate(node);
        if (projectDate == null) return masterDate;
        if (masterDate == null) return projectDate;
        if (masterDate.before(projectDate))
            return new ErrorValue(projectDate,
                    resources.format("Milestones.Commit_Date.Master_Error_FMT",
                        projectDate, masterDate));
        else
            return projectDate;
    }

    public static Date getCommitDate(WBSNode node) {
        Date projectDate = getProjectCommitDate(node);
        Date masterDate = getMasterCommitDate(node);
        if (projectDate == null) return masterDate;
        if (masterDate == null) return projectDate;
        if (masterDate.before(projectDate)) return masterDate;
        return projectDate;
    }

    private static Date getProjectCommitDate(WBSNode node) {
        return getDateAttr(node, VALUE_ATTR, CACHED_DATE_ATTR);
    }
    private static Date getMasterCommitDate(WBSNode node) {
        return getDateAttr(node, MASTER_VALUE_ATTR, MASTER_CACHED_DATE_ATTR);
    }
    private static Date getDateAttr(WBSNode node, String valueAttr,
            String cachedAttr) {
        Date result = (Date) node.getAttribute(cachedAttr);
        if (result == null) {
            result = NO_DATE_VALUE;
            String value = (String) node.getAttribute(valueAttr);
            if (value != null)
                try {
                    result = STORAGE_FMT.parse(value);
                } catch (Exception e) {
                }
            node.setAttribute(cachedAttr, result);
        }

        if (result == NO_DATE_VALUE)
            return null;
        else
            return result;
    }

    public boolean isCellEditable(WBSNode node) {
        return MilestonesWBSModel.MILESTONE_TYPE.equals(node.getType());
    }

    public void setValueAt(Object value, WBSNode node) {
        String storageVal;
        Date dateVal;
        if (value instanceof Date) {
            dateVal = (Date) value;
            storageVal = STORAGE_FMT.format(dateVal);
        } else {
            dateVal = NO_DATE_VALUE;
            storageVal = null;
        }
        node.setAttribute(VALUE_ATTR, storageVal);
        node.setAttribute(CACHED_DATE_ATTR, dateVal);
    }

    @Override
    public Object getConflictDisplayValue(String value, WBSNode node) {
        Object date = getValueAt(node);
        return CELL_RENDERER.format(date);
    }

    @Override
    public void storeConflictResolutionValue(Object storageValue, WBSNode node) {
        node.setAttribute(VALUE_ATTR, storageValue);
        node.setAttribute(CACHED_DATE_ATTR, null);
    }

    private static final DateFormat DISPLAY_FMT = DateFormat.getDateInstance();
    private static final SimpleDateFormat STORAGE_FMT = new SimpleDateFormat(
            "yyyy-MM-dd");


    public TableCellRenderer getCellRenderer() {
        return CELL_RENDERER;
    }

    public TableCellEditor getCellEditor() {
        return CELL_EDITOR;
    }

    private static class CellRenderer extends DataTableCellRenderer {

        @Override
        protected Object format(Object value) {
            return (value == null ? "" : DISPLAY_FMT.format(value));
        }

    }

    public static final CellRenderer CELL_RENDERER = new CellRenderer();

    private static class CellEditor extends JDateTimeChooserCellEditor {

        public CellEditor() {
            super(getDateFormatString());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            value = WrappedValue.unwrap(value);

            if (value == null) {
                for (int r = row; r-- > 1;) {
                    Object oneVal = table.getValueAt(r, column);
                    if (oneVal instanceof Date) {
                        value = (Date) oneVal;
                        break;
                    }
                }
            }

            return super.getTableCellEditorComponent(table, value, isSelected,
                row, column);
        }

    }

    public static final TableCellEditor CELL_EDITOR = new CellEditor();

    private static String getDateFormatString() {
        if (DISPLAY_FMT instanceof SimpleDateFormat) {
            return ((SimpleDateFormat) DISPLAY_FMT).toPattern();
        }
        return STORAGE_FMT.toPattern();
    }


}
