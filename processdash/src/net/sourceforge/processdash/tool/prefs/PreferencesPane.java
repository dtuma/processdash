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

package net.sourceforge.processdash.tool.prefs;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

public class PreferencesPane implements Comparable<PreferencesPane> {
    /** We can find the category display name defined by this PreferencesPane under
     *   <named resources bundle>.Category.Label */
    private static final String CATEGORY_DISPLAY_NAME_RESOURCE = "Category.Label";

    /** The xml tags used in the template.xml file defining preferences-panes */
    private static final String ID_TAG = "id";
    private static final String CATEGORY_ID_TAG = "categoryID";
    private static final String SPEC_FILE_TAG = "specFile";
    private static final String PRIORITY_TAG = "priority";
    private static final String RESOURCES_TAG = "resources";

    /** The id of first party panes starts with pdash. */
    private static final String FIRST_PARTY_PREFIX = "pdash.";

    /** The priority used when the preferences-pane definition does
     *   not specify one */
    private static final int DEFAULT_PRIORITY = 250;

    /** The maximum allowed priority for a third party preferences pane */
    private static final int MAX_THIRD_PARTY_PRIORITY = 500;

    /** The various infos defining this pane */
    private String id;
    private String categoryID;
    private String specFile;
    private String resourcesLocation;
    private int priority;

    /** The actual XML element defining the pane */
    private Element xmlDefinition;

    private static final Logger logger = Logger.getLogger(PreferencesPane.class.getName());

    public PreferencesPane(Element xmlDefinition) throws IllegalArgumentException {
        this.xmlDefinition = xmlDefinition;

        this.id = getAttribute(xmlDefinition, ID_TAG);
        this.categoryID = getAttribute(xmlDefinition, CATEGORY_ID_TAG);
        this.specFile = getAttribute(xmlDefinition, SPEC_FILE_TAG);
        this.resourcesLocation = getAttribute(xmlDefinition, RESOURCES_TAG);

        try {
            this.priority = Integer.parseInt(xmlDefinition.getAttribute(PRIORITY_TAG));
        } catch (NumberFormatException e) {
            this.priority = DEFAULT_PRIORITY;
        }

        // We want to make sure that the priority of third party panes is not
        //  too high.
        if (!this.id.startsWith(FIRST_PARTY_PREFIX)) {
            this.priority = Math.min(this.priority, MAX_THIRD_PARTY_PRIORITY);
        }
    }

    /**
     * Returns the specified attribute in the org.w3c.dom.Element argument. If no
     *  such attribute exists, an error is logged and a XMLParseException is thrown.
     */
    private String getAttribute(Element xmlDefinition, String tag)
            throws IllegalArgumentException {
        String attribute = xmlDefinition.getAttribute(tag);

        if (!XMLUtils.hasValue(attribute)) {
            String message = "Missing attribute \"" +
                             tag +
                             "\" in preferences-pane declaration.";

            logger.log(Level.SEVERE, message);
            throw new IllegalArgumentException(message);
        }

        return attribute;
    }

    /**
     * Returns the display name for the category defined by this preferences pane
     */
    public String getCategoryDisplayName() {
        return getResources().getString(CATEGORY_DISPLAY_NAME_RESOURCE);
    }

    public Resources getResources() {
        return Resources.getDashBundle(this.resourcesLocation);
    }

    /**
     * We want the natural ordering of the panes to be the opposite of their priority.
     *  For instance, a pane with a priority value of 10 is smaller than one with
     *  the priority of 5.
     * Since we don't want 2 panes to have the same order, if both priorities are
     *  the same, we compare the spec file.
     */
    public int compareTo(PreferencesPane o) {
        int priorityOrder = o.priority - this.priority;
        return (priorityOrder == 0) ? o.specFile.compareTo(this.specFile) : priorityOrder;
    }

    public String getCategoryID() {
        return categoryID;
    }

    public int getPriority() {
        return priority;
    }

    public String getSpecFile() {
        return specFile;
    }

}
