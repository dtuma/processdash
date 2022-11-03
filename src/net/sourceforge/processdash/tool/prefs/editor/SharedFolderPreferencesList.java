// Copyright (C) 2022 Tuma Solutions, LLC
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
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

import org.w3c.dom.Element;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.tool.export.mgr.FolderMappingManager;
import net.sourceforge.processdash.ui.lib.FileSelectionField;
import net.sourceforge.processdash.ui.lib.JOptionPaneTweaker;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.StringUtils;

public class SharedFolderPreferencesList extends PreferencesList implements ListSelectionListener {

    private DefaultTableModel tableModel;
    private JTable folderTable;

    private static final String SETTING_PREFIX = InternalSettings.SHARED_FOLDER_PREFIX;

    public SharedFolderPreferencesList(BoundMap map, Element xml) {
        super(map, xml);
    }

    @Override
    public boolean doAddItem() {
        boolean itemAdded = false;

        Object[] row = getRow(null, null);

        if (rowIsValid(row, "Folder_Already_Set_Message")) {
            tableModel.addRow(row);
            map.put(SETTING_PREFIX + row[0], row[1]);
            itemAdded = true;
        }

        return itemAdded;
    }

    @Override
    public boolean doEditItem() {
        boolean itemEdited = false;

        int rowNum = folderTable.getSelectedRow();
        String selectedKey = (String) tableModel.getValueAt(rowNum, 0);
        String selectedFolder = (String) tableModel.getValueAt(rowNum, 1);

        Object[] row = getRow(selectedKey, selectedFolder);

        if (row != null) {
            tableModel.setValueAt(row[0], rowNum, 0);
            tableModel.setValueAt(row[1], rowNum, 1);
            map.put(SETTING_PREFIX + row[0], row[1]);
            itemEdited = true;
        }

        return itemEdited;
    }

    private boolean rowIsValid(Object[] row, String errorMessageKey) {
        boolean rowIsValid = false;

        if (row != null && row.length == 2 &&
                StringUtils.hasValue((String) row[0]) &&
                StringUtils.hasValue((String) row[1])) {

            if (!keyIsPresent(row[0])) {
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


    private boolean keyIsPresent(Object key) {
        for (int i = 0; i < tableModel.getRowCount(); ++i) {

            if (tableModel.getValueAt(i, 0).equals(key))
                return true;
        }

        return false;
    }

    @Override
    protected boolean doRemoveItem() {
        int index = folderTable.getSelectedRow();
        String selectedKey = (String) tableModel.getValueAt(index, 0);
        map.put(SETTING_PREFIX + selectedKey, "");

        tableModel.removeRow(index);

        return true;
    }

    public Object[] getRow(String key, String folder) {
        Object[] row = null;
        boolean isEdit = (key != null);
        String resPrefix = (isEdit ? "Edit_Folder_Prompt_" : "Add_Folder_Prompt_");

        JTextField keyField = new JTextField(key);
        FileSelectionField folderField = new FileSelectionField(null, null,
                JFileChooser.DIRECTORIES_ONLY, resources.getString("Browse"));
        folderField.getTextField().setText(folder);
        GridBagLayout layout = new GridBagLayout();
        JPanel panel = new JPanel(layout);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = gc.gridy = 0; gc.insets = new Insets(0, 0, 5, 2);
        gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(map.getResource(id + "." + resPrefix + "Message")), gc);
        gc.gridy = 1;
        panel.add(new JLabel(map.getResource(id + ".Set_Folder_Prompt_Message")), gc);
        gc.gridx = 1; gc.gridy = 0; gc.fill = GridBagConstraints.BOTH;
        panel.add(isEdit ? new JLabel(key) : keyField, gc);
        gc.gridy = 1;
        panel.add(folderField, gc);
        Object focusHandler = new JOptionPaneTweaker.GrabFocus(
                isEdit ? folderField : keyField);

        int userResponse = JOptionPane.showConfirmDialog(this,
            new Object[] { panel, focusHandler },
            map.getResource(id + "." + resPrefix + "Title"),
            JOptionPane.OK_CANCEL_OPTION);
        if (userResponse == JOptionPane.OK_OPTION) {
            key = keyField.getText().trim();
            folder = folderField.getSelectedPath();
            if (StringUtils.hasValue(key) && StringUtils.hasValue(folder)) {
                row = new Object[] { key, folder };
            }
        }

        return row;
    }


    @Override
    protected void createList(String id, String currentValue) {
        String keyColumnName = map.getResource(id + ".Key_Column");
        String folderColumnName = map.getResource(id + ".Folder_Column");
        Object[] columns = { keyColumnName, folderColumnName };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        Map<String, String> currentFolders = FolderMappingManager.getInstance()
                .reload().getFolders();
        for (Entry<String, String> e : currentFolders.entrySet()) {
            String key = e.getKey();
            String folder = e.getValue();
            if (StringUtils.hasValue(folder))
                tableModel.addRow(new Object[] { key, folder });
        }

        folderTable = new JTable(tableModel);
        folderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        folderTable.getSelectionModel().addListSelectionListener(this);

        JScrollPane listScroller = new JScrollPane(folderTable);
        listScroller.setPreferredSize(new Dimension(LIST_WIDTH, LIST_HEIGHT));

        this.add(listScroller, BorderLayout.CENTER);
    }

    @Override
    protected void retrieveAttributes(Element xml) { }

    @Override
    protected void updateButtons() {
        boolean itemSelected = folderTable.getSelectedRow() >= 0;
        editButton.setEnabled(itemSelected);
        removeButton.setEnabled(itemSelected);
    }

    @Override
    protected Object getValue() {
        return null;
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            updateButtons();
        }
    }

}
