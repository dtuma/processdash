// Copyright (C) 2002-2020 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.mcf.editor;

import java.awt.Component;
import java.awt.Font;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import net.sourceforge.processdash.team.mcf.CustomProcess;
import net.sourceforge.processdash.team.mcf.CustomProcess.Item;

public class ProcessPhaseTableModel extends ItemListTableModel {


    private static final String PHASE_ITEM = CustomProcess.PHASE_ITEM;
    private static final String LONG_NAME = CustomProcess.LONG_NAME;
    private static final String NAME = CustomProcess.NAME;
    private static final String TYPE = CustomProcess.TYPE;

    static final int LONG_NAME_COL = 0;
    static final int SHORT_NAME_COL = 1;
    static final int TYPE_COL = 2;
    static final int SIZE_METRIC_COL = 3;


    public ProcessPhaseTableModel(CustomProcess p) {
        super(p, PHASE_ITEM);

        pasteComparisonColumns = new int[] { SHORT_NAME_COL };
    }

    public void initNewProcess() {
        insertItem(0);
        setValueAt(NEW_PROCESS_LONG_NAME, 0, LONG_NAME_COL);
        markAllItemsInserted();
        clearDirty();
    }

    public int insertItem(int pos) {
        Item newPhase = getProcess().new Item(PHASE_ITEM);
        newPhase.putAttr(LONG_NAME, DEFAULT_LONG_NAME);
        newPhase.putAttr(NAME, DEFAULT_SHORT_NAME);
        newPhase.putAttr(TYPE, "develop");
        return super.insertItem(newPhase, pos);
    }

    private static final String[] columnNames = {
        "Descriptive Name", "Short Name", "Type", "Size Metric" };

    public String getColumnName(int col) { return columnNames[col]; }

    private static final String[] columnAttrs = { CustomProcess.LONG_NAME,
            CustomProcess.NAME, CustomProcess.TYPE, CustomProcess.SIZE_METRIC };

    protected String[] getColumnAttrs() {
        return columnAttrs;
    }

    protected boolean isStructuralColumn(int column) {
        return column == SHORT_NAME_COL;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == LONG_NAME_COL
                || super.isCellEditable(rowIndex, columnIndex);
    }

    protected Object getItemDisplay(int column, Object value) {
        if (column == TYPE_COL) {
            String text = (String) value;
            return getPhaseDisplay(text).trim();
        } else {
            return value;
        }
    }

    protected Object filterValidValue(Object aValue, int row, int col) {
        if (col == TYPE_COL) {
            String type = ((String) aValue).toLowerCase();
            if (PHASE_DISPLAY_NAMES.containsKey(type))
                return type;
            else
                return INVALID_VALUE;
        } else
            return super.filterValidValue(aValue, row, col);
    }



    public void checkForErrors(Set errors) {
        checkForMissingField(errors, LONG_NAME_COL, DISCARDABLE_VALUES,
                "Every phase must have a descriptive name.");
        checkForMissingField(errors, SHORT_NAME_COL, DISCARDABLE_VALUES,
                "Every phase must have a short name.");
        checkForDuplicateFields(errors, new int[] { LONG_NAME_COL, SHORT_NAME_COL },
                "There is more than one phase named \"{0}\". "
                        + "Phase names must be unique.", DISCARDABLE_VALUES);
        checkForSizeMetricsErrors(errors);
    }

    private void checkForSizeMetricsErrors(Set errors) {
        Set validSizeValues = new HashSet();
        List sizeList = getProcess().getItemList(CustomProcess.SIZE_METRIC);
        for (Iterator i = sizeList.iterator(); i.hasNext();) {
            Item sizeMetric = (Item) i.next();
            String name = sizeMetric.getAttr(NAME);
            validSizeValues.add(name.toLowerCase());
        }
        validSizeValues.add("DLD Lines".toLowerCase());
        validSizeValues.add("LOC".toLowerCase());
        validSizeValues.add("");

        for (int row = 0;   row < getRowCount();  row++) {
            String phaseSize = (String) getValueAt(row, SIZE_METRIC_COL);
            if (phaseSize != null
                    && !validSizeValues.contains(phaseSize.toLowerCase())) {
                String phaseName = (String) getValueAt(row, LONG_NAME_COL);
                String errMsg = MessageFormat.format("Phase \"{0}\" is using "
                        + "size metric \"{1}\", but there is no size metric "
                        + "with that name.",
                      new Object[] { phaseName, phaseSize });
                errors.add(errMsg);
            }
        }
    }


