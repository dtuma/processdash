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
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.ui.web.reports.analysis.AnalysisPage;
import net.sourceforge.processdash.util.HTMLUtils;

/** Abstract base class for renderers that generate a single page of content.
 */
public abstract class AbstractPageAssembler implements PageAssembler,
        Needs.Environment, Needs.Parameters, Needs.Prefix, Needs.Data {

    protected static final String AUTO_HEADER_INSTANCE_ID = "auto";
    public static final String PAGE_URI_PARAM = "cmsPageUri";
    public static final String PAGE_TITLE_PARAM = "cmsPageTitle";
    public static final String LOCALIZED_PREFIX_PARAM = "cmsLocalizedPrefix";

    protected static final Resources resources = Resources
            .getDashBundle("CMS.Snippet");

    protected static final String HTML_STRICT_DOCTYPE =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\n" +
        "    \"http://www.w3.org/TR/html4/strict.dtd\">\n";
    protected static final String HTML_TRANSITIONAL_DOCTYPE =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
        "    \"http://www.w3.org/TR/html4/loose.dtd\">\n";

    // collect information, via the Needs interfaces.

    protected Map environment;

    protected Map parameters;

    protected String prefix;

    protected DataContext dataContext;

    public void setData(DataContext dataContext) {
        this.dataContext = dataContext;
    }

    public void setEnvironment(Map environment) {
        this.environment = environment;
    }

    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }


    // methods that subclasses can override.

    protected abstract void writePage(Writer out, Set headerItems,
            PageContentTO page) throws IOException;

    private boolean shouldClobberForms() {
        return false;
    }

    protected void setSnippetNamespace(SnippetInstanceTO snippet,
            String namespace) {
        snippet.setNamespace(namespace);
    }

    protected void addPageSpecificHeaderItems(Set headerItems) {
        addStyleSheet(headerItems, "/style.css");
    }


    /**  Handle the default tasks associated with rendering a page.
     */
    public void service(Writer out, PageContentTO page) throws IOException {

        addPageSpecificParameters(parameters, page);

        SnippetInvoker invoker = new SnippetInvoker(environment, parameters,
                prefix, dataContext);
        String selfURI = CmsContentDispatcher.getSimpleSelfUri(environment,
                false);
        Set headerItems = new LinkedHashSet();
        addPageSpecificHeaderItems(headerItems);

        List snippets = page.getContentSnippets();
        maybeAddHeaderSnippet(snippets);

        int num = 0;
        for (Iterator i = snippets.iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            setSnippetNamespace(snip, "snip" + (num++) + "_");

            if (shouldInvokeSnippet(page, snip)) {
                try {
                    String snipContent = invoker.invoke(snip);
                    if (snipContent != null) {
                        SnippetHtmlPostprocessor post = new SnippetHtmlPostprocessor(snip
                                .getNamespace(), selfURI, snip.getUri(),
                                shouldClobberForms(), snipContent);
                        headerItems.addAll(post.getHeaderItems());
                        snip.setGeneratedContent(post.getResults());
                    } else {
                        snip.setGeneratedContent(null);
                    }
                } catch (IOException ioe) {
                    snip.setGeneratedContent(null);
                }
            } else {
                snip.setGeneratedContent(null);
            }
        }

        writePage(out, headerItems, page);
    }

    protected void maybeAddHeaderSnippet(List snippets) {
        if (!containsHeaderSnippet(snippets)) {
            SnippetInstanceTO headerSnip = getDefaultHeaderSnippet();
            headerSnip.setInstanceID(AUTO_HEADER_INSTANCE_ID);
            snippets.add(0, headerSnip);
        }
    }
    private boolean containsHeaderSnippet(List snippets) {
        for (Iterator i = snippets.iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            if (snip.isHeaderSnippet() && snip.getDefinition() != null) {
                String mode = (String) parameters.get("mode");
                if (mode == null || "view".equals(mode)
                        || snip.getDefinition().getModes().contains(mode))
                    return true;
            }
        }
        return false;
    }
    protected SnippetInstanceTO getDefaultHeaderSnippet() {
        SnippetInstanceTO snip = new SnippetInstanceTO();
        String snipID = getDefaultHeaderSnippetID();
        snip.setSnippetID(snipID);
        snip.setDefinition(SnippetDefinitionManager.getSnippet(snipID));
        snip.setHeaderSnippet(true);
        return snip;
    }
    protected String getDefaultHeaderSnippetID() {
        return "pdash.pageHeading";
    }

    protected void addPageSpecificParameters(Map params, PageContentTO page) {
        params.put(PAGE_URI_PARAM, getPageUri());
        params.put(PAGE_TITLE_PARAM, page.getPageTitle());
        params.put(LOCALIZED_PREFIX_PARAM,
                AnalysisPage.localizePrefix(prefix));
    }

    private String getPageUri() {
        String result = (String) environment.get("SCRIPT_NAME");
        result = result.substring(WebServer.CMS_URI_PREFIX.length());
        result = HTMLUtils.urlDecode(result);
        return result;
    }

    /** Return true if the SnippetInvoker should be run on the given snippet.
     * 
     * Page assemblers which do not display all of the snippets on a page may
     * choose to override this, returning false for snippets that will not
     * be displayed, to speed the rendering of pages.
     */
    protected boolean shouldInvokeSnippet(PageContentTO page,
            SnippetInstanceTO snip) {
        return true;
    }

    protected void writeHead(Writer out, Set headerItems, PageContentTO page) throws IOException {
        out.write("<head>\n");

        String pageTitle = getPageTitle(page);
        out.write("<title>");
        out.write(pageTitle);
        out.write("</title>\n");

        out.write("\n");

        for (Iterator i = headerItems.iterator(); i.hasNext();) {
            String item = (String) i.next();
            out.write(item);
            out.write('\n');
        }

        out.write("</head>\n");
    }

    protected String getPageTitle(PageContentTO page) {
        return "";
    }

    protected void addStyleSheet(Set headerItems, String uri) {
        headerItems.add(getStyleSheetLink(uri));
    }

    private String getStyleSheetLink(String uri) {
        return "<link href=\"" + uri
                + "\" rel=\"stylesheet\" type=\"text/css\">";
    }

    protected void addScript(Set headerItems, String uri) {
        headerItems.add(getScriptElem(uri));
    }

    private String getScriptElem(String uri) {
        return "<script src=\"" + uri
                + "\" type=\"text/javascript\"></script>";
    }

    protected void addFixedPositionCssItems(Set headerItems) {
        addStyleSheet(headerItems, "/dash/fixedPosition.css");
        headerItems.add("<!--[if gte IE 5.5]><![if lt IE 7]>"
                + getStyleSheetLink("/dash/fixedPositionIE.css")
                + "<![endif]><![endif]-->");
    }

    protected void addIEFloatHack(Set headerItems) {
        headerItems.add("<!--[if IE]>"
                + "<style> .IEFloatHack { height: 1% } </style>"
                + "<![endif]-->\n");
    }

    /** Convience routine */
    protected static String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

}
