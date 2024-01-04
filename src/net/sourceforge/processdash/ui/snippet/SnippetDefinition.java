// Copyright (C) 2006-2024 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.ui.snippet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Manages the definition of a snippet which is available in the system.
 * 
 * Authors of dashboard add-ons define snippets by including a &lt;snippet&gt;
 * tag in their template.xml file.  An instance of this object is used to
 * hold the data provided by such a declaration.
 */
public class SnippetDefinition {

    public static final String SNIPPET_TAG = "snippet";

    private Element xml;

    private String id;

    private String version;

    private String category;

    private boolean hide;

    private boolean autoParsePersistedText;

    private Set aliases;

    private String resourceBundleName;

    private Set contexts;

    private Set modes;

    private Set<String> permissions;

    private Set uris;

    private Set widgets;

    private Set filters;

    /** Read a snippet definition from an XML declaration
     * 
     * @throws InvalidSnippetDefinitionException if the XML fragment does not
     * contain a valid snippet definition.
     */
    public SnippetDefinition(Element xml)
            throws InvalidSnippetDefinitionException {

        if (!SNIPPET_TAG.equals(xml.getTagName()))
            throw new InvalidSnippetDefinitionException(
                "<snippet> tag expected for declaration");

        id = xml.getAttribute("id");
        if (!XMLUtils.hasValue(id))
            throw new InvalidSnippetDefinitionException(
                    "The id attribute must be specified");

        version = xml.getAttribute("version");
        if (!XMLUtils.hasValue(version))
            version = "1.0";

        hide = "true".equals(xml.getAttribute("hide"));
        category = xml.getAttribute("category");
        if ("hidden".equals(category)) {
            hide = true;
        } else if (hide && !XMLUtils.hasValue(category)) {
            category = "hidden";
        }

        autoParsePersistedText = !"false".equals(xml.getAttribute("parseText"));

        aliases = readSet(xml, "alias", false);

        resourceBundleName = getElemContent(xml, "resources");
        if (!XMLUtils.hasValue(resourceBundleName))
            throw new InvalidSnippetDefinitionException(
                    "The resource bundle must be specified");

        contexts = readSet(xml, "context", false);
        if (contexts.isEmpty())
            throw new InvalidSnippetDefinitionException(
                    "The contexts must be specified");

        modes = readSet(xml, "mode", false);

        permissions = readSet(xml, "permission", false);

        uris = readSet(xml, "uri", true);

        widgets = readSet(xml, "widget", true);
        filters = readSet(xml, "pageFilter", true);
        if (!widgets.isEmpty() || !filters.isEmpty())
            this.xml = xml;

        if (uris.isEmpty() && widgets.isEmpty() && filters.isEmpty())
            throw new InvalidSnippetDefinitionException(
                    "At least one uri, widget, or pageFilter must be specified");
    }


