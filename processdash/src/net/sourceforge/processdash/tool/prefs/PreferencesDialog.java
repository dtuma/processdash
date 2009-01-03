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

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.management.modelmbean.XMLParseException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.processdash.ProcessDashboard;
import net.sourceforge.processdash.templates.ExtensionManager;

import org.w3c.dom.Element;

public class PreferencesDialog extends JDialog implements ListSelectionListener {
    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 600;

    /** The tag name for preferences panes in templates.xml files. */
    private static final String PREFERENCES_PANE_TAG_NAME = "preferences-pane";

    /** The PreferencesForm currently shown */
    private PreferencesForm preferencesForm = new PreferencesForm();

    public PreferencesDialog(ProcessDashboard parent, String title) {
        super(parent, title);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        List panesDefinitions = ExtensionManager
                .getXmlConfigurationElements(PREFERENCES_PANE_TAG_NAME);

        List<PreferencesPane> panes = getPreferencesPanes(panesDefinitions);
        Vector<PreferencesCategory> categories = getPreferencesGategories(panes);

        Container pane = this.getContentPane();
        pane.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                getCategoryChooser(categories),
                                preferencesForm));

        setVisible(true);
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

    public void valueChanged(ListSelectionEvent e) {
        JList list = (JList) e.getSource();
        PreferencesCategory selectedCategory = (PreferencesCategory) list.getSelectedValue();

        preferencesForm.setCategory(selectedCategory);
    }


}
