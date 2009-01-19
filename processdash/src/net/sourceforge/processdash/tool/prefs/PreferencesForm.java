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
import java.io.IOException;
import java.net.URLConnection;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.tool.prefs.editor.PreferencesCheckBox;
import net.sourceforge.processdash.ui.lib.binding.BoundForm;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * This form is used to modify the user preferences under a specific category.
 */
public class PreferencesForm extends BoundForm {

    /** Various xml tags used in spec files */
    private static final String ID_TAG = "id";
    private static final String REQUIRES_TAG = "requires";
    public static final String SETTING_TAG = "setting";

    /** The tags for which special Preferences editors are used */
    private static final String CHECKBOX_TAG = "checkbox";

    /** The JPanel containing the GUI */
    private JPanel panel = new JPanel();

    private static final Logger logger = Logger.getLogger(PreferencesForm.class.getName());

    public PreferencesForm(PreferencesCategory category) {
        addElementType(CHECKBOX_TAG, PreferencesCheckBox.class);
        selectCategory(category);
        panel.setLayout(new BorderLayout());
        panel.add(BorderLayout.CENTER, getContainer());
    }

    /**
     * Selects the category shown by the PreferencesForm
     */
    private void selectCategory(PreferencesCategory category) {
        if (category != null) {
            SortedSet<PreferencesPane> panes = category.getPanes();

            for (PreferencesPane pane : panes) {
                setResources(pane.getResources());
                loadSpecFileContents(pane.getSpecFile());
            }
        }
    }

    private void loadSpecFileContents(String specFileLocation) {
        try {
            Document spec = getSpecDocument(specFileLocation);
            addFormElements(spec.getDocumentElement());
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

    /**
     * An element should be ignored if the requirements in its "requires" attribute
     *  are not met.
     */
    @Override
    protected boolean shouldIgnoreElement(Element xml) {
        boolean elementIsValid = false;

        if (!super.shouldIgnoreElement(xml)) {
            String requirements = xml.getAttribute(REQUIRES_TAG);

            elementIsValid =
                TemplateLoader.meetsPackageRequirement(requirements);

            if (!elementIsValid) {
                logger.log(Level.INFO, "Could not load preferences widget \"" +
                                        xml.getAttribute(ID_TAG) + "\". " +
                                        "Requirements \"" + requirements + "\" " +
                                        "not met.");
            }
        }

        return !elementIsValid;
    }

    private Document getSpecDocument(String specFileLocation) {
        Document document = null;
        URLConnection conn = TemplateLoader.resolveURLConnection(specFileLocation);

        if (conn != null) {
            try {
                document = XMLUtils.parse(conn.getInputStream());
            }
            catch (SAXException e) { document = null; }
            catch (IOException e) { document = null; }
        }

        if (document == null) {
            throw new IllegalArgumentException("Could not open specFile \"" +
                                               specFileLocation + "\"");
        }

        return document;
    }

    public JPanel getPanel() {
        return panel;
    }

}
