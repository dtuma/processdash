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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import net.sourceforge.processdash.i18n.Resources;

import teamdash.wbs.DataTableModel;

public class CustomColumnEditor {

    private DataTableModel dataModel;

    private JPanel contents;

    private GridBagLayout layout;

    private JTextField columnID;

    private JLabel columnIDLabel;

    private JTextField columnName;

    private JComboBox columnType;

    private JCheckBox autocomplete, multivalued, inherited, syncAsLabel;


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
        add(columnID = new JTextField(15), f);
        add(columnIDLabel = new JLabel(), f);
        ((AbstractDocument) columnID.getDocument())
                .setDocumentFilter(new ColumnIDFilter());

        l.gridy = ++f.gridy;
        add(new JLabel(resources.getString("Column_Name")), l);
        add(columnName = new JTextField(15), f);

        l.gridy = ++f.gridy;
        add(new JLabel(resources.getString("Column_Type")), l);
        add(columnType = new JComboBox(resources.getStrings("Column_Type_",
            TEXT_TYPES)), f);

        ++f.gridy;
        add(autocomplete = checkbox("Autocomplete", true), f);

        ++f.gridy;
        add(multivalued = checkbox("Multivalued", false), f);

        ++f.gridy;
        add(inherited = checkbox("Inherited", true), f);

        ++f.gridy;
        add(syncAsLabel = checkbox("Sync_As_Label", false), f);
    }

    private void add(Component comp, GridBagConstraints constraints) {
        contents.add(comp);
        layout.setConstraints(comp, constraints);
    }

    private JCheckBox checkbox(String resKey, boolean selected) {
        return new JCheckBox(resources.getString(resKey), selected);
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

        // load other details about the column
        columnType.setSelectedIndex(TEXT_TYPE_FREETEXT);
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

        while (true) {
            int userChoice = JOptionPane.showConfirmDialog(parent, contents,
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null);
            if (userChoice != JOptionPane.OK_OPTION)
                return null;

            try {
                return buildColumn(isAddOperation);
            } catch (ColumnException ce) {
                JOptionPane.showMessageDialog(parent, ce.getMessage(), title,
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private CustomColumn buildColumn(boolean checkUniqueID)
            throws ColumnException {
        // Ensure we have a nonempty column ID.
        String usrColumnID = columnID.getText().trim();
        if (usrColumnID.length() == 0)
            throw new ColumnException("ID_Missing");
        String newColumnID = CUST_ID_PREFIX + usrColumnID;

        // When adding a new column, ensure that the column ID is unique.
        if (checkUniqueID && dataModel.findColumn(newColumnID) != -1)
            throw new ColumnException("ID_Not_Unique_FMT", usrColumnID);

        // Ensure they entered a column name.
        String newColumnName = columnName.getText().trim();
        if (newColumnName.length() == 0)
            throw new ColumnException("Name_Missing");

        // Determine the label prefix to use
        String labelPrefix = (syncAsLabel.isSelected() ? newColumnName : null);

        // build an appropriate column
        return new CustomTextColumn(dataModel, newColumnID, newColumnName,
                autocomplete.isSelected(), multivalued.isSelected(),
                inherited.isSelected(), null, labelPrefix);
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

        ColumnException(String resKey, String... args) {
            super(args.length == 0 ? resources.getString("Errors." + resKey)
                    : resources.format("Errors." + resKey, args[0]));
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

    private static final String CUST_ID_PREFIX = "custom.";

    private static final String[] TEXT_TYPES = { "Text" };

    private static final int TEXT_TYPE_FREETEXT = 0;

}
