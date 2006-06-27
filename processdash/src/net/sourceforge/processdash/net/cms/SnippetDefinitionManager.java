// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;

/** Keeps track of all the snippets defined by the dashboard and add-ons
 * in the current configuration.
 */
public class SnippetDefinitionManager {

    private static final Logger logger = Logger
            .getLogger(SnippetDefinitionManager.class.getName());

    private static final Map SNIPPETS = new HashMap();

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

        List config = TemplateLoader
                .getXmlConfigurationElements(SnippetDefinition.SNIPPET_TAG);
        for (Iterator i = config.iterator(); i.hasNext();) {
            Element e = (Element) i.next();
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
    public static Set getAllSnippets() {
        Set result = new HashSet(SNIPPETS.values());
        return result;
    }
}
