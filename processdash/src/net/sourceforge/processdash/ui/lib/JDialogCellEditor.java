package net.sourceforge.processdash.ui.lib;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;


/**
 * A table cell editor implementation that brings up a dialog window to perform
 * the editing of a value.
 */
public abstract class JDialogCellEditor<T> extends DefaultCellEditor implements
        ActionListener {

    protected JButton button;

    protected T currentValue = null;


    public JDialogCellEditor() {
        // unfortunately, our constructor expects a check box, combo box,
        // or a text field, so we must supply a dummy object.
        super(new JCheckBox());

        // create a JButton that we will use as the actual editor.
        button = new JButton("");
        button.setBorderPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(this);

        // install our JButton as the editor component
        editorComponent = button;
        setClickCountToStart(1);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            currentValue = showEditorDialog(currentValue);
            fireEditingStopped();
        } catch (EditingCancelled ec) {
            fireEditingCanceled();
        }
    }

    protected abstract T showEditorDialog(T value) throws EditingCancelled;

    protected String getButtonText(T value) {
        return (value == null ? "" : value.toString());
    }

    public Object getCellEditorValue() {
        return currentValue;
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        button.setBackground(isSelected ? table.getSelectionBackground()
                : table.getBackground());
        button.setText(getButtonText((T) value));

        currentValue = (T) value;
        return editorComponent;
    }

    public static class EditingCancelled extends Exception {}

}
