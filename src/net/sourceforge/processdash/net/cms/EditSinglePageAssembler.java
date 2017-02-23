// Copyright (C) 2006-2017 Tuma Solutions, LLC
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
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

/** Page Assember that displays a form for editing a page of content.
 */
public class EditSinglePageAssembler extends AbstractPageAssembler
        implements EditPageParameters {

    @Override
    protected void writePage(Writer out, Set headerItems, PageContentTO page)
            throws IOException {

        if (!hasEditPermission())
            throw new TinyCGIException(403, "No permission");

        out.write(HTML_TRANSITIONAL_DOCTYPE);

        out.write("<html>\n");
        writeHead(out, headerItems, page);
        out.write("<body scroll=\"yes\">\n");

        writeFormStart(out);

        writeHeaderElements(out, page);

        writePageMetadataEditors(out, page);

        writePageSnippetEditors(out, page);

        writeFooterElements(out, page);

        writeFormEnd(out);

        out.write("<script type=\"text/javascript\">DashCMS.initPage();</script>\n");

        out.write("</body>\n");
        out.write("</html>\n");
    }



    private void writeHeaderElements(Writer out, PageContentTO page)
            throws IOException {
        for (Iterator i = page.getHeaderSnippets(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            writeSnippet(out, snip, EDIT_STYLE_CHROMELESS);
        }
    }



    private void writeFooterElements(Writer out, PageContentTO page)
            throws IOException {
        for (Iterator i = page.getFooterSnippets(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            writeSnippet(out, snip, EDIT_STYLE_CHROMELESS);
        }
    }



    protected void writePageSnippetEditors(Writer out, PageContentTO page)
            throws IOException {

        out.write("<div id='snippetContainer'>\n\n");

        for (Iterator i = page.getContentSnippets(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            writeSnippet(out, snip, EDIT_STYLE_WRAPPED);
        }

        out.write("</div>\n\n");
    }



    protected void writeFormStart(Writer out) throws IOException {
        // Write a autocompletion div that any editor on the page can reuse.
        out.write("<div id='cmsAutocomplete' style='display:none'>&nbsp;"
                + "</div>\n\n");

        // write the start of the form
        out.write("<div><form method=\"POST\" target=\"_top\" action=\"");
        out.write(CmsContentDispatcher.getSimpleSelfUri(environment, true));
        out.write("\">\n\n");
    }



    protected void writeFormEnd(Writer out) throws IOException {
        out.write("<div id='fixedFooter' class='cmsButtonRow'>\n");

        out.write("<a href='#' onclick='DashCMS.addSnippet(); return false'>");
        out.write(resources.getHTML("Edit_Page.Add_Snippet"));
        out.write("</a>\n\n");

        out.write("<button type='submit' name='submit' value='save'>");
        out.write(resources.getHTML("Save"));
        out.write("</button>\n");

        out.write("<button type='button' name='cancel' value='cancel' "
                + "onclick='DashCMS.cancelEdit();'>");
        out.write(resources.getHTML("Cancel"));
        out.write("</button>\n");

        writeHidden(out, "mode", "edit");
        writeHidden(out, "action", "save", "formActionParam");

        out.write("</div>\n\n");
        out.write("</form></div>\n\n");
        out.write("<div style='height:4em'>&nbsp;</div>\n\n");
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

        writePageMetadataValues(out, page);

        out.write("</div>\n\n");
    }

    /** write hidden fields to capture page metadata. */
    protected void writePageMetadataValues(Writer out, PageContentTO page)
            throws IOException {
        for (Map.Entry<String, String> e : page.getMetadata().entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            if (name != null && value != null) {
                writeHidden(out, PAGE_METADATA, name);
                writeHidden(out, PAGE_METADATA + '_' + name, value);
            }
        }
        out.write("\n\n");
    }



    protected void addPageSpecificHeaderItems(Set headerItems) {
        super.addPageSpecificHeaderItems(headerItems);
        addScript(headerItems, "/lib/prototype.js");
        addScript(headerItems, "/lib/scriptaculous.js");
        addScript(headerItems, "/dash/cmsEdit.js");
        addStyleSheet(headerItems, "/dash/cmsEdit.css");
        addFixedPositionCssItems(headerItems);
        addIEFloatHack(headerItems);
    }


    protected static final int EDIT_STYLE_UNWRAPPED = 0;
    protected static final int EDIT_STYLE_WRAPPED = 1;
    protected static final int EDIT_STYLE_CHROMELESS = 2;
    protected static final int EDIT_STYLE_INVISIBLE = 3;

    protected static void writeSnippet(Writer out, SnippetInstanceTO snippet,
            int style) throws IOException {

        boolean wrapWithDiv = (style == EDIT_STYLE_WRAPPED);
        boolean invisible = (style == EDIT_STYLE_INVISIBLE);
        boolean chromeless = (style == EDIT_STYLE_CHROMELESS);

        int status = snippet.getStatus();
        String ns = snippet.getNamespace();
        String id = snippet.getSnippetID();
        String version = snippet.getSnippetVersion();
        if (snippet.getDefinition() != null) {
            id = snippet.getDefinition().getId();
            version = snippet.getDefinition().getVersion();
        }

        if (wrapWithDiv)
            writeDivWrapperStart(out, ns);

        if (!invisible && !chromeless) {
            out.write("<div class='cmsEditingTitle IEFloatHack'>");
            writeTitleButtonLink(out, "Delete_Item", "deleteSnippet",
                    "cmsDeleteButton");
            writeTitleButtonLink(out, "Move_Item_Down", "moveSnippetDown",
                    "cmsMoveDownButton");
            writeTitleButtonLink(out, "Move_Item_Up", "moveSnippetUp",
                    "cmsMoveUpButton");
            out.write(getSnippetDisplayName(snippet));
            out.write("</div>");

            out.write("<div class='cmsDeleteMessage' style='display:none'>\n");
            String deleteMessage = resources
                    .getString("Edit_Page.Delete_Item.Message_HTML");
            deleteMessage = StringUtils
                    .findAndReplace(deleteMessage, "<a>",
                            "<a href='#' onclick='DashCMS.undeleteSnippet(this); return false;'>");
            out.write(deleteMessage);
            out.write("</div>");

            out.write("<div class='cmsEditingRegion'>");
        }
        writeHidden(out, SNIPPET_INSTANCE, ns);
        writeHidden(out, SNIPPET_ID_ + ns, id);
        writeHidden(out, SNIPPET_VERSION_ + ns, version);
        writeHidden(out, SNIPPET_INSTANCE_ID_ + ns, snippet.getInstanceID());
        writeHidden(out, SNIPPET_DISCARDED_ + ns, "");
        writeHidden(out, SNIPPET_PAGE_REGION_ + ns,
                Integer.toString(snippet.getPageRegion()));

        if (status == SnippetInvoker.STATUS_OK && !invisible) {
            out.write(snippet.getGeneratedContent());

        } else {
            writeHidden(out, SNIPPET_VERBATIM_TEXT_ + ns, snippet
                    .getPersistedText());
            writeHidden(out, SNIPPET_VERBATIM_PERSISTER_ + ns,
                    snippet.getPersisterID());

            if (status > SnippetInvoker.STATUS_OK
                    && !invisible && !chromeless) {
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

        if (!invisible && !chromeless)
            out.write("</div>");

        if (wrapWithDiv)
            out.write("</div>");
    }



    protected static void writeDivWrapperStart(Writer out, String ns)
            throws IOException {
        out.write("<div class='cmsEditedItem' id='");
        out.write(ns);
        out.write("'>");
    }



    private static void writeTitleButtonLink(Writer out, String resKey,
            String script, String linkClass) throws IOException {
        out.write("<a href='#' class='" + linkClass + "' title=\"");
        out.write(resources.getHTML("Edit_Page." + resKey + ".Tooltip"));
        out.write("\" onclick='DashCMS." + script
                + "(this); return false'/></a>");
    }



    private static String getSnippetDisplayName(SnippetInstanceTO snippet) {
        if (snippet.getAlternateName() != null)
            return HTMLUtils.escapeEntities(snippet.getAlternateName());
        else if (snippet.getDefinition() != null)
            return snippet.getDefinition().getNameHtml();
        else
            return resources.format("Edit_Page.Unrecognized_Snippet.Name_FMT",
                    snippet.getSnippetID());
    }


    protected static void writeHidden(Writer out, String name, String value)
            throws IOException {
        writeHidden(out, name, value, null);
    }

    private static void writeHidden(Writer out, String name, String value,
            String id) throws IOException {
        out.write("<input type='hidden' name='");
        out.write(name);
        if (XMLUtils.hasValue(id)) {
            out.write("' id='");
            out.write(XMLUtils.escapeAttribute(id));
        }
        out.write("' value='");
        if (XMLUtils.hasValue(value))
            out.write(XMLUtils.escapeAttribute(value));
        out.write("'>");
    }


    protected String getPageTitle(PageContentTO page) {
        String pageName = (String) parameters.get(CMS_PAGE_FILENAME);
        String title = resources.format("Snippet.PageHeading.Title_FMT",
                pageName);
        return esc(title);
    }

}
