package teamdash.process;

import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import teamdash.process.CustomProcess.Item;

public class ProcessPhaseTableModel extends ItemListTableModel {

    private static final String PHASE_ITEM = CustomProcess.PHASE_ITEM;
    private static final String LONG_NAME = CustomProcess.LONG_NAME;
    private static final String NAME = CustomProcess.NAME;
    private static final String TYPE = CustomProcess.TYPE;

    static final int LONG_NAME_COL = 0;
    static final int SHORT_NAME_COL = 1;
    static final int TYPE_COL = 2;
    static final int SIZE_METRIC_COL = 3;

    private CustomProcess customProcess;

    public ProcessPhaseTableModel(CustomProcess p) {
        super(p, PHASE_ITEM);
        this.customProcess = p;
    }


    public void insertItem(int pos) {
        Item newPhase = customProcess.new Item(PHASE_ITEM);
        newPhase.putAttr(LONG_NAME, "Enter Phase Name");
        newPhase.putAttr(NAME, "Short Name");
        newPhase.putAttr(TYPE, "develop");
        super.insertItem(newPhase, pos);
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



    protected Object getItemDisplay(int column, Object value) {
        if (column == TYPE_COL) {
            String text = (String) value;
            return getPhaseDisplay(text).trim();
        } else {
            return value;
        }
    }


    public JTable createJTable() {
        JTable table = new JTable(this);

        // adjust column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        table.setRowHeight(table.getRowHeight()+4);

        // draw read-only phases with a different appearance
        table.setDefaultRenderer(String.class, new ItemTableCellRenderer());

        // install a combo box as the editor for the "phase type" column
        TableColumn typeColumn = table.getColumnModel().getColumn(2);
        JComboBox phaseTypeEditor = new JComboBox(PHASE_TYPES);
        phaseTypeEditor.setRenderer(new PhaseListCellRenderer());
        phaseTypeEditor.setFont
            (phaseTypeEditor.getFont().deriveFont(Font.PLAIN));
        typeColumn.setCellEditor(new DefaultCellEditor(phaseTypeEditor));

        return table;
    }

    private class PhaseListCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            value = getPhaseDisplay((String) value);
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }

    }


    private static final String[] PHASE_TYPES = { "overhead", "develop",
        "appraisal", "review", "insp", "failure" };

    private static final Map PHASE_DISPLAY_NAMES = new HashMap();
    private static void addPhaseName(String type, String displayName) {
        PHASE_DISPLAY_NAMES.put(type,  displayName);
        PHASE_DISPLAY_NAMES.put(type.toLowerCase(),  displayName);
        PHASE_DISPLAY_NAMES.put(type.toUpperCase(),  displayName);
    }
    static {
        // standard entries first
        addPhaseName("overhead", "Overhead");
        addPhaseName("develop",   "Development");
        addPhaseName("appraisal", "Appraisal");
        addPhaseName("review",    "   Review");
        addPhaseName("insp",      "   Inspection");
        addPhaseName("failure",   "Failure");

        // now entries for the PSP specific phases
        addPhaseName("plan", "Overhead");
        addPhaseName("dld",  "Development");
        addPhaseName("dldr", "Review");
        addPhaseName("code", "Development");
        addPhaseName("cr",   "Review");
        addPhaseName("comp", "Failure");
        addPhaseName("ut",   "Failure");
        addPhaseName("pm",   "Overhead");
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
