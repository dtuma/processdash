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
import java.util.Set;

import net.sourceforge.processdash.net.http.WebServer;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

/** Page Assember that displays a form for editing a page of content.
 */
public class EditSinglePageAssembler extends AbstractSinglePageAssembler
        implements EditPageParameters {

    protected void writePage(Writer out, Set headerItems, PageContentTO page)
            throws IOException {

        out.write("<html>\n");
        out.write("<head>\n");

        String pageTitle = getPageTitle();
        out.write("<title>");
        out.write(pageTitle);
        out.write("</title>\n");

        for (Iterator i = headerItems.iterator(); i.hasNext();) {
            String item = (String) i.next();
            out.write(item);
            out.write('\n');
        }

        if (isInternetExplorer())
            // workaround IE bug for floated content
            out.write("<style> .IEWidth { width: 100% } </style>\n");

        out.write("</head>\n");
        out.write("<body>\n");

        out.write("<h1>");
        out.write(pageTitle);
        out.write("</h1>\n\n");

        writeFormStart(out);

        writePageMetadataEditors(out, page);

        out.write("<div id='snippetContainer'>\n\n");

        for (Iterator i = page.getContentSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            writeSnippet(out, snip, true);
        }

        out.write("</div>\n\n");

        writeAddNewSnippetLink(out);
        writeFormEnd(out);

        out.write("<script>DashCMS.initPage();</script>\n");

        out.write("</body>\n");
        out.write("</html>\n");
    }



    private boolean isInternetExplorer() {
        String userAgent = (String) environment.get("HTTP_USER_AGENT");
        return (userAgent.indexOf("MSIE") != -1);
    }



    private void writeFormStart(Writer out) throws IOException {
        // Write a autocompletion div that any editor on the page can reuse.
        out.write("<div id='cmsAutocomplete' style='display:none'>&nbsp;"
                + "</div>\n\n");

        // write the start of the form
        out.write("<form method=\"POST\" action=\"");
        out.write(CmsContentDispatcher.getSimpleSelfUri(environment, true));
        out.write("\">\n\n");
        writeHidden(out, "mode", "edit");
        writeHidden(out, "action", "save", "formActionParam");
    }



    private void writeAddNewSnippetLink(Writer out) throws IOException {
        out.write("<div class='cmsAddNewSnippet'>\n");
        out.write("<a href='#' onclick='DashCMS.addSnippet(); return false'>");
        out.write(resources.getHTML("Edit_Page.Add_Snippet"));
        out.write("</a></div>\n\n");
    }

    private void writeFormEnd(Writer out) throws IOException {
        out.write("<div class='cmsButtonRow'>\n");

        out.write("<button type='submit' name='submit' value='save'>");
        out.write(resources.getHTML("Save"));
        out.write("</button>\n");

        out.write("<button type='button' name='cancel' value='cancel' "
                + "onclick='DashCMS.cancelEdit();'>");
        out.write(resources.getHTML("Cancel"));
        out.write("</button>\n");

        out.write("</div>\n\n");
        out.write("</form>\n\n");
    }



    protected void writePageMetadataEditors(Writer out, PageContentTO page)
            throws IOException {
        out.write("<div class='cmsMetadataSection'>\n\n");

        // write an area for the user to edit the page title.
        out.write("<b>");
        out.write(resources.getHTML("Page_Title.Label"));
        out.write("</b>&nbsp;<input type='text' size='40' " //
                + "name='" + PAGE_TITLE + "' value='");
        if (page.getPageTitle() != null)
            out.write(XMLUtils.escapeAttribute(page.getPageTitle()));
        out.write("'/>\n\n");

        out.write("</div>\n\n");
    }



    protected void addPageSpecificHeaderItems(Set headerItems) {
        super.addPageSpecificHeaderItems(headerItems);
        addScript(headerItems, "/js/prototype.js");
        addScript(headerItems, "/js/scriptaculous.js");
        addScript(headerItems, "/dash/cmsEdit.js");
        addStyleSheet(headerItems, "/dash/cmsEdit.css");
    }



    protected static void writeSnippet(Writer out,
            SnippetInstanceTO snippet, boolean wrapWithDiv)
            throws IOException {

        int status = snippet.getStatus();
        String ns = snippet.getNamespace();
        String id = snippet.getSnippetID();
        String version = snippet.getSnippetVersion();
        if (snippet.getDefinition() != null) {
            id = snippet.getDefinition().getId();
            version = snippet.getDefinition().getVersion();
        }

        if (wrapWithDiv) {
            out.write("<div class='cmsEditedItem' id='");
            out.write(ns);
            out.write("'>");
        }

        out.write("<div class='cmsEditingTitle IEWidth'>");
        writeTitleButtonLink(out, "Delete_Item", "deleteSnippet",
                "/Images/delete.gif", "cmsEditingTitleDeleteButton");
        //        writeTitleButtonLink(out, "Move_Item_Down", "moveSnippetDown", "/Images/down.gif", "cmsEditingTitleButton");
        //        writeTitleButtonLink(out, "Move_Item_Up", "moveSnippetUp", "/Images/up.gif", "cmsEditingTitleButton");
        out.write(getSnippetDisplayName(snippet));
        out.write("</div>");

        out.write("<div class='cmsDeleteMessage' style='display:none'>\n");
        writeHidden(out, SNIPPET_DISCARDED_ + ns, "");
        String deleteMessage = resources
                .getString("Edit_Page.Delete_Item.Message_HTML");
        deleteMessage = StringUtils
                .findAndReplace(deleteMessage, "<a>",
                        "<a href='#' onclick='DashCMS.undeleteSnippet(this); return false;'>");
        out.write(deleteMessage);
        out.write("</div>");

        out.write("<div class='cmsEditingRegion'>");
        writeHidden(out, SNIPPET_INSTANCE, ns);
        writeHidden(out, SNIPPET_ID_ + ns, id);
        writeHidden(out, SNIPPET_VERSION_ + ns, version);

        if (status == SnippetInvoker.STATUS_OK) {
            out.write(snippet.getGeneratedContent());

        } else {
            writeHidden(out, SNIPPET_VERBATIM_TEXT_ + ns, snippet
                    .getPersistedText());

            if (status != SnippetInvoker.STATUS_NOT_RUN) {
                String key = "Edit_Page.Errors."
                        + SnippetInvoker.STATUS_RESOURCE_KEYS[status]
                        + "_FMT";
                String errDump = StringUtils.getStackTrace(snippet
                        .getInvocationException());
                out.write("<span class='cmsNoEditMessage'>");
                out.write(esc(resources.format(key, id)));
                out.write("<!-- ");
                out.write(errDump);
                out.write(" --></span>\n");
            }
        }

        out.write("</div>");

        if (wrapWithDiv)
            out.write("</div>");
    }



    private static void writeTitleButtonLink(Writer out, String resKey,
            String script, String imgSrc, String linkClass) throws IOException {
        out.write("<a href='#' class='" + linkClass + "' title=\"");
        out.write(resources.getHTML("Edit_Page." + resKey + ".Tooltip"));
        out.write("\" onclick='DashCMS." + script + "(this); return false'/>");
        out.write("<img border='0' src='" + imgSrc + "'/></a>");
    }



    private static String getSnippetDisplayName(SnippetInstanceTO snippet) {
        if (snippet.getDefinition() != null)
            return snippet.getDefinition().getNameHtml();
        else
            return resources.format("Edit_Page.Unrecognized_Snippet.Name_FMT",
                    snippet.getSnippetID());
    }

    private static void writeHidden(Writer out, String name, String value)
            throws IOException {
        writeHidden(out, name, value, null);
    }

    private static void writeHidden(Writer out, String name, String value,
            String id) throws IOException {
        out.write("<input type='hidden' name='");
        out.write(name);
        if (id != null) {
            out.write("' id='");
            out.write(XMLUtils.escapeAttribute(id));
        }
        out.write("' value='");
        if (value != null)
            out.write(XMLUtils.escapeAttribute(value));
        out.write("'/>");
    }

    private String getPageTitle() {
        String pageName = (String) environment.get("SCRIPT_NAME");
        pageName = pageName.substring(WebServer.CMS_URI_PREFIX.length());
        pageName = HTMLUtils.urlDecode(pageName);

        String title = resources.format("Edit_Page.Title_FMT", pageName);
        return esc(title);
    }

}
