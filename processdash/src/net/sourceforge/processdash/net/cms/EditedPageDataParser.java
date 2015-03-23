// Copyright (C) 2006-2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.net.cms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.ui.snippet.SnippetDefinition;
import net.sourceforge.processdash.ui.snippet.SnippetDefinitionManager;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

/** Parses data posted by an editing page, and constructs the page content
 * described by that page.
 */
public class EditedPageDataParser implements EditPageParameters,
        Needs.Parameters, Needs.Environment, Needs.Prefix, Needs.Data {

    private static final Logger logger = Logger
            .getLogger(EditedPageDataParser.class.getName());

    private static final ParamDataPersister PERSISTER =
            new XmlParamDataPersisterV1();

    protected Map environment;

    protected Map parameters;

    protected String prefix;

    protected DataContext dataContext;

    protected SnippetInvoker invoker;

    protected int nextInstanceID = 100;

    public void setData(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    public void setEnvironment(Map environment) {
        this.environment = environment;
    }

    public void setParameters(Map parameters) {
        this.parameters = new TreeMap(parameters);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    protected String getParameter(String name) {
        return (String) parameters.get(name);
    }

    protected int getIntParameter(String name) {
        return Integer.parseInt((String) parameters.get(name));
    }

    public PageContentTO parsePostedPageContent() {
        PageContentTO page = new PageContentTO();
        parsePageMetaData(page);
        parseSnippetData(page);
        return page;
    }

    private void parsePageMetaData(PageContentTO page) {
        page.setPageTitle(getParameter(PAGE_TITLE));

        String[] metadataNames = (String[]) parameters.get(PAGE_METADATA
            + "_ALL");
        if (metadataNames != null) {
            for (String name : metadataNames) {
                String value = (String) parameters.get(PAGE_METADATA + '_'
                        + name);
                page.setMetadataValue(name, value);
            }
        }
    }

    private void parseSnippetData(PageContentTO page) {
        List snippets = new ArrayList();

        String[] snippetNamespaces = (String[]) parameters.get(SNIPPET_INSTANCE
                + "_ALL");
        if (snippetNamespaces != null) {
            for (int i = 0; i < snippetNamespaces.length; i++) {
                SnippetInstanceTO snip = parseSnippet(snippetNamespaces[i]);
                if (snip != null)
                    snippets.add(snip);
            }
            for (Iterator i = snippets.iterator(); i.hasNext();) {
                SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
                if (!XMLUtils.hasValue(snip.getInstanceID()))
                    snip.setInstanceID(Integer.toString(nextInstanceID++));
            }
        }

        page.setSnippets(snippets);
    }

    private SnippetInstanceTO parseSnippet(String ns) {
        if (StringUtils.hasValue(getParameter(SNIPPET_DISCARDED_ + ns)))
            return null;

        SnippetInstanceTO snip = new SnippetInstanceTO();
        snip.setSnippetID(getParameter(SNIPPET_ID_ + ns));
        snip.setSnippetVersion(getParameter(SNIPPET_VERSION_ + ns));
        snip.setPageRegion(getIntParameter(SNIPPET_PAGE_REGION_ + ns));

        String instId = getParameter(SNIPPET_INSTANCE_ID_ + ns);
        if (AbstractPageAssembler.AUTO_HEADER_INSTANCE_ID.equals(instId))
            return null;
        else if (XMLUtils.hasValue(instId))
            try {
                nextInstanceID = Math.max(nextInstanceID,
                        Integer.parseInt(instId) + 1);
            } catch (NumberFormatException nfe) {}
        snip.setInstanceID(instId);

        if (parameters.containsKey(SNIPPET_VERBATIM_TEXT_ + ns)) {
            snip.setPersistedText(getParameter(SNIPPET_VERBATIM_TEXT_ + ns));
            snip.setPersisterID(getParameter(SNIPPET_VERBATIM_PERSISTER_+ ns));
        } else {
            SnippetDefinition defn = SnippetDefinitionManager.getSnippet(snip
                    .getSnippetID());
            if (defn == null)
                return snip;
            snip.setDefinition(defn);
            if (defn.shouldParsePersistedText())
                parseParameters(snip, ns);
            else
                invokeSnippet(snip, ns);
        }
        return snip;
    }

    private void parseParameters(SnippetInstanceTO snippet, String ns) {
        Map filteredParams = filterParamMap(parameters, new TreeMap(), ns,
                null, false, true);
        String text = PERSISTER.getTextToPersist(filteredParams);
        snippet.setPersistedText(text);
        snippet.setPersisterID(PERSISTER.getIdentifier());
    }

    private void invokeSnippet(SnippetInstanceTO snippet, String ns) {
        try {
            snippet.setNamespace(ns);
            SnippetInvoker invoker = getInvoker();
            String text = invoker.invoke(snippet);
            snippet.setPersistedText(text);
            snippet.setPersisterID(null);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to save data for snippet '"
                    + snippet.getSnippetID() + "'", e);
        }
    }

    private SnippetInvoker getInvoker() {
        if (invoker == null)
            invoker = new SnippetInvoker(environment, parameters, prefix,
                    dataContext);
        return invoker;
    }

    /** Search through a map for keys matching a certain prefix and/or suffix,
     * and copy the matching entries into another map.
     * 
     * @param src the Map to search.
     * @param dest the Map to place matching entries in
     * @param prefix a prefix to look for in key names; null means no prefix
     * @param suffix a suffix to look for in key names; null means no suffix
     * @param removeFromOriginal if true, items found will be removed from the
     *     src map as they are added to the dest map
     * @param trim if true, the prefix and/or suffix will be stripped from the
     *     key when placing the entries into dest
     * @return the dest map
     */
    public static Map filterParamMap(Map src, Map dest,
            String prefix, String suffix, boolean removeFromOriginal,
            boolean trim) {

        for (Iterator i = src.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (e.getKey() instanceof String) {
                String name = (String) e.getKey();
                if (prefix != null) {
                    if (!name.startsWith(prefix))
                        continue;
                    else if (trim)
                        name = name.substring(prefix.length());
                }
                if (suffix != null) {
                    if (!name.endsWith(suffix))
                        continue;
                    else if (trim)
                        name = name.substring(0, name.length() - suffix.length());
                }

                dest.put(name, e.getValue());
                if (removeFromOriginal)
                    i.remove();
            }
        }
        return dest;
    }
}
