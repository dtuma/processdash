// Copyright (C) 2015 Tuma Solutions, LLC
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

package teamdash.wbs.columns;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.lib.JHintTextField;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.util.StringUtils;

import teamdash.wbs.DataTableModel;

public class CustomColumnEditor implements ActionListener {

    private DataTableModel dataModel;

    private JPanel contents;

    private GridBagLayout layout;

    private JTextField columnID;

    private JLabel columnIDLabel;

    private JTextField columnName;

    private JComboBox textColumnType;

    private JCheckBox autocomplete, multivalued, inherited, syncAsLabel;

    private JHintTextField allowedValues;


    private static final Resources resources = Resources
            .getDashBundle("WBSEditor.CustomColumns.Edit");


    public CustomColumnEditor(DataTableModel dataModel) {
        this.dataModel = dataModel;
        buildGUI();
    }

    private void buildGUI() {
        layout = new GridBagLayout();
        contents = new JPanel(layout);

        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.anchor = GridBagConstraints.EAST;
        l.insets = new Insets(0, 0, 5, 5);

        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 1;
        f.anchor = GridBagConstraints.WEST;
        f.insets = new Insets(0, 0, 5, 0);

        l.gridy = f.gridy = 0;
        add(new JLabel(resources.getString("Column_ID")), l);
        add(columnID = hintField("Column_ID_Hint", 15), f);
        add(columnIDLabel = new JLabel(), f);
        ((AbstractDocument) columnID.getDocument())
                .setDocumentFilter(new ColumnIDFilter());

        l.gridy = ++f.gridy;
        add(new JLabel(resources.getString("Column_Name")), l);
        add(columnName = hintField("Column_Name_Hint", 15), f);

        l.gridy = ++f.gridy;
        add(new JLabel(resources.getString("Column_Type")), l);
        add(textColumnType = new JComboBox(resources.getStrings("Column_Type_",
            TEXT_TYPES)), f);

        ++f.gridy;
        add(autocomplete = checkbox("Autocomplete", true), f);

        GridBagConstraints ff = (GridBagConstraints) f.clone();
        ff.fill = GridBagConstraints.HORIZONTAL;
        add(allowedValues = hintField("Values_Hint", 0), ff);
        add(Box.createHorizontalStrut(new JLabel(allowedValues.getHint())
                .getPreferredSize().width), f);

        ++f.gridy;
        add(multivalued = checkbox("Multivalued", false), f);

        ++f.gridy;
        add(inherited = checkbox("Inherited", true), f);

        ++f.gridy;
        add(syncAsLabel = checkbox("Sync_As_Label", false), f);

        textColumnType.addActionListener(this);
        actionPerformed(null);
    }

    private void add(Component comp, GridBagConstraints constraints) {
        contents.add(comp);
        layout.setConstraints(comp, constraints);
    }

    private JHintTextField hintField(String resKey, int columns) {
        return new JHintTextField(resources.getString(resKey), columns);
    }

    private JCheckBox checkbox(String resKey, boolean selected) {
        return new JCheckBox(resources.getString(resKey), selected);
    }

    public void actionPerformed(ActionEvent e) {
        if (e == null || e.getSource() == textColumnType) {
            int type = textColumnType.getSelectedIndex();
            autocomplete.setVisible(type == TEXT_TYPE_FREETEXT);
            allowedValues.setVisible(type == TEXT_TYPE_VALUES);
        }
    }


    /**
     * Show a window prompting the user for information about a new custom
     * column to be added.
     * 
     * @param parent
     *            the component to center the editing window over
     * @return a new custom column, or null if the user cancelled the operation
     */
    public CustomColumn showAddWindow(Component parent) {
        columnIDLabel.setVisible(false);
        textColumnType.setSelectedIndex(TEXT_TYPE_FREETEXT);
        return showWindow(parent, true);
    }


