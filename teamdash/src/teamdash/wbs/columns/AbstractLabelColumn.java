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
import java.util.Collections;
import java.util.Iterator;
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

public class AbstractLabelColumn extends AbstractDataColumn implements
        CustomRenderedColumn, CustomEditedColumn, CalculatedDataColumn,
        LabelSource {

    protected DataTableModel dataModel;
    protected WBSModel wbsModel;
    private String explicitAttr;
    private String inheritedAttr;
    private String separatorRegexp;
    String labelPrefix;
    private boolean autocomplete, constrained;

    protected AbstractLabelColumn() {}

    protected void init(DataTableModel dataModel, String columnID,
            String columnName, int columnWidth, String attribute,
            boolean multivalued, String labelPrefix, boolean inherits,
            boolean autocomplete, boolean constrained) {

        this.dataModel = dataModel;
        this.wbsModel = dataModel.getWBSModel();
        this.columnID = columnID;
        this.columnName = columnName;
        this.preferredWidth = columnWidth > 0 ? columnWidth : 200;
        this.explicitAttr = attribute;
        if (inherits)
            this.inheritedAttr = "Inherited_" + attribute;
        this.separatorRegexp = (multivalued ? "," : null);
        this.labelPrefix = labelPrefix;
        this.autocomplete = autocomplete;
        this.constrained = constrained;
        setConflictAttributeName(attribute);
    }

    public boolean isAutocomplete() {
        return autocomplete;
    }

    public boolean isInherits() {
        return inheritedAttr != null;
    }

    public boolean recalculate() {
        if (inheritedAttr != null)
            recalculate(wbsModel.getRoot(), null);
        return true;
    }

    private void recalculate(WBSNode node, String inheritedValue) {
        node.setAttribute(inheritedAttr, inheritedValue);
        String nodeValue = (String) node.getAttribute(explicitAttr);
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
        String nodeValue = (String) node.getAttribute(explicitAttr);
        if (EMPTY_VALUE.equals(nodeValue))
            return null;
        if (nodeValue != null)
            return nodeValue;

        String inheritedValue = getInheritedVal(node);
        if (inheritedValue != null)
            return new ErrorValue(inheritedValue, EFFECTIVE_LABEL_MESSAGE);

        return null;
    }

    public void setValueAt(Object aValue, WBSNode node) {
        List<String> values = getValues((String) aValue, true);

        String val;
        if (values.isEmpty()) {
            // if the user is deleting the value, record this as a space char
            val = EMPTY_VALUE;

        } else {
            // sort and normalize the list of values
            Collections.sort(values, String.CASE_INSENSITIVE_ORDER);
            val = StringUtils.join(values, ", ");
        }

        // check to see if the value was just reset to equal the inherited val.
        String inheritedValue = getInheritedVal(node);
        if (val != null && val.equals(inheritedValue))
            val = null;
        else if (EMPTY_VALUE.equals(val) && inheritedValue == null)
            val = null;

        // store the value in our explicit attribute
        node.setAttribute(explicitAttr, val);
    }

    protected String getInheritedVal(WBSNode node) {
        if (inheritedAttr == null)
            return null;
        else
            return (String) node.getAttribute(inheritedAttr);
    }

    protected List<String> getValues(String s, boolean normalize) {
        if (s == null || s.equals(EMPTY_VALUE))
            return Collections.EMPTY_LIST;

        s = s.trim();
        if (s.length() == 0)
            return Collections.EMPTY_LIST;

        String[] values;
        if (separatorRegexp == null)
            values = new String[] { s };
        else
            values = s.split(separatorRegexp);

        List<String> result = new ArrayList<String>(values.length);
        for (String oneValue : values) {
            oneValue = oneValue.trim();
            if (normalize)
                oneValue = normalizeValue(oneValue);
            if (oneValue != null && oneValue.length() > 0)
                result.add(oneValue);
        }

        return result;
    }

    protected String normalizeValue(String s) {
        return s;
    }

    public String getLabels(WBSNode node) {
        if (labelPrefix == null)
            return null;

        String nodeValue = (String) WrappedValue.unwrap(getValueAt(node));
        List<String> values = getValues(nodeValue, false);
        if (values.isEmpty())
            return null;

        StringBuilder result = new StringBuilder();
        for (String oneValue : values)
            result.append(",").append(convertToLabel(labelPrefix + oneValue));
        return result.substring(1);
    }

    public static String convertToLabel(String text) {
        if (text == null || text.length() == 0)
            return null;

        String[] tokens = text.split(NON_LABEL_CHARS_REGEXP);
        return StringUtils.join(Arrays.asList(tokens), "_");
    }

    private static final String NON_LABEL_CHARS_REGEXP = "[,\u0000- |()]+";

    public TableCellRenderer getCellRenderer() {
        return LABEL_RENDERER;
    }

    public TableCellEditor getCellEditor() {
        if (autocomplete == false)
            return null;
        else if (",".equals(separatorRegexp))
            return new TaskMultiLabelCellEditor();
        else
            return new TaskLabelCellEditor();
    }

    public TableCellEditor getFilterEditor() {
        if (autocomplete == false)
            return null;
        else
            return new TaskMultiLabelCellEditor();
    }

    protected Set<String> getAutocompleteValues() {
        return getValuesInUse();
    }

    protected Set<String> getValuesInUse() {
        SortedSet values = new TreeSet();
        collectValuesInUse(values, wbsModel.getRoot());
        return values;
    }

    protected void collectValuesInUse(Set<String> values, WBSNode node) {
        String nodeValue = (String) node.getAttribute(explicitAttr);
        if (nodeValue != null && !nodeValue.equals(EMPTY_VALUE)) {
            values.addAll(getValues(nodeValue, false));
        }

        WBSNode[] children = wbsModel.getChildren(node);
        for (int i = 0; i < children.length; i++)
            collectValuesInUse(values, children[i]);
    }

    private class TaskLabelCellEditor extends AutocompletingDataTableCellEditor {

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            // refresh the data model with the current set of known labels.
            Set labels = getAutocompleteValues();
            getComboBox().removeAllItems();
            for (Iterator i = labels.iterator(); i.hasNext();)
                getComboBox().addItem(i.next());

            // now defer to the parent for the rest of the work.
            return super.getTableCellEditorComponent(table, ErrorValue
                    .unwrap(value), isSelected, row, column);
        }

    }

    private class TaskMultiLabelCellEditor extends AutocompletingDataTableCellEditor {

        private AssignedToComboBox comboBox;

        public TaskMultiLabelCellEditor() {
            super(new AssignedToComboBox(constrained));
            comboBox = (AssignedToComboBox) getComboBox();
            comboBox.setWordPattern(WORD_REGEXP);
            comboBox.setSeparatorChar(',');
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            // refresh the data model with the current set of known labels.
            comboBox.setInitialsList(new ArrayList(getAutocompleteValues()));

            // call super() so the editor setup timer will be restarted
            super.getTableCellEditorComponent(table, null, isSelected, row,
                column);

            // initialize the combo box contents and return it
            Object val = ErrorValue.unwrap(value);
            comboBox.setFullText(val == null ? "" : val.toString());
            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox.getFullText();
        }

    }


    private static final String EMPTY_VALUE = " ";
    private static final String EFFECTIVE_LABEL_MESSAGE = "Inherited Value";
    private static final TableCellRenderer LABEL_RENDERER =
        new ItalicCellRenderer(EFFECTIVE_LABEL_MESSAGE);
    private static final Pattern WORD_REGEXP = Pattern.compile("([^ ,][^,]*)");

}
