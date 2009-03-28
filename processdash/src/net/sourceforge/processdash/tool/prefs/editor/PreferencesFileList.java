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
import java.io.File;
import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class PreferencesFileList extends PreferencesList implements ListSelectionListener {

    /** Used to specify what string is used to separate entries when
     *   the values are written to the map. */
    private static final String SPERARATOR_TAG = "entrySeparator";
    private static final String DEFAULT_SEPARATOR = ";";
    private String separator;

    /** Used to specify how many items should be allowed in the list. */
    private static final String MAX_ITEMS_TAG = "maxItems";
    private Integer maxItems;

    /** Used to indicate if the file list can contain folders, files or both */
    private static final String ALLOW_FOLDERS_TAG = "allowFolders";
    private static final String ALLOW_FILES_TAG = "allowFiles";
    private boolean allowFolders;
    private boolean allowFiles;

    private JList fileList;
    private DefaultListModel listModel;

    public PreferencesFileList(BoundMap map, Element xml) {
        super(map, xml);
    }

    @Override
    protected void retrieveAttributes(Element xml) {
        String allowFoldersProp = xml.getAttribute(ALLOW_FOLDERS_TAG);
        String allowFilesProp = xml.getAttribute(ALLOW_FILES_TAG);
        String separator = xml.getAttribute(SPERARATOR_TAG);
        String maxItems = xml.getAttribute(MAX_ITEMS_TAG);

        if (StringUtils.hasValue(separator))
            this.separator = separator;
        else
            this.separator = DEFAULT_SEPARATOR;

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

    }

    private String getNewFilePath(String initialPath) {
        String path = null;

        JFileChooser fileChooser = new JFileChooser();
        int fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES;

        if (!allowFiles)
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY;
        if (!allowFolders)
            fileSelectionMode = JFileChooser.FILES_ONLY;
        fileChooser.setFileSelectionMode(fileSelectionMode);

        if (initialPath != null)
            fileChooser.setSelectedFile(new File(initialPath));

        int returnVal = fileChooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile != null)
                path = selectedFile.getAbsolutePath();
        }

        return path;
    }

    @Override
    public boolean doAddItem() {
        boolean itemAdded = false;

        String path = getNewFilePath(null);

        if (pathIsValid(path, "File_Already_Added_Message")) {
            listModel.addElement(path);
            itemAdded = true;
        }

        return itemAdded;
    }

    @Override
    public boolean doEditItem() {
        boolean itemEdited = false;

        int index = fileList.getSelectedIndex();
        if (index < 0)
            return false;

        String pathToEdit = listModel.get(index).toString();
        String path = getNewFilePath(pathToEdit);

        if (path != null && !path.equals(pathToEdit)
                && pathIsValid(path, "File_Already_Added_Message")) {
            listModel.set(index, path);
            itemEdited = true;
        }

        return itemEdited;
    }

    private boolean pathIsValid(String path, String errorMessageKey) {
        boolean pathIsValid = false;

        if (StringUtils.hasValue(path)) {

            if (!fileIsPresent(path)) {
                pathIsValid = true;
            }
            else {
                JOptionPane.showMessageDialog(this,
                    resources.getString(errorMessageKey),
                    resources.getString("Item_Already_Present_Title"),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
        return pathIsValid;
    }

    private boolean fileIsPresent(String path) {
        for (int i = 0; i < listModel.size(); ++i) {

            if (listModel.get(i).equals(path))
                return true;
        }

        return false;
    }

    @Override
    public boolean doRemoveItem() {
        listModel.remove(fileList.getSelectedIndex());
        return true;
    }

    @Override
    protected String getValue() {
        return StringUtils.join(Arrays.asList(listModel.toArray()), separator);
    }

    @Override
    protected void createList(String id, String currentValue) {
        listModel = new DefaultListModel();

        // The currentValue is a separator-delimited string of paths to add to the
        //  list.
        if (StringUtils.hasValue(currentValue)) {
            String[] paths = currentValue.split(separator);

            for (String path : paths) {
                listModel.addElement(path);
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

    @Override
    protected void updateButtons() {
        boolean itemSelected = fileList.getSelectedIndex() >= 0;
        removeButton.setEnabled(itemSelected);
        editButton.setEnabled(itemSelected && (allowFolders || allowFiles));

        boolean enableAddButton =
            (allowFolders || allowFiles) &&
                (this.maxItems == null || listModel.size() < this.maxItems.intValue());
        addButton.setEnabled(enableAddButton);
    }

}