    /**
     * Show a window allowing the user to edit information about an existing
     * custom column.
     * 
     * @param parent
     *            the component to center the editing window over
     * @param columnToEdit
     *            the existing column to be edited
     * @return a new custom column, or null if the user cancelled the operation
     */
    public CustomColumn showEditWindow(Component parent,
            CustomColumn columnToEdit) {
        // only support editing of custom text columns for now
        if (!(columnToEdit instanceof CustomTextColumn)) {
            JOptionPane.showMessageDialog(parent,
                resources.getStrings("Errors.Unrecognized_Type"),
                resources.getString("Edit_Title"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
        CustomTextColumn oldCol = (CustomTextColumn) columnToEdit;

        // load the ID of the column we are editing
        String oldColumnID = oldCol.getColumnID();
        if (oldColumnID.startsWith(CUST_ID_PREFIX))
            oldColumnID = oldColumnID.substring(CUST_ID_PREFIX.length());
        columnIDLabel.setText(oldColumnID);
        columnID.setText(oldColumnID);
        columnID.setVisible(false);

        // load the column name
        columnName.setText(oldCol.getColumnName());

        // set the column type and configure allowed values for the column
        Set<String> values;
        if (oldCol.allowedValues != null) {
            textColumnType.setSelectedIndex(TEXT_TYPE_VALUES);
            values = oldCol.allowedValues;
        } else {
            textColumnType.setSelectedIndex(TEXT_TYPE_FREETEXT);
            values = oldCol.getAutocompleteValues();
        }
        allowedValues.setText(StringUtils.join(values, ", "));

        // load other details about the column
        autocomplete.setSelected(oldCol.autocomplete);
        multivalued.setSelected(oldCol.multivalued);
        inherited.setSelected(oldCol.inherits);
        syncAsLabel.setSelected(oldCol.labelPrefix != null);

        // show the editing window and get the result
        CustomColumn newCol = showWindow(parent, false);

        // if no changes were made, return null to indicate "edit cancelled"
        if (columnsEqual(oldCol, newCol))
            newCol = null;

        return newCol;
    }

    private CustomColumn showWindow(Component parent, boolean isAddOperation) {
        String title = resources.getString(isAddOperation ? "Add_Title"
                : "Edit_Title");
        JComponent fieldToFocus = isAddOperation ? columnID : columnName;

        while (true) {
            Object[] message = new Object[] { contents,
                    new JOptionPaneTweaker.GrabFocus(fieldToFocus) };
            int userChoice = JOptionPane.showConfirmDialog(parent, message,
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null);
            if (userChoice != JOptionPane.OK_OPTION)
                return null;

            try {
                return buildColumn(isAddOperation);
            } catch (ColumnException ce) {
                JOptionPane.showMessageDialog(parent, ce.getMessage(), title,
                    JOptionPane.ERROR_MESSAGE);
                if (ce.fieldToFocus != null)
                    fieldToFocus = ce.fieldToFocus;
            }
        }
    }

    private CustomColumn buildColumn(boolean checkUniqueID)
            throws ColumnException {
        // Ensure we have a nonempty column ID.
        String usrColumnID = columnID.getText().trim();
        if (usrColumnID.length() == 0)
            throw new ColumnException("ID_Missing", columnID);
        String newColumnID = CUST_ID_PREFIX + usrColumnID;

        // When adding a new column, ensure that the column ID is unique.
        if (checkUniqueID && dataModel.findColumn(newColumnID) != -1)
            throw new ColumnException("ID_Not_Unique_FMT", columnID, usrColumnID);

        // Ensure they entered a column name.
        String newColumnName = columnName.getText().trim();
        if (newColumnName.length() == 0)
            throw new ColumnException("Name_Missing", columnName);

        // Determine allowed values and autocompletion
        boolean auto_complete;
        Set<String> allowed_values;
        if (textColumnType.getSelectedIndex() == TEXT_TYPE_VALUES) {
            auto_complete = true;
            allowed_values = parseAllowedValues();
        } else { // TEXT_TYPE_FREETEXT
            auto_complete = autocomplete.isSelected();
            allowed_values = null;
        }

        // Determine the label prefix to use
        String labelPrefix = (syncAsLabel.isSelected() ? newColumnName : null);

        // build an appropriate column
        return new CustomTextColumn(dataModel, newColumnID, newColumnName,
                auto_complete, multivalued.isSelected(),
                inherited.isSelected(), allowed_values, labelPrefix);
    }

    private Set<String> parseAllowedValues() throws ColumnException {
        Set<String> result = new LinkedHashSet<String>();
        for (String oneValue : allowedValues.getText().split(",")) {
            oneValue = oneValue.trim();
            if (oneValue.length() > 0)
                result.add(oneValue);
        }

        if (result.isEmpty())
            throw new ColumnException("Values_Missing", allowedValues);

        return result;
    }

    private boolean columnsEqual(CustomColumn a, CustomColumn b) {
        if (a == b)
            return true;
        else if (a == null || b == null)
            return false;
        else
            return (getAsXml(a).equals(getAsXml(b)));
    }

    private String getAsXml(CustomColumn col) {
        StringWriter buf = new StringWriter();
        PrintWriter out = new PrintWriter(buf);
        if (col != null)
            col.getAsXml(out);
        out.flush();
        return buf.toString();
    }


    private static class ColumnException extends Exception {

        JComponent fieldToFocus;

        ColumnException(String resKey, JComponent fieldToFocus, String... args) {
            super(args.length == 0 ? resources.getString("Errors." + resKey)
                    : resources.format("Errors." + resKey, args[0]));
            this.fieldToFocus = fieldToFocus;
        }

    }


    private static class ColumnIDFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text,
                AttributeSet attr) throws BadLocationException {
            if (isAcceptable(text))
                super.insertString(fb, offset, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length,
                String text, AttributeSet attrs) throws BadLocationException {
            if (isAcceptable(text))
                super.replace(fb, offset, length, text, attrs);
        }

        private boolean isAcceptable(String text) {
            if (COLUMN_ID_PAT.matcher(text).matches())
                return true;

            Toolkit.getDefaultToolkit().beep();
            return false;
        }

    }


    private static final Pattern COLUMN_ID_PAT = Pattern.compile("[a-z0-9.]*",
        Pattern.CASE_INSENSITIVE);

    static final String CUST_ID_PREFIX = "custom.";

    private static final String[] TEXT_TYPES = { "Text", "Values" };

    private static final int TEXT_TYPE_FREETEXT = 0;

    private static final int TEXT_TYPE_VALUES = 1;

}
