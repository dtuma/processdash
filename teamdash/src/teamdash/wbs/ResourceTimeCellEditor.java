package teamdash.wbs;

import java.awt.Component;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.text.JTextComponent;

public class ResourceTimeCellEditor extends DataTableCellEditor {

    private DataTableModel dataModel;
    private int timePerPersonColumn = -1;
    private int editingRow;
    private Component editor;

    public ResourceTimeCellEditor(DataTableModel dataModel) {
        this.dataModel = dataModel;
    }

    public Component getTableCellEditorComponent
        (JTable table, Object value, boolean isSelected, int row, int column) {

        this.editingRow = row;
        editor = super.getTableCellEditorComponent
            (table, value, isSelected, row, column);
        return editor;
    }


    public boolean stopCellEditing() {
        if (newValueIsBad()) {
            promptForTime();
            return false;
        } else
            return super.stopCellEditing();
    }

    private boolean newValueIsBad() {
        if (!(editor instanceof JTextComponent)) return false;

        String value = ((JTextComponent) editor).getText();
        if (value == null) return false;
        if (CHAR.matcher(value).find() == false) return false;
        if (DIGIT.matcher(value).find() == true) return false;

        if (timePerPersonColumn == -1)
            timePerPersonColumn = dataModel.findColumn("Time Per Person");
        if (timePerPersonColumn == -1) return false;

        double timePerPerson = NumericDataValue.parse
            (dataModel.getValueAt(editingRow, timePerPersonColumn));
        return !(timePerPerson > 0);
    }

    private static final Pattern CHAR = Pattern.compile("[a-zA-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");


    private void promptForTime() {
        JTextComponent field = (JTextComponent) editor;
        String prompt = field.getText();
        prompt = prompt + "(???)";
        field.setText(prompt);
        int len = prompt.length();
        field.select(len-4, len-1);
    }

}
