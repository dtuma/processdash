
package teamdash;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;


public class TeamMemberListEditor extends JPanel {

    TeamMemberList teamMemberList;

    public TeamMemberListEditor() {
        // create the table model
        teamMemberList = new TeamMemberList();

        // create the table
        JTable table = new JTable(teamMemberList);

        //Set up renderer and editor for the Color column.
        ColorRenderer.setUpColorRenderer(table);
        ColorEditor.setUpColorEditor(table);

        JScrollPane scrollPane = new JScrollPane(table);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

}



class ColorRenderer extends JLabel implements TableCellRenderer {
    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;

    public ColorRenderer(boolean isBordered) {
        super(); //"foo");
        this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }

    private Border getSelectedBorder(JTable table) {
        if (selectedBorder == null)
            selectedBorder = BorderFactory.createMatteBorder
                (2, 5, 2, 5, table.getSelectionBackground());
        return selectedBorder;
    }
    private Border getUnselectedBorder(JTable table) {
        if (unselectedBorder == null)
            unselectedBorder = BorderFactory.createMatteBorder
                (2,5,2,5, table.getBackground());
        return unselectedBorder;
    }

    public Component getTableCellRendererComponent
        (JTable table, Object color, boolean isSelected,
         boolean hasFocus, int row, int column)
    {
        setBackground((Color) color);
        setForeground(xorColor((Color) color));
        if (isBordered)
            setBorder(isSelected
                      ? getSelectedBorder(table)
                      : getUnselectedBorder(table));

        return this;
    }

    private static HashMap XOR_COLORS = new HashMap();
    private static Color xorColor(Color c) {
        Color result = (Color) XOR_COLORS.get(c);
        if (result == null) {
            result = new Color(c.getRed() ^ 255,
                               c.getGreen() ^ 255,
                               c.getBlue() ^ 255);
            XOR_COLORS.put(c, result);
        }
        return result;
    }

    static void setUpColorRenderer(JTable table) {
        table.setDefaultRenderer(Color.class, new ColorRenderer(true));
    }

}




/*
 * The editor button that brings up the dialog.
 * We extend DefaultCellEditor for convenience,
 * even though it mean we have to create a dummy
 * check box.  Another approach would be to copy
 * the implementation of TableCellEditor methods
 * from the source code for DefaultCellEditor.
 */
class ColorEditor extends DefaultCellEditor {
    Color currentColor = null;

    public ColorEditor(JButton b) {
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
        final ColorEditor colorEditor = new ColorEditor(button);
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
