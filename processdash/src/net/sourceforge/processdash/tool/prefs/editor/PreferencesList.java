// Copyright (C) 2009-2012 Tuma Solutions, LLC
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
import java.awt.event.ActionListener;
import java.beans.EventHandler;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.tool.prefs.PreferencesForm;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;

import org.w3c.dom.Element;

public abstract class PreferencesList extends JPanel {

    /** The spacing between the Add, Edit, and Remove buttons */
    private static final int CONTROL_BUTTONS_SPACING = 10;

    /** The border width of the button box */
    private static final int BUTTON_BOX_BORDER = 10;

    private static final int LABEL_BOTTOM_PAD = 5;

    /** The list dimensions */
    protected static final int LIST_WIDTH = 260;
    protected static final int LIST_HEIGHT = 80;

    /** Used to add, edit or remove items form the list */
    protected JButton addButton;
    protected JButton editButton;
    protected JButton removeButton;

    protected BoundMap map;
    protected String id;
    protected String settingName;

    protected static final Resources resources = Resources
                .getDashBundle("Tools.Prefs.List");

    public PreferencesList(BoundMap map, Element xml) {
        this.map = map;

        id = xml.getAttribute("id");

        settingName = xml.getAttribute(PreferencesForm.SETTING_TAG);
        String currentValue = Settings.getVal(settingName);
        map.put(settingName, currentValue);

        retrieveAttributes(xml);

        createUI(id, currentValue);
    }

    protected abstract void retrieveAttributes(Element xml);

    protected void createUI(String id, String currentValue) {
        this.setLayout(new BorderLayout());
        maybeCreateLabel(id);
        createList(id, currentValue);
        createButtonBox(id);
    }

    private void maybeCreateLabel(String id) {
        String text = map.getResource(id + ".List_Label");
        if (text != null) {
            JLabel label = new JLabel(text);
            label.setBorder(new EmptyBorder(0, 0, LABEL_BOTTOM_PAD, 0));
            this.add(label, BorderLayout.PAGE_START);
            this.add(new JLabel("    "), BorderLayout.LINE_START);
        }
    }

    protected abstract void createList(String id, String currentValue);

    private void createButtonBox(String id) {
        addButton = new JButton(getButtonLabel(id, "Add_Button_Label"));
        addButton.addActionListener((ActionListener) EventHandler.create(
            ActionListener.class, this, "addItem"));

        editButton = new JButton(getButtonLabel(id, "Edit_Button_Label"));
        editButton.addActionListener((ActionListener) EventHandler.create(
            ActionListener.class, this, "editItem"));

        removeButton = new JButton(getButtonLabel(id, "Remove_Button_Label"));
        removeButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "removeItem"));

        updateButtons();

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(addButton);
        buttonBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        buttonBox.add(editButton);
        buttonBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        buttonBox.add(removeButton);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.setBorder(new EmptyBorder(BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER));

        this.add(buttonBox, BorderLayout.PAGE_END);
    }

    protected abstract void updateButtons();

    private String getButtonLabel(String id, String key) {
        String result = map.getResource(id + "." + key);
        if (result == null)
            result = resources.getString(key);
        return result;
    }

    protected abstract boolean doAddItem();
    public void addItem() {
        if (doAddItem()) {
            map.put(settingName, getValue());
            updateButtons();
        }
    }

    protected abstract boolean doEditItem();
    public void editItem() {
        if (doEditItem()) {
            map.put(settingName, getValue());
            updateButtons();
        }
    }

    protected abstract boolean doRemoveItem();
    public void removeItem() {
        if (doRemoveItem()) {
            map.put(settingName, getValue());
            updateButtons();
        }
    }

    protected abstract Object getValue();


}
