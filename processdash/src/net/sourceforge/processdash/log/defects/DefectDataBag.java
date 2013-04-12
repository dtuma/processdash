// Copyright (C) 2007-2013 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.log.defects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;

public class DefectDataBag extends AbstractTableModel {

    public static final int INCLUDED = 0;
    public static final int PROJECT = 1;
    public static final int ID = 2;
    public static final int TYPE = 3;
    public static final int INJECTED = 4;
    public static final int REMOVED = 5;
    public static final int FIX_TIME = 6;
    public static final int FIX_DEFECT = 7;
    public static final int DESCRIPTION = 8;
    public static final int DATE = 9;

    public static final String[] ATTRS = { "Included", "Project", "ID", "Type",
            "Injected", "Removed", "Time", "Fix", "Description", "Date" };
    private static final String[] ATTRS_UPPER = new String[ATTRS.length];
    private static final String[] ATTRS_LOWER = new String[ATTRS.length];
    static {
        for (int i = 0; i < ATTRS.length; i++) {
            ATTRS_UPPER[i] = ATTRS[i].toUpperCase();
            ATTRS_LOWER[i] = ATTRS[i].toLowerCase();
        }
    }

    private static Resources resources = Resources.getDashBundle("Defects");

    private static String[] COLUMN_NAMES = resources.getStrings("Columns.",
            ATTRS, ".Name");
    public static String[] COLUMN_TOOLTIPS = resources.getStrings("Columns.",
            ATTRS, ".Tooltip");
    public static int[] COLUMN_WIDTHS = resources.getInts("Columns.", ATTRS,
            ".Width_");



    private List defectData = Collections.EMPTY_LIST;

    private StringMapper[] fieldMappers = new StringMapper[ATTRS.length];

    public void setDefectData(List data) {
        if (data == null)
            this.defectData = Collections.EMPTY_LIST;
        else
            this.defectData = new ArrayList(data);
        fireTableDataChanged();
    }

    public void setStringMapper(int column, StringMapper m) {
        if (getColumnClass(column) == String.class) {
            fieldMappers[column] = m;
            fireTableChanged(new TableModelEvent(this, 0, getRowCount() - 1,
                    column));
        }
    }

    public int getColumnCount() {
        return ATTRS.length;
    }

    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case INCLUDED:
            return Boolean.class;
        case DATE:
            return Date.class;
        default:
            return String.class;
        }
    }

    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getRowCount() {
        return defectData.size();
    }

    protected Map getItemForRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= defectData.size())
            return Collections.EMPTY_MAP;
        return (Map) defectData.get(rowIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Map row = getItemForRow(rowIndex);
        Object result = row.get(ATTRS[columnIndex]);
        if (result == null) result = row.get(ATTRS_UPPER[columnIndex]);
        if (result == null) result = row.get(ATTRS_LOWER[columnIndex]);

        if (columnIndex == INCLUDED && result == null)
            result = Boolean.TRUE;
        else if (fieldMappers[columnIndex] != null)
            result = fieldMappers[columnIndex].getString((String) result);
        return result;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex == INCLUDED);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        getItemForRow(rowIndex).put(ATTRS[columnIndex], aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void selectUnselectAll(boolean select) {
        Object newVal = (select ? Boolean.TRUE : Boolean.FALSE);
        for (Iterator i = defectData.iterator(); i.hasNext();) {
            Map row = (Map) i.next();
            row.put(ATTRS[INCLUDED], newVal);
        }
        fireTableChanged(new TableModelEvent(this, 0, getRowCount() - 1,
                INCLUDED));
    }

    public boolean isIncluded(int row) {
        return getValueAt(row, INCLUDED) == Boolean.TRUE;
    }

    public String getStringAt(int row, int col) {
        return getStringAt(row, col, "");
    }

    protected String getStringAt(int row, int col, String defaultVal) {
        Object result = getValueAt(row, col);
        if (result == null)
            return defaultVal;
        else
            return StringUtils.canonicalizeNewlines(result.toString());
    }

    public Date getDate(int row) {
        Object result = getValueAt(row, DATE);
        if (result == null)
            return null;
        else if (result instanceof Date)
            return (Date) result;
        else try {
            return FormatUtil.parseDate(result.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasData(int column) {
        for (int row = getRowCount();  row-- > 0; ) {
            Object value = getValueAt(row, column);
            if (value != null && !"".equals(value))
                return true;
        }
        return false;
    }

    public List getAsDefectList() {
        List result = new ArrayList(defectData.size());
        for (int row = 0;  row < getRowCount();  row++) {
            if (!isIncluded(row))
                continue;

            Defect d = new Defect();
            d.number = getStringAt(row, ID);
            if ("".equals(d.number)) d.number = null;
            d.defect_type = getStringAt(row, TYPE);
            d.phase_injected = getStringAt(row, INJECTED);
            d.phase_removed = getStringAt(row, REMOVED);
            d.fix_time = getStringAt(row, FIX_TIME, "0");
            d.fix_defect = getStringAt(row, FIX_DEFECT);
            d.description = getStringAt(row, DESCRIPTION);
            d.date = getDate(row);

            result.add(d);
        }
        return result;
    }

    public boolean hasSelectedDefects() {
        for (int row = getRowCount();  row-- > 0; )
            if (isIncluded(row))
                return true;
        return false;
    }

}
