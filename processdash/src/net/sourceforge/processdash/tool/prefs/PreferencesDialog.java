// Copyright (C) 2009-2013 Tuma Solutions, LLC
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
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.ui.DashboardIconFactory;
import net.sourceforge.processdash.ui.help.PCSH;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class PreferencesDialog extends JDialog implements ListSelectionListener,
                                                          PropertyChangeListener {

    public static final String RESTART_REQUIRED_KEY = " Restart Required ";

    static final Resources resources = Resources.getDashBundle("Tools.Prefs.Dialog");

    /** The spacing between the OK, Cancel and Apply buttons */
    private static final int CONTROL_BUTTONS_SPACING = 10;

    /** The border width of the button box */
    private static final int BUTTON_BOX_BORDER = 10;

    /** The tag name for preferences panes in templates.xml files. */
    private static final String PREFERENCES_PANE_TAG_NAME = "preferences-pane";

    /** The dashboard context */
    private DashboardContext dashboardContext;

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

    /** Contains all settings for which a modification needs the Dashboard to
     *   be restarted in order to be effective */
    private Set<String> restartRequiredSettings = new TreeSet<String>();

    /** Contains the restartRequiresSettings that were changed by the user */
    private Set<String> modifiedRestartRequiredSettings = new TreeSet<String>();

    /** Visual indication informing the user when restarting the Dashboard is necessary
     *   in order for all the modified settings to be effective */
    private JLabel restartRequireLabel;
    private boolean restartRequired;

    public PreferencesDialog(ProcessDashboard parent, String title) {
        super(parent, title);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.dashboardContext = parent;

        this.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                confirmClose();
            }
            });

        PCSH.enableHelpKey(this, "PreferencesTool");
        reload();
        setVisible(true);
        pack();
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

        pane.add(getBottomSection(), BorderLayout.PAGE_END);
    }

    public void showIt() {
        if (this.isShowing())
            this.toFront();
        else {
            reload();
            this.setVisible(true);
            this.pack();
        }
    }

    private Component getBottomSection() {
        JButton okButton = new JButton(resources.getString("OK_Button"));
        okButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "saveAndClose"));

        JButton cancelButton = new JButton(resources.getString("Cancel_Button"));
        cancelButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "closePreferences"));

        applyButton = new JButton(resources.getString("Apply_Button"));
        applyButton.addActionListener((ActionListener) EventHandler.create(
                ActionListener.class, this, "applyChanges"));

        restartRequireLabel = new JLabel(resources.getString("Restart_Required"));
        restartRequireLabel.setIcon(DashboardIconFactory.getRestartRequiredIcon());

        Box bottomBox = Box.createHorizontalBox();
        bottomBox.add(restartRequireLabel);
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.add(okButton);
        bottomBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        bottomBox.add(cancelButton);
        bottomBox.add(Box.createHorizontalStrut(CONTROL_BUTTONS_SPACING));
        bottomBox.add(applyButton);
        bottomBox.setBorder(new EmptyBorder(BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER,
                                            BUTTON_BOX_BORDER));

        updateApplyButton();
        updateRestartRequired();

        return bottomBox;
    }

    public void applyChanges() {
        Map.Entry<String, String> setting = null;

        for (Iterator<Map.Entry<String, String>> it = changedSettings.entrySet().iterator();
                it.hasNext();) {
            setting = it.next();

            InternalSettings.set(setting.getKey(), setting.getValue());
            it.remove();
        }

        if (modifiedRestartRequiredSettings.size() > 0) {
            restartRequired = true;
            modifiedRestartRequiredSettings.clear();
            JOptionPane.showMessageDialog(this,
                resources.getStrings("Restart_Required_Message"),
                resources.getString("Restart_Required"),
                JOptionPane.INFORMATION_MESSAGE);
        }

        updateApplyButton();
        updateRestartRequired();
    }

    private void updateApplyButton() {
        applyButton.setEnabled(!changedSettings.isEmpty());
    }

    private void updateRestartRequired() {
        restartRequireLabel.setVisible(modifiedRestartRequiredSettings.size() > 0 ||
                                        restartRequired);
    }

    public void closePreferences() {
        changedSettings.clear();
        modifiedRestartRequiredSettings.clear();
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
                form.put(DashboardContext.class, dashboardContext);
                form.addPropertyChangeListener(this);
                restartRequiredSettings.addAll(form.getRequireRestartSettings());
                preferencesPanels.add(form.getPanel(), selectedCategory.getCategoryID());
                builtForms.add(selectedCategory.getCategoryID());
            }

            CardLayout layout = (CardLayout) preferencesPanels.getLayout();
            layout.show(preferencesPanels, selectedCategory.getCategoryID());
            pack();
        }
    }

    /**
     * Called when the user modifies a preference.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String newValue = evt.getNewValue() != null ?
                             evt.getNewValue().toString().trim() : null;

        String propertyName = evt.getPropertyName();
        if (propertyName != null && propertyName.startsWith("-"))
            return;

        if (RESTART_REQUIRED_KEY.equals(propertyName)) {
            modifiedRestartRequiredSettings.add(RESTART_REQUIRED_KEY);
        }
        else if (newValue == null ||
             !newValue.equals(InternalSettings.getVal(propertyName))) {
            changedSettings.put(propertyName,
                                StringUtils.hasValue(newValue) ? newValue : null);

            if (restartRequiredSettings.contains(propertyName))
                modifiedRestartRequiredSettings.add(propertyName);
        }
        else {
            changedSettings.remove(propertyName);
            modifiedRestartRequiredSettings.remove(propertyName);
        }

        updateApplyButton();
        updateRestartRequired();
    }
}
