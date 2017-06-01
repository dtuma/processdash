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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.processdash.ui.lib.autocomplete.AssignedToComboBox;
import net.sourceforge.processdash.ui.lib.autocomplete.AutocompletingDataTableCellEditor;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.CalculatedDataColumn;
import teamdash.wbs.CustomEditedColumn;
import teamdash.wbs.CustomRenderedColumn;
import teamdash.wbs.DataTableModel;
import teamdash.wbs.ErrorValue;
import teamdash.wbs.ItalicCellRenderer;
import teamdash.wbs.WBSModel;
import teamdash.wbs.WBSNode;
import teamdash.wbs.WrappedValue;

public class TaskLabelColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CustomEditedColumn, CalculatedDataColumn,
        LabelSource {

    public static final String COLUMN_ID = "Labels";

    public static final String VALUE_ATTR = "Label";

    private static final String INHERITED_PREFIX = "Inherited_";


    private WBSModel wbsModel;

    private String explicitValueAttr;

    private String inheritedValueAttr;


    public TaskLabelColumn(DataTableModel dataModel) {
        this(dataModel, VALUE_ATTR);
    }

    protected TaskLabelColumn(DataTableModel dataModel, String attr) {
        this.columnID = COLUMN_ID;
        this.columnName = resources.getString("Labels.Name");
        this.preferredWidth = 200;
        this.wbsModel = dataModel.getWBSModel();
        this.explicitValueAttr = attr;
        this.inheritedValueAttr = INHERITED_PREFIX + attr;
        setConflictAttributeName(explicitValueAttr);
    }

    public boolean recalculate() {
        recalculate(wbsModel.getRoot(), null);
        return true;
    }

    private void recalculate(WBSNode node, String inheritedValue) {
        node.setAttribute(inheritedValueAttr, inheritedValue);
        String nodeValue = (String) node.getAttribute(explicitValueAttr);
        if (nodeValue != null)
            inheritedValue = nodeValue;

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0; i < children.length; i++)
            recalculate(children[i], inheritedValue);
    }

    public void storeDependentColumn(String ID, int columnNumber) {
    }

    public boolean isCellEditable(WBSNode node) {
        return true;
    }

    public Object getValueAt(WBSNode node) {
        String nodeValue = (String) node.getAttribute(explicitValueAttr);
        if (" ".equals(nodeValue))
            return null;
        if (nodeValue != null)
            return nodeValue;

        String inheritedValue = (String) node.getAttribute(inheritedValueAttr);
        if (inheritedValue != null)
            return new ErrorValue(inheritedValue, EFFECTIVE_LABEL_MESSAGE);

        return null;
    }

    public static String getEffectiveLabelsAt(WBSNode node) {
        String value = (String) node.getAttribute(VALUE_ATTR);
        if (value == null)
            value = (String) node.getAttribute(INHERITED_PREFIX + VALUE_ATTR);
        if (value != null)
            value = value.trim();
        return ("".equals(value) ? null : value);
    }

    public void setValueAt(Object aValue, WBSNode node) {
        String val = (String) aValue;

        if (val == null || val.trim().length() == 0) {
            val = " ";
        } else {
            // labels are separated by commas and/or whitespace.  They also
            // may not contain several other characters (which are reserved
            // for use by the search expression logic)
            String[] labels = val.split(NON_LABEL_CHARS_REGEXP);
            Arrays.sort(labels, String.CASE_INSENSITIVE_ORDER);
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < labels.length; i++)
                if (labels[i].length() > 0)
                    result.append(", ").append(labels[i]);
            val = result.toString();
            if (val.length() == 0)
                val = " ";
            else
                val = val.substring(2);
        }

        String inheritedValue = (String) node.getAttribute(inheritedValueAttr);
        if (val != null && val.equals(inheritedValue))
            val = null;
        else if (" ".equals(val) && inheritedValue == null)
            val = null;

        node.setAttribute(explicitValueAttr, val);
    }

    public String getLabels(WBSNode node) {
        String result = (String) WrappedValue.unwrap(getValueAt(node));
        if (" ".equals(result))
            result = null;
        return result;
    }

    public static String mergeLabels(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        TreeSet<String> labels = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        labels.addAll(Arrays.asList(a.split(NON_LABEL_CHARS_REGEXP)));
        labels.addAll(Arrays.asList(b.split(NON_LABEL_CHARS_REGEXP)));
        labels.remove("");
        if (labels.isEmpty())
            return " ";
        else
            return StringUtils.join(labels, ", ");
    }

    public static String subtractLabels(String a, String b) {
        if (a == null) return null;
        if (b == null) return a;
        List<String> labels = new ArrayList(Arrays.asList( //
                a.split(NON_LABEL_CHARS_REGEXP)));
        labels.removeAll(Arrays.asList(b.split(NON_LABEL_CHARS_REGEXP)));
        labels.remove("");
        return (labels.isEmpty() ? null : StringUtils.join(labels, ", "));
    }

    public static String convertToLabel(String text) {
        if (text == null || text.length() == 0)
            return null;

        String[] tokens = text.split(NON_LABEL_CHARS_REGEXP);
        return StringUtils.join(Arrays.asList(tokens), "_");
    }

    private static final String NON_LABEL_CHARS_REGEXP = "[,\u0000- |()]+";

    private static final Pattern LABEL_REGEXP = Pattern
            .compile("([^" + NON_LABEL_CHARS_REGEXP.substring(1) + ")");

    public TableCellRenderer getCellRenderer() {
        return LABEL_RENDERER;
    }

    public TableCellEditor getCellEditor() {
        return new TaskLabelCellEditor();
    }

    private class TaskLabelCellEditor extends AutocompletingDataTableCellEditor {

        private AssignedToComboBox comboBox;

        public TaskLabelCellEditor() {
            super(new AssignedToComboBox(false));
            comboBox = (AssignedToComboBox) getComboBox();
            comboBox.setWordPattern(LABEL_REGEXP);
            comboBox.setSeparatorChar(',');
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            // refresh the data model with the current set of known labels.
            comboBox.setInitialsList(new ArrayList(collectLabels(table)));

            // call super() so the editor setup timer will be restarted
            super.getTableCellEditorComponent(table, null, isSelected, row,
                column);

            // initialize the combo box contents and return it
            Object val = ErrorValue.unwrap(value);
            comboBox.setFullText(val == null ? "" : val.toString());
            return comboBox;
        }

        private Set collectLabels(JTable table) {
            DataTableModel dtm = (DataTableModel) table.getModel();
            WBSModel wbs = dtm.getWBSModel();
            SortedSet labels = new TreeSet();
            collectLabels(labels, wbs.getRoot());
            return labels;
        }

        private void collectLabels(Set labels, WBSNode node) {
            String nodeValue = (String) node.getAttribute(explicitValueAttr);
            if (nodeValue != null) {
                for (String l : nodeValue.split(NON_LABEL_CHARS_REGEXP))
                    if (l.length() > 0)
                        labels.add(l);
            }

            WBSNode[] children = wbsModel.getChildren(node);
            for (int i = 0; i < children.length; i++)
                collectLabels(labels, children[i]);
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox.getFullText();
        }

    }


    private static final String EFFECTIVE_LABEL_MESSAGE =
            resources.getString("Inherited_Tooltip");
    private static final TableCellRenderer LABEL_RENDERER =
            new ItalicCellRenderer(EFFECTIVE_LABEL_MESSAGE);

}