    public JTable createJTable() {
        JTable table = new ItemListJTable(this);

        // adjust column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        table.setRowHeight(table.getRowHeight()+4);

        // draw read-only phases with a different appearance
        table.setDefaultRenderer(String.class, new ItemTableCellRenderer());

        // install a combo box as the editor for the "phase type" column
        TableColumn column = table.getColumnModel().getColumn(TYPE_COL);
        column.setCellEditor(createPhaseTypeEditor());

        // install a combo box as the editor for the "size metric" column
        column = table.getColumnModel().getColumn(SIZE_METRIC_COL);
        column.setCellEditor(new SizeCellEditor());

        return table;
    }


    private DefaultCellEditor createPhaseTypeEditor() {
        JComboBox phaseTypeEditor = new JComboBox(PHASE_TYPES);
        phaseTypeEditor.setRenderer(new PhaseListCellRenderer());
        phaseTypeEditor.setFont
            (phaseTypeEditor.getFont().deriveFont(Font.PLAIN));
        return new DefaultCellEditor(phaseTypeEditor);
    }

    private static final String[] PHASE_TYPES = { "overhead", "develop",
        "appraisal", "review", "insp", "failure" };

    private class PhaseListCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            value = getPhaseDisplay((String) value);
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }

    }

    private class SizeCellEditor extends DefaultCellEditor {

        private JComboBox comboBox;

        public SizeCellEditor() {
            super(new JComboBox());
            comboBox = (JComboBox) getComponent();
            comboBox.setFont(comboBox.getFont().deriveFont(Font.PLAIN));
        }

        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            comboBox.removeAllItems();
            comboBox.addItem(null);
            List sizes = getProcess().getItemList(CustomProcess.SIZE_METRIC);
            for (Iterator i = sizes.iterator(); i.hasNext();) {
                Item sizeMetric = (Item) i.next();
                String name = sizeMetric.getAttr(NAME);
                if (name != null && name.length() > 0)
                    comboBox.addItem(name);
            }
            comboBox.addItem("DLD Lines");

            return super.getTableCellEditorComponent(table, value, isSelected,
                    row, column);
        }

    }



    private static final String NEW_PROCESS_LONG_NAME = "Add your own phases";
    private static final String DEFAULT_LONG_NAME = "Enter Phase Name";
    private static final String DEFAULT_SHORT_NAME = "Short Name";

    private static final List DISCARDABLE_VALUES = Arrays.asList(new String[] {
        DEFAULT_LONG_NAME, DEFAULT_SHORT_NAME, NEW_PROCESS_LONG_NAME, null, "" });


    protected boolean rowIsDiscardable(int row) {
        if (row >= 0 && row < getRealRowCount()) {
            Item item = get(row);
            return (DISCARDABLE_VALUES.contains(item.getAttr(LONG_NAME))
                    && DISCARDABLE_VALUES.contains(item.getAttr(NAME)));
        } else
            return super.rowIsDiscardable(row);
    }



    private static final String OVERHEAD = "Overhead";
    private static final String DEVELOP = "Development";
    private static final String APPRAISAL = "Appraisal";
    private static final String REVIEW = "Review";
    private static final String INSPECT = "Inspection";
    private static final String FAILURE = "Failure";


    private static final Map PHASE_DISPLAY_NAMES = new HashMap();

    private static void addPhaseName(String[] type, String displayName) {
        for (int i = 0; i < type.length; i++)
            addPhaseName(type[i], displayName);
    }
    private static void addPhaseName(String type, String displayName) {
        PHASE_DISPLAY_NAMES.put(type,  displayName);
        PHASE_DISPLAY_NAMES.put(type.toLowerCase(),  displayName);
        PHASE_DISPLAY_NAMES.put(type.toUpperCase(),  displayName);
    }
    static {
        // standard entries first
        addPhaseName("overhead", OVERHEAD);
        addPhaseName("develop",   DEVELOP);
        addPhaseName("appraisal", APPRAISAL);
        addPhaseName("review",    "   " + REVIEW);
        addPhaseName("insp",      "   " + INSPECT);
        addPhaseName("failure",   FAILURE);

        // now entries for other recognized phase types
        addPhaseName(new String[] { "mgmt", "strat", "plan", "pm" }, OVERHEAD);
        addPhaseName(new String[] { "req", "stp", "hld", "itp", "td", "dld",
                "code", "doc" }, DEVELOP);
        addPhaseName(new String[] { "reqinsp", "hldrinsp", "dldinsp",
                "codeinsp" }, INSPECT);
        addPhaseName(new String[] { "dldr", "cr" }, REVIEW);
        addPhaseName(new String[] { "comp", "ut", "it", "st", "at", "pl" },
                FAILURE);
    }

    public static String getPhaseDisplay(String phaseName) {
        if (phaseName == null) return "";

        String result = (String) PHASE_DISPLAY_NAMES.get(phaseName);
        if (result != null) return result;

        result = (String) PHASE_DISPLAY_NAMES.get(phaseName.toLowerCase());
        if (result != null) return result;

        return phaseName;
    }


}
