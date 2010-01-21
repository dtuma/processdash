// Copyright (C) 2006-2010 Tuma Solutions, LLC
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.ExtensionManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

/** Keeps track of all the snippets defined by the dashboard and add-ons
 * in the current configuration.
 */
public class SnippetDefinitionManager {

    private static final Logger logger = Logger
            .getLogger(SnippetDefinitionManager.class.getName());

    private static final Map<String, SnippetDefinition> SNIPPETS = new TreeMap();

    /** Initialize this object if it needs it.  If the object has already
     * been initialized, do nothing.
     */
    public static void initialize() {
        initialize(false);
    }

    /** Initialize this object, reading snippet definitions from dashboard
     * add-ons.
     * 
     * @param force if true, unequivocally scan for snippets.  if false, only
     *    scan if initialization has not already been performed.
     */
    public static void initialize(boolean force) {
        if (!SNIPPETS.isEmpty() && force == false)
            return;

        List config = ExtensionManager
                .getXmlConfigurationElements(SnippetDefinition.SNIPPET_TAG);
        for (Iterator i = config.iterator(); i.hasNext();) {
            Element e = (Element) i.next();

            String requiresVal = e.getAttribute("requires");
            if (!TemplateLoader.meetsPackageRequirement(requiresVal))
                continue;

            try {
                SnippetDefinition d = new SnippetDefinition(e);
                addSnippetDefinition(d);
            } catch (InvalidSnippetDefinitionException ex) {
                logger.log(Level.WARNING,
                        "Could not load snippet definition; xml was:\n"
                                + XMLUtils.getAsText(e), ex);
            }
        }
    }

    private static void addSnippetDefinition(SnippetDefinition d) {
        addSnippetDefinition(d.getId(), d);
        for (Iterator i = d.getAliases().iterator(); i.hasNext();) {
            String aliasName = (String) i.next();
            addSnippetDefinition(aliasName, d);
        }
    }

    private static void addSnippetDefinition(String name, SnippetDefinition d) {
        SnippetDefinition replaced = (SnippetDefinition) SNIPPETS.put(name, d);
        if (replaced != null) {
            String replacedVersion = replaced.getVersion();
            String thisVersion = d.getVersion();
            if (DashPackage.compareVersions(replacedVersion, thisVersion) > 0)
                SNIPPETS.put(name, replaced);
        }
    }

    /** Get a snippet definition.
     * 
     * @param id the ID or alias of the snippet to find.
     */
    public static SnippetDefinition getSnippet(String id) {
        return (SnippetDefinition) SNIPPETS.get(id);
    }

    /** Return a list of all the snippets known to this manager.
     */
    public static Set<SnippetDefinition> getAllSnippets() {
        Set result = new HashSet(SNIPPETS.values());
        return result;
    }

    public static Set<SnippetDefinition> getSnippetsInCategory(String category) {
        Set<SnippetDefinition> result = new LinkedHashSet();
        for (SnippetDefinition snip : SNIPPETS.values()) {
            if (snip.getCategory().startsWith(category))
                result.add(snip);
        }
        return result;
    }

}
