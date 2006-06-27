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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.util.HTMLUtils;

/** Parses data posted by an editing page, and constructs the page content
 * described by that page.
 */
public class EditedPageDataParser implements EditPageParameters,
        Needs.Parameters, Needs.Environment, Needs.Prefix, Needs.Data {

    private static final Logger logger = Logger
            .getLogger(EditedPageDataParser.class.getName());

    protected Map environment;

    protected Map parameters;

    protected String prefix;

    protected DataContext dataContext;

    protected SnippetInvoker invoker;

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

    public PageContentTO parsePostedPageContent() {
        PageContentTO page = new PageContentTO();
        parsePageMetaData(page);
        parseSnippetData(page);
        return page;
    }

    private void parsePageMetaData(PageContentTO page) {
        page.setPageTitle(getParameter(PAGE_TITLE));
    }

    private void parseSnippetData(PageContentTO page) {
        List snippets = new ArrayList();

        String[] snippetNamespaces = (String[]) parameters.get(SNIPPET_INSTANCE
                + "_ALL");
        for (int i = 0; i < snippetNamespaces.length; i++) {
            SnippetInstanceTO snip = parseSnippet(snippetNamespaces[i]);
            if (snip != null)
                snippets.add(snip);
        }

        page.setContentSnippets(snippets);
    }

    private SnippetInstanceTO parseSnippet(String ns) {
        if ("t".equals(getParameter(SNIPPET_DISCARDED_ + ns)))
            return null;

        SnippetInstanceTO snip = new SnippetInstanceTO();
        snip.setSnippetID(getParameter(SNIPPET_ID_ + ns));
        snip.setSnippetVersion(getParameter(SNIPPET_VERSION_ + ns));
        if (parameters.containsKey(SNIPPET_VERBATIM_TEXT_ + ns)) {
            snip.setPersistedText(getParameter(SNIPPET_VERBATIM_TEXT_ + ns));
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
        StringBuffer text = new StringBuffer();
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String name = (String) e.getKey();
            if (name.startsWith(ns) && name.endsWith("_ALL")) {
                name = HTMLUtils.urlEncode(name.substring(ns.length(), name
                        .length() - 4));
                String[] values = (String[]) e.getValue();
                for (int j = 0; j < values.length; j++) {
                    text.append("&").append(name).append("=").append(
                            HTMLUtils.urlEncode(values[j]));
                }
            }
        }
        if (text.length() > 0)
            snippet.setPersistedText(text.substring(1));
    }

    private void invokeSnippet(SnippetInstanceTO snippet, String ns) {
        try {
            snippet.setNamespace(ns);
            SnippetInvoker invoker = getInvoker();
            String text = invoker.invoke(snippet);
            snippet.setPersistedText(text);
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
}
