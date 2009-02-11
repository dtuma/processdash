// Copyright (C) 2009 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.prefs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class PreferencesDialog extends JDialog implements ListSelectionListener,
                                                          PropertyChangeListener {
    static final Resources resources = Resources.getDashBundle("Tools.Prefs.Dialog");

    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 600;

    /** The spacing between the OK, Cancel and Apply buttons */
    private static final int CONTROL_BUTTONS_SPACING = 10;

    /** The border width of the button box */
    private static final int BUTTON_BOX_BORDER = 10;

    /** The tag name for preferences panes in templates.xml files. */
    private static final String PREFERENCES_PANE_TAG_NAME = "preferences-pane";

    /** The button that is used to save changes*/
    private JButton applyButton;

    /** The JPanel containing the PreferencesForms shown to the user */
    JPanel preferencesPanels = new JPanel(new CardLayout());

    /** This Set contains the category IDs for which a PreferencesForm has been built.
     *   It is used to quickly determine if a PreferencesForm for a specific category
     *   has been built and is present in preferencesPanels.*/
    private Set<String> builtForms = new HashSet<String>();

    /** A mapping of all the settings that were changed by the user and their
     *   new value */
    private Map<String, String> changedSettings = new HashMap<String, String>();

    public PreferencesDialog(ProcessDashboard parent, String title) {
        super(parent, title);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmClose();
            }
            });

        reload();
        setVisible(true);
    }

    private void reload() {
        List panesDefinitions = ExtensionManager
                .getXmlConfigurationElements(PREFERENCES_PANE_TAG_NAME);

        List<PreferencesPane> panes = getPreferencesPanes(panesDefinitions);
        Vector<PreferencesCategory> categories = getPreferencesGategories(panes);

        Container pane = this.getContentPane();
        pane.removeAll();
        pane.setLayout(new BorderLayout());
        pane.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                getCategoryChooser(categories),
                                preferencesPanels),
                 BorderLayout.CENTER);
        pane.add(getButtonBox(), BorderLayout.PAGE_END);
    }

    public void showIt() {
        if (this.isShowing())
            this.toFront();
        else {
            reload();
            this.setVisible(true);
        }
    }

    private Component getButtonBox() {
        JButton okButton = new JButton(resources.getString("OK_Button"));
        okButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "saveAndClose"));

        JButton cancelButton = new JButton(resources.getString("Cancel_Button"));
        cancelButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "closePreferences"));

        applyButton = new JButton(resources.getString("Apply_Button"));
        applyButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "applyChanges"));
        updateApplyButton();

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(okButton);
        buttonBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        buttonBox.add(cancelButton);
        buttonBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        buttonBox.add(applyButton);
        buttonBox.setBorder(new EmptyBorder(BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER));

        return buttonBox;
    }

    public void applyChanges() {
        Map.Entry<String, String> setting = null;

        for (Iterator<Map.Entry<String, String>> it = changedSettings.entrySet().iterator();
                it.hasNext();) {
            setting = it.next();

            InternalSettings.set(setting.getKey(), setting.getValue());
            it.remove();
        }

        updateApplyButton();
    }

    private void updateApplyButton() {
        applyButton.setEnabled(!changedSettings.isEmpty());
    }

    public void closePreferences() {
        changedSettings.clear();
        preferencesPanels.removeAll();
        builtForms.clear();
        this.setVisible(false);
    }

    public void confirmClose() {
        boolean shouldClose = false;

        if (!changedSettings.isEmpty()) {
            int choice =
                JOptionPane.showConfirmDialog(
                    this,
                    resources.getString("Save_On_Close_Dialog"),
                    resources.getString("Save_Dialog_Title"),
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (choice == JOptionPane.YES_OPTION)
                applyChanges();

            shouldClose = choice != JOptionPane.CANCEL_OPTION;
        }
        else {
            shouldClose = true;
        }

        if (shouldClose)
            closePreferences();
    }

    public void saveAndClose() {
        applyChanges();
        closePreferences();
    }

    private Vector<PreferencesCategory> getPreferencesGategories(
            List<PreferencesPane> panes) {
        Vector<PreferencesCategory> categories = new Vector<PreferencesCategory>();

        // First, we sort the preferences panes by category
        Collections.sort(panes, new PreferencesPaneCategoryComparator());

        // We then iterate through all preferences panes and put the ones
        //  that define a single category in a PreferencesCategory object.
        String previousCategoryID = null;
        PreferencesCategory category = null;

        for (PreferencesPane pane : panes) {

            if (pane.getCategoryID().equals(previousCategoryID)) {
                category.addPane(pane);
            }
            else {
                if (category != null) {
                    categories.add(category);
                }

                category = new PreferencesCategory(pane.getCategoryID());
                category.addPane(pane);
                previousCategoryID = pane.getCategoryID();
            }
        }

        if (category != null) {
            categories.add(category);
        }

        return categories;
    }

    private List<PreferencesPane> getPreferencesPanes(List panesDefinitions) {
        // We want to make sure not to have 2 PreferencesPanes with the same specFile.
        //  That's why we use the specFile as a key for this map.
        Map<String, PreferencesPane> panes = new HashMap<String, PreferencesPane>();

        PreferencesPane pane;

        for (Iterator i = panesDefinitions.iterator(); i.hasNext();) {
            Element xmlDefinition = (Element) i.next();

            try {
                pane = new PreferencesPane(xmlDefinition);
                panes.put(pane.getSpecFile(), pane);
            } catch (IllegalArgumentException e) {}
        }

        return new ArrayList<PreferencesPane>(panes.values());
    }

    private Component getCategoryChooser(Vector<PreferencesCategory> categories) {
        Collections.sort(categories);

        JList categoryList = new JList(categories);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.addListSelectionListener(this);
        categoryList.setSelectedIndex(0);

        return categoryList;
    }

    /**
     * Called when the category is changed
     */
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            JList list = (JList) e.getSource();
            PreferencesCategory selectedCategory =
                (PreferencesCategory) list.getSelectedValue();

            if (!builtForms.contains(selectedCategory.getCategoryID())) {
                // The selected form has not been built yet so we create it.
                PreferencesForm form = new PreferencesForm(selectedCategory);
                form.addPropertyChangeListener(this);
                preferencesPanels.add(form.getPanel(), selectedCategory.getCategoryID());
                builtForms.add(selectedCategory.getCategoryID());
            }

            CardLayout layout = (CardLayout) preferencesPanels.getLayout();
            layout.show(preferencesPanels, selectedCategory.getCategoryID());
        }
    }

    /**
     * Called when the user modifies a preference.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String newValue = evt.getNewValue() != null ?
                             evt.getNewValue().toString().trim() : null;

        changedSettings.put(evt.getPropertyName(),
                            StringUtils.hasValue(newValue) ? newValue : null);
        updateApplyButton();
    }

}
