// Copyright (C) 2009 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.prefs.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.tool.prefs.PreferencesDialog;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class ManuallyEnteredPreferencesList extends PreferencesList implements ListSelectionListener {

    /** Used to specify what string is used to separate entries when the settings
     *   managed by this widget are written to the map. */
    private static String SETTING_SEPARATOR = ";";

    private DefaultTableModel tableModel;
    private JTable settingTable;

    public ManuallyEnteredPreferencesList(BoundMap map, Element xml) {
        super(map, xml);
    }

    @Override
    public boolean doAddItem() {
        boolean itemAdded = false;

        Object[] row = getRow(null, null);

        if (rowIsValid(row, "Setting_Already_Set_Message")) {
            tableModel.addRow(row);
            map.put(row[0], row[1]);
            itemAdded = true;
        }

        return itemAdded;
    }

    @Override
    public boolean doEditItem() {
        boolean itemEdited = false;

        String selectedSetting =
            (String) tableModel.getValueAt(settingTable.getSelectedRow(), 0);
        String selectedValue =
            (String) tableModel.getValueAt(settingTable.getSelectedRow(), 1);

        Object[] row = getRow(selectedSetting, selectedValue);

        if (row != null) {
            tableModel.setValueAt(row[0], settingTable.getSelectedRow(), 0);
            tableModel.setValueAt(row[1], settingTable.getSelectedRow(), 1);
            map.put(row[0], row[1]);
            map.put(PreferencesDialog.RESTART_REQUIRED_KEY, "true");
            itemEdited = true;
        }

        return itemEdited;
    }

    private boolean rowIsValid(Object[] row, String errorMessageKey) {
        boolean rowIsValid = false;

        if (row != null && row.length == 2 &&
                StringUtils.hasValue((String) row[0]) &&
                StringUtils.hasValue((String) row[1])) {

            if (!settingIsPresent(row[0])) {
                rowIsValid = true;
            }
            else {
                JOptionPane.showMessageDialog(this,
                                              map.getResource(id + "." + errorMessageKey),
                                              resources.getString("Item_Already_Present_Title"),
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
        return rowIsValid;
    }


    private boolean settingIsPresent(Object setting) {
        for (int i = 0; i < tableModel.getRowCount(); ++i) {

            if (tableModel.getValueAt(i, 0).equals(setting))
                return true;
        }

        return false;
    }

    @Override
    protected boolean doRemoveItem() {
        int index = settingTable.getSelectedRow();
        String selectedSetting = (String) tableModel.getValueAt(index, 0);
        map.put(selectedSetting, null);

        tableModel.removeRow(index);

        return true;
    }

    public Object[] getRow(String settingName, String defaultValue) {
        Object[] row = null;
        boolean isEdit = (settingName != null);
        String resPrefix = (isEdit ? "Edit_Key_Prompt_" : "Add_Key_Prompt_");

        JTextField nameField = new JTextField(settingName);
        JTextField valueField = new JTextField(defaultValue);
        JPanel panel = new JPanel(new GridLayout(2,2,5,0));
        panel.add(new JLabel(map.getResource(id + "." + resPrefix + "Message")));
        panel.add(isEdit ? new JLabel(settingName) : nameField);
        panel.add(new JLabel(map.getResource(id + ".Set_Value_Prompt_Message")));
        panel.add(valueField);
        Object focusHandler = new JOptionPaneTweaker.GrabFocus(
                isEdit ? valueField : nameField);

        int userResponse = JOptionPane.showConfirmDialog(this,
            new Object[] { panel, focusHandler },
            map.getResource(id + "." + resPrefix + "Title"),
            JOptionPane.OK_CANCEL_OPTION);
        if (userResponse == JOptionPane.OK_OPTION) {
            String setting = nameField.getText();
            String value = valueField.getText();
            if (StringUtils.hasValue(setting) &&
                    StringUtils.hasValue(value)) {
                row = new Object[] { setting.trim(), value.trim() };
            }
        }

        return row;
    }


    @Override
    protected void createList(String id, String currentValue) {
        Object[][] data = getCurrentData(currentValue);

        String settingColumnName = map.getResource(id + ".Setting_Column");
        String valueColumnName = map.getResource(id + ".Value_Column");
        Object[] columns = {settingColumnName, valueColumnName};

        tableModel = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        settingTable = new JTable(tableModel);
        settingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        settingTable.getSelectionModel().addListSelectionListener(this);

        JScrollPane listScroller = new JScrollPane(settingTable);
        listScroller.setPreferredSize(new Dimension(LIST_WIDTH, LIST_HEIGHT));

        this.add(listScroller, BorderLayout.CENTER);
    }

    private Object[][] getCurrentData(String currentValue) {
        Object[][] data = null;

        if (StringUtils.hasValue(currentValue)) {
            String[] settings = currentValue.split(SETTING_SEPARATOR);
            data = new Object[settings.length][2];

            for (int i = 0; i < settings.length; ++i) {
                data[i][0] = settings[i];
                data[i][1] = InternalSettings.getVal(settings[i]);
            }
        }

        return data;
    }

    @Override
    protected void retrieveAttributes(Element xml) { }

    @Override
    protected void updateButtons() {
        boolean itemSelected = settingTable.getSelectedRow() >= 0;
        editButton.setEnabled(itemSelected);
        removeButton.setEnabled(itemSelected);
    }

    @Override
    protected Object getValue() {
        int rowCount = settingTable.getRowCount();
        StringBuffer value = rowCount > 0 ? new StringBuffer() : null;

        for (int i = 0; i < rowCount; ++i) {
            value.append((String) tableModel.getValueAt(i, 0) + SETTING_SEPARATOR);
        }

        if (value != null && value.length() > 1)
            return value.deleteCharAt(value.length()-1).toString();
        else
            return null;
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            updateButtons();
        }
    }

}
