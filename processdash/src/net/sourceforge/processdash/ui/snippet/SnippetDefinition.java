// Copyright (C) 2006-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.snippet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.TagData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.templates.ExtensionManager;
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

    private Set uris;

    private Set widgets;

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
        if (hide)
            category = "hidden";
        else {
            category = xml.getAttribute("category");
            if ("hidden".equals(category)) hide = true;
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

        uris = readSet(xml, "uri", true);

        widgets = readSet(xml, "widget", true);
        if (!widgets.isEmpty())
            this.xml = xml;

        if (uris.isEmpty() && widgets.isEmpty())
            throw new InvalidSnippetDefinitionException(
                    "At least one uri or widget must be specified");
    }


    private Set readSet(Element xml, String tagName, boolean modeSpecific) {
        Set result = new HashSet();
        NodeList nodes = xml.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            if (modeSpecific)
                result.add(new ModeSpecificValue(e));
            else
                result.add(XMLUtils.getTextContents(e));
        }
        return Collections.unmodifiableSet(result);
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

        for (Iterator iter = contexts.iterator(); iter.hasNext();) {
            String contextName = (String) iter.next();
            if (ctx.getValue(contextName) instanceof TagData)
                return true;
        }

        return false;
    }

    /** Returns a list of modes this snippet supports, in addition to "view" */
    public Set getModes() {
        return modes;
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
        String className = findModeSpecificValue(widgets, mode, action);
        if (className == null)
            return null;

        Object result = ExtensionManager.getExecutableExtension(xml, null,
            className, null);
        if (result instanceof SnippetWidget)
            return (SnippetWidget) result;
        else
            throw new InvalidSnippetDefinitionException("The class '"
                    + className + "' is not a SnippetWidget");
    }

    private String findModeSpecificValue(Set items, String mode, String action) {
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
