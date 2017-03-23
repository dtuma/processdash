// Copyright (C) 2012-2017 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


/**
 * An action to hide or show columns in a JTable.
 */
public class JTableColumnVisibilityAction extends AbstractAction {

    private JTable table;

    private ResourceBundle resources;

    private Pattern readOnlyColumnNamePattern;

    private Map<Integer, TableColumn> columnCache;

    public JTableColumnVisibilityAction(JTable table, ResourceBundle resources,
            String readOnlyNamePat, int... readOnlyColumns) {
        this.table = table;
        this.resources = resources;
        this.columnCache = buildColumnCache(table.getColumnModel());

        setReadOnlyColumnNamePattern(readOnlyNamePat);
        for (int i : readOnlyColumns)
            makeColumnReadOnly(i);

        putValue(Action.SMALL_ICON, new ColumnSelectorIcon(true));
        putValue(Action.NAME, resources.getString("Column_Chooser.Title"));

        table.getActionMap().put(ACTION_KEY, this);
    }

    private Map<Integer, TableColumn> buildColumnCache(
            TableColumnModel columnModel) {
        Map<Integer, TableColumn> result = new HashMap<Integer, TableColumn>();
        for (int i = columnModel.getColumnCount(); i-- > 0;) {
            TableColumn column = columnModel.getColumn(i);
            result.put(column.getModelIndex(), column);
        }
        return result;
    }

    public void makeColumnReadOnly(int modelIndex) {
        columnCache.remove(modelIndex);
    }

    public void setReadOnlyColumnNamePattern(String pat) {
        this.readOnlyColumnNamePattern = (pat == null ? null
                : Pattern.compile(pat));
    }

    public void actionPerformed(ActionEvent e) {
        showColumnChooserDialog();
    }

    private void showColumnChooserDialog() {
        CheckboxList list = createColumnSelector();
        JScrollPane sp = new JScrollPane(list);
        Dimension d = list.getPreferredSize();
        d.width = Math.max(d.width, 200);
        d.height = Math.min(d.height + 5, 300);
        sp.setPreferredSize(d);

        String title = resources.getString("Column_Chooser.Title");
        Object[] message = new Object[] {
                resources.getString("Column_Chooser.Prompt"), sp,
                new JOptionPaneTweaker.MakeResizable() };
        int userChoice = JOptionPane.showConfirmDialog(table, message, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (userChoice == JOptionPane.OK_OPTION)
            applyColumnSelections(list);
    }

    private CheckboxList createColumnSelector() {
        Set<Integer> visibleColumns = new HashSet<Integer>();
        Set<Integer> hiddenColumns = new HashSet<Integer>();
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = columnModel.getColumnCount(); i-- > 0;) {
            TableColumn column = columnModel.getColumn(i);
            if (column.getMaxWidth() == 0)
                hiddenColumns.add(i);
            else
                visibleColumns.add(column.getModelIndex());
        }

        TableModel tableModel = table.getModel();
        List<NumberedColumn> columns = new ArrayList();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String columnName = tableModel.getColumnName(i);
            if (columnCache.containsKey(i)
                    && !hiddenColumns.contains(i)
                    && !isReadOnlyColumnName(columnName)) {
                boolean isVisible = visibleColumns.contains(i);
                columns.add(new NumberedColumn(i, columnName, isVisible));
            }
        }

        CheckboxList list = new CheckboxList(columns.toArray());
        for (int i = columns.size(); i-- > 0;)
            list.setChecked(i, columns.get(i).initiallyVisible);

        return list;
    }

    private boolean isReadOnlyColumnName(String name) {
        return readOnlyColumnNamePattern != null
                && readOnlyColumnNamePattern.matcher(name).matches();
    }

    private void applyColumnSelections(CheckboxList list) {
        TableColumnModel columnModel = table.getColumnModel();

        for (int i = 0; i < list.getRowCount(); i++) {
            NumberedColumn col = (NumberedColumn) list.getValueAt(i, 1);
            boolean shouldBeVisible = list.getChecked(i);
            if (shouldBeVisible == col.initiallyVisible)
                continue;

            int viewPos = table.convertColumnIndexToView(col.modelIndex);
            if (shouldBeVisible && viewPos == -1) {
                addColumnToModel(columnModel, col.modelIndex);
            } else if (shouldBeVisible == false && viewPos != -1) {
                columnModel.removeColumn(columnModel.getColumn(viewPos));
            }
        }
    }

    public void saveColumnVisibility(Preferences prefs, TableColumnModel orig,
            TableColumnModel changed) {
        for (TableColumn column : columnCache.values()) {
            Object id = column.getIdentifier();
            boolean inOrig = modelContainsColumn(orig, id);
            boolean inChanged = modelContainsColumn(changed, id);
            String prefsKey = id + ".visibility";
            if (inOrig == inChanged)
                prefs.remove(prefsKey);
            else if (inOrig)
                prefs.put(prefsKey, COLUMN_REMOVED);
            else
                prefs.put(prefsKey, COLUMN_ADDED);
        }
    }

    public void loadColumnVisibility(Preferences prefs) {
        TableColumnModel columnModel = table.getColumnModel();
        for (TableColumn column : columnCache.values()) {
            Object id = column.getIdentifier();
            String prefsKey = id + ".visibility";
            String pref = prefs.get(prefsKey, null);
            if (pref == null)
                continue;

            int viewPos = getColumnIndex(columnModel, id);
            if (COLUMN_ADDED.equals(pref) && viewPos == -1) {
                addColumnToModel(columnModel, column.getModelIndex());
            } else if (COLUMN_REMOVED.equals(pref) && viewPos != -1) {
                columnModel.removeColumn(columnModel.getColumn(viewPos));
            }
        }
    }

    private void addColumnToModel(TableColumnModel columnModel, int modelIndex) {
        TableColumn newCol = columnCache.get(modelIndex);
        if (newCol != null) {
            int appendPos = columnModel.getColumnCount();
            columnModel.addColumn(TableUtils.cloneTableColumn(newCol));
            int destPos = getPreferredInsertionPos(newCol.getModelIndex());
            if (destPos != -1 && destPos != appendPos)
                columnModel.moveColumn(appendPos, destPos);
        }
    }

    private int getPreferredInsertionPos(int modelIndex) {
        while (modelIndex-- > 0) {
            int viewPos = table.convertColumnIndexToView(modelIndex);
            if (viewPos != -1)
                return viewPos + 1;
        }
        return -1;
    }

    private boolean modelContainsColumn(TableColumnModel columns, Object id) {
        return getColumnIndex(columns, id) != -1;
    }

    private int getColumnIndex(TableColumnModel columns, Object id) {
        for (int i = columns.getColumnCount(); i-- > 0;)
            if (id.equals(columns.getColumn(i).getIdentifier()))
                return i;
        return -1;
    }

    public static JTableColumnVisibilityAction getForTable(JTable t) {
        return (JTableColumnVisibilityAction) t.getActionMap().get(ACTION_KEY);
    }

    private class NumberedColumn {
        int modelIndex;

        String name;

        boolean initiallyVisible;

        public NumberedColumn(int modelIndex, String name,
                boolean initiallyVisible) {
            this.modelIndex = modelIndex;
            this.name = name;
            this.initiallyVisible = initiallyVisible;
        }

        public String toString() {
            return name;
        }
    }

    private static final String ACTION_KEY =
        JTableColumnVisibilityButton.class.getSimpleName();

    private static final String COLUMN_REMOVED = "removed";

    private static final String COLUMN_ADDED = "added";

}
