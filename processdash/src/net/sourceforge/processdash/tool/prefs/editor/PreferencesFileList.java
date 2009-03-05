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
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.prefs.PreferencesForm;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class PreferencesFileList extends JPanel implements ListSelectionListener {

    /** The spacing between the Add, Edit, and Remove buttons */
    private static final int CONTROL_BUTTONS_SPACING = 10;

    /** The border width of the button box */
    private static final int BUTTON_BOX_BORDER = 10;

    private static final int LABEL_BOTTOM_PAD = 5;

    /** The file list dimensions */
    private static final int LIST_WIDTH = 260;
    private static final int LIST_HEIGHT = 80;

    /** Used to specify what string is used to separate entries when
     *   the values are written to the map. */
    private static final String SPERARATOR_TAG = "entrySeparator";
    private static final String DEFAULT_SEPARATOR = ";";
    private String separator = DEFAULT_SEPARATOR;

    /** Used to specify how many items should be allowed in the list. */
    private static final String MAX_ITEMS_TAG = "maxItems";
    private Integer maxItems = null;

    /** Used to indicate if the file list can contain folders, files or both */
    private static final String ALLOW_FOLDERS_TAG = "allowFolders";
    private static final String ALLOW_FILES_TAG = "allowFiles";
    private boolean allowFolders;
    private boolean allowFiles;

    /** Used to add, edit or remove a file/folder form the list */
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private BoundMap map;
    private String settingName;
    private JList fileList;
    private DefaultListModel listModel;

    private static final Resources resources = Resources
            .getDashBundle("Tools.Prefs.File_List");

    public PreferencesFileList(BoundMap map, Element xml) {
        this.map = map;

        String id = xml.getAttribute("id");
        String allowFoldersProp = xml.getAttribute(ALLOW_FOLDERS_TAG);
        String allowFilesProp = xml.getAttribute(ALLOW_FILES_TAG);
        String separator = xml.getAttribute(SPERARATOR_TAG);
        String maxItems = xml.getAttribute(MAX_ITEMS_TAG);

        settingName = xml.getAttribute(PreferencesForm.SETTING_TAG);
        String currentValue = Settings.getVal(settingName);
        map.put(settingName, currentValue);

        if (StringUtils.hasValue(separator))
            this.separator = separator;

        try {
            this.maxItems = new Integer(maxItems);
        } catch (NumberFormatException e) {
            this.maxItems = null;
        }

        // If the properties are not specified, we allow files and folders.
        allowFolders = !StringUtils.hasValue(allowFoldersProp)
                                ? true : Boolean.parseBoolean(allowFoldersProp);
        allowFiles = !StringUtils.hasValue(allowFilesProp)
                                ? true : Boolean.parseBoolean(allowFilesProp);

        createUI(id, currentValue);
    }

    private void createUI(String id, String currentValue) {
        this.setLayout(new BorderLayout());
        maybeCreateLabel(id);
        createList(currentValue);
        createButtonBox(id);
    }

    private void maybeCreateLabel(String id) {
        String text = map.getResource(id + ".FileList_Label");
        if (text != null) {
            JLabel label = new JLabel(text);
            label.setBorder(new EmptyBorder(0, 0, LABEL_BOTTOM_PAD, 0));
            this.add(label, BorderLayout.PAGE_START);
            this.add(new JLabel("    "), BorderLayout.LINE_START);
        }
    }

    private void createButtonBox(String id) {
        addButton = new JButton(getButtonLabel(id, "Add_Button_Label"));

        editButton = new JButton(getButtonLabel(id, "Edit_Button_Label"));

        removeButton = new JButton(getButtonLabel(id, "Remove_Button_Label"));
        removeButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "removeFile"));

        if (allowFiles || allowFolders) {
            addButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "addFile"));

            editButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "editFile"));
        }
        else
            addButton.setEnabled(false);

        updateButtons();

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(addButton);
        buttonBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        buttonBox.add(editButton);
        buttonBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        buttonBox.add(removeButton);
        buttonBox.setBorder(new EmptyBorder(BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER));

        this.add(buttonBox, BorderLayout.PAGE_END);

    }

    private String getButtonLabel(String id, String key) {
        String result = map.getResource(id + "." + key);
        if (result == null)
            result = resources.getString(key);
        return result;
    }

    private String getNewFilePath(String initialPath) {
        String path = null;

        JFileChooser fileChooser = new JFileChooser();
        int fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES;

        if (!allowFiles)
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY;
        if (!allowFolders)
            fileSelectionMode = JFileChooser.FILES_ONLY;
        if (initialPath != null)
            fileChooser.setSelectedFile(new File(initialPath));

        fileChooser.setFileSelectionMode(fileSelectionMode);

        int returnVal = fileChooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile != null)
                path = selectedFile.getAbsolutePath();
        }

        return path;
    }

    public void addFile() {
        String path = getNewFilePath(null);

        if (StringUtils.hasValue(path))
            listModel.addElement(path);

        updateButtons();
        map.put(settingName, getValue());
    }

    public void editFile() {
        int index = fileList.getSelectedIndex();
        if (index < 0)
            return;

        String pathToEdit = listModel.get(index).toString();
        String path = getNewFilePath(pathToEdit);

        if (StringUtils.hasValue(path)) {
            listModel.set(index, path);
        }

        updateButtons();
        map.put(settingName, getValue());
    }

    public void removeFile() {
        listModel.remove(fileList.getSelectedIndex());
        updateButtons();
        map.put(settingName, getValue());
    }

    private String getValue() {
        return StringUtils.join(Arrays.asList(listModel.toArray()), separator);
    }

    private void createList(String currentValue) {
        listModel = new DefaultListModel();

        // The currentValue is a separator-delimited string of paths to add to the
        //  list.
        if (StringUtils.hasValue(currentValue)) {
            String[] paths = currentValue.split(separator);
            File file = null;

            for (String path : paths) {
                file = new File(path);
                listModel.addElement(file.getPath());
            }
        }

        fileList = new JList(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addListSelectionListener(this);
        JScrollPane listScroller = new JScrollPane(fileList);
        listScroller.setPreferredSize(new Dimension(LIST_WIDTH, LIST_HEIGHT));

        this.add(listScroller, BorderLayout.CENTER);
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            updateButtons();
        }
    }

    private void updateButtons() {
        boolean itemSelected = fileList.getSelectedIndex() >= 0;
        removeButton.setEnabled(itemSelected);
        editButton.setEnabled(itemSelected && (allowFolders || allowFiles));

        boolean enableAddButton =
            (allowFolders || allowFiles) &&
                (this.maxItems == null || listModel.size() < this.maxItems.intValue());
        addButton.setEnabled(enableAddButton);
    }

}
