package teamdash;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;


/*
 * The editor button that brings up the dialog.
 * We extend DefaultCellEditor for convenience,
 * even though it mean we have to create a dummy
 * check box.  Another approach would be to copy
 * the implementation of TableCellEditor methods
 * from the source code for DefaultCellEditor.
 */
class ColorCellEditor extends DefaultCellEditor {
    Color currentColor = null;

    public ColorCellEditor(JButton b) {
        super(new JCheckBox()); //Unfortunately, the constructor
                                //expects a check box, combo box,
                                //or text field.
        editorComponent = b;
        setClickCountToStart(1); //This is usually 1 or 2.

        //Must do this so that editing stops when appropriate.
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
    }

    protected void fireEditingStopped() {
        super.fireEditingStopped();
    }

    public Object getCellEditorValue() {
        return currentColor;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        ((JButton)editorComponent).setText(value.toString());
        currentColor = (Color)value;
        return editorComponent;
    }

    //Set up the editor for the Color cells.
    static void setUpColorEditor(JTable table) {
        //First, set up the button that brings up the dialog.
        final JButton button = new JButton("") {
                public void setText(String s) {
                    //Button never shows text -- only color.
                }
            };
        button.setBackground(Color.white);
        button.setBorderPainted(false);
        button.setMargin(new Insets(0,0,0,0));

        //Now create an editor to encapsulate the button, and
        //set it up as the editor for all Color cells.
        final ColorCellEditor colorEditor =
            new ColorCellEditor(button);
        table.setDefaultEditor(Color.class, colorEditor);

        //Set up the dialog that the button brings up.
        final JColorChooser colorChooser = new JColorChooser();
        ActionListener okListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                colorEditor.currentColor = colorChooser.getColor();
            }
        };
        final JDialog dialog = JColorChooser.createDialog
            (button, "Pick a Color", true, colorChooser, okListener, null);

        //Here's the code that brings up the dialog.
        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    button.setBackground(colorEditor.currentColor);
                    colorChooser.setColor(colorEditor.currentColor);
                    //Without the following line, the dialog comes up
                    //in the middle of the screen.
                    //dialog.setLocationRelativeTo(button);
                    dialog.show();
                }
            });
    }
}