    private Set readSet(Element xml, String tagName, boolean modeSpecific) {
        Set result = new HashSet();
        NodeList nodes = xml.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            if (isTagExcludedByConfig(e))
                continue;
            else if (modeSpecific)
                result.add(new ModeSpecificValue(e));
            else
                result.add(XMLUtils.getTextContents(e));
        }
        return Collections.unmodifiableSet(result);
    }

    private boolean isTagExcludedByConfig(Element xml) {
        // check required installed package
        String requires = xml.getAttribute("requires");
        if (XMLUtils.hasValue(requires)
                && !TemplateLoader.meetsPackageRequirement(requires))
            return true;

        // check forbidden installed package
        String unless = xml.getAttribute("unless");
        if (XMLUtils.hasValue(unless)
                && TemplateLoader.meetsPackageRequirement(unless))
            return true;

        // check required user setting
        String enabledBy = xml.getAttribute("enabledBy");
        if (XMLUtils.hasValue(enabledBy)
                && !Settings.getBool(enabledBy, false))
            return true;

        // check forbidden user setting
        String disabledBy = xml.getAttribute("disabledBy");
        if (XMLUtils.hasValue(disabledBy)
                && Settings.getBool(disabledBy, false))
            return true;

        return false;
    }

    private String getElemContent(Element xml, String tagName) {
        Element e = (Element) xml.getElementsByTagName(tagName).item(0);
        if (e == null)
            return null;
        else
            return XMLUtils.getTextContents(e);
    }


    /** Returns the unique id of this snippet */
    public String getId() {
        return id;
    }

    /** Returns the version number of this snippet */
    public String getVersion() {
        return version;
    }

    /** Return the category to which this snippet belongs */
    public String getCategory() {
        return category;
    }

    /** Return true if this snippet matches the given category */
    public boolean matchesCategory(String c) {
        return (c != null && c.equalsIgnoreCase(category));
    }

    /** Returns a list of aliases by which this snippet is known */
    public Set getAliases() {
        return aliases;
    }

    /** Returns true if this snippet should be hidden from the user when
     * displaying a list of available snippets */
    public boolean shouldHide() {
        return hide;
    }

    /** Returns true if this snippet saves its text data as a set of name/value
     * paris, which should be parsed and handled automatically. */
    public boolean shouldParsePersistedText() {
        return autoParsePersistedText;
    }


    /** Returns true if this snippet is appropriate for the given context */
    public boolean matchesContext(DataContext ctx) {
        if (contexts.contains("*"))
            return true;

        boolean foundMatch = false;

        for (Iterator iter = contexts.iterator(); iter.hasNext();) {
            String contextName = (String) iter.next();
            boolean isProhibition = false;
            if (contextName.startsWith("!")) {
                contextName = contextName.substring(1);
                isProhibition = true;
            }
            if (ctx.getValue(contextName) instanceof TagData) {
                if (isProhibition)
                    return false;
                else
                    foundMatch = true;
            }
        }

        return foundMatch;
    }

    /** Returns a list of modes this snippet supports, in addition to "view" */
    public Set getModes() {
        return modes;
    }

    /**
     * Returns the ID of a permission that is required to view this snippet, or
     * null if no permission is necessary.
     */
    public String getPermission() {
        return permissions.isEmpty() ? null : permissions.iterator().next();
    }

    /** Returns the resource bundle that is used by the snippet */
    public Resources getResources() {
        return Resources.getDashBundle(resourceBundleName);
    }

    /** Return a user-friendly name for this snippet */
    public String getName() {
        return getResources().getString("Snippet_Name");
    }

    /** Return a user-friendly name for this snippet, in HTML format */
    public String getNameHtml() {
        return getResources().getHTML("Snippet_Name");
    }

    /** Return a user-friendly description for this snippet */
    public String getDescription() {
        return getResources().getString("Snippet_Description");
    }

    /** Return a user-friendly description for this snippet, in HTML format */
    public String getDescriptionHtml() {
        return getResources().getHTML("Snippet_Description");
    }

    /** Returns an internal dashboard URI which can respond to requests
     * for this snippet
     * 
     * @param mode the current mode in effect
     * @param action the current action being executed, or null for no action
     */
    public String getUri(String mode, String action) {
        String result = findModeSpecificValue(uris, mode, action);
        if (result == null || result.startsWith("/"))
            return result;
        else
            return "/" + result;
    }

    /**
     * Get a widget that can be used to produce components for display in
     * the dashboard.
     * 
     * @param mode the current mode in effect
     * @param action the current action being executed, or null for no action
     * @return a {@link SnippetWidget} to produce components, or null if no
     *     matching widget declaration was found
     * @throws InvalidSnippetDefinitionException if the widget declaration names
     *     a class that does not implement <code>SnippetWidget</code>
     * @throws Exception if an exception occurs when attempting to instantiate
     *     the specified <code>SnippetWidget</code>
     */
    public SnippetWidget getWidget(String mode, String action)
            throws InvalidSnippetDefinitionException, Exception {
        return (SnippetWidget) getModeSpecificJavaObject(SnippetWidget.class,
            widgets, mode, action);
    }

    /**
     * Get a filter that can be used to alter the content of a CMS page.
     * 
     * @param mode the current mode in effect
     * @param action the current action being executed, or null for no action
     * @return a {@link SnippetPageFilter} to alter a CMS page, or null if no
     *     matching filter declaration was found
     * @throws InvalidSnippetDefinitionException if the filter declaration names
     *     a class that does not implement <code>SnippetPageFilter</code>
     * @throws Exception if an exception occurs when attempting to instantiate
     *     the specified <code>SnippetPageFilter</code>
     */
    public SnippetPageFilter getFilter(String mode, String action)
            throws InvalidSnippetDefinitionException, Exception {
        return (SnippetPageFilter) getModeSpecificJavaObject(
            SnippetPageFilter.class, filters, mode, action);
    }

    private Object getModeSpecificJavaObject(Class expectedType, Set sourceSet,
            String mode, String action)
            throws InvalidSnippetDefinitionException, Exception {
        String className = findModeSpecificValue(sourceSet, mode, action);
        if (className == null)
            return null;

        Object result = ExtensionManager.getExecutableExtension(xml, null,
            className, null);
        if (expectedType.isInstance(result))
            return result;
        else
            throw new InvalidSnippetDefinitionException("The class '"
                    + className + "' is not a " + expectedType.getSimpleName());
    }

    private String findModeSpecificValue(Set items, String mode, String action) {
        if (items == null || items.isEmpty())
            return null;

        // if both flags are empty, this implicitly means that we are in
        // "view" mode.
        if (action == null && mode == null)
            mode = "view";

        String result = lookupModeSpecificValue(items, mode, action);
        if (result == null)
            result = lookupModeSpecificValue(items, mode, null);
        if (result == null)
            result = lookupModeSpecificValue(items, null, null);
        return result;
    }

    private String lookupModeSpecificValue(Set items, String mode, String action) {
        for (Iterator i = items.iterator(); i.hasNext();) {
            ModeSpecificValue v = (ModeSpecificValue) i.next();
            if (v.isMatch(mode, action))
                return v.getValue();
        }
        return null;
    }

    private static class ModeSpecificValue {
        private String mode;
        private String action;
        private String value;
        public ModeSpecificValue(Element e) {
            this.mode = e.getAttribute("mode");
            this.action = e.getAttribute("action");
            this.value = XMLUtils.getTextContents(e);
        }
        public boolean isMatch(String mode, String action) {
            return equals(mode, this.mode) && equals(action, this.action);
        }
        private boolean equals(String a, String b) {
            if (a == null || a.length() == 0)
                return (b == null || b.length() == 0);
            return a.equals(b);
        }
        public String getValue() {
            return value;
        }
    }

}
