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

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public abstract class AbstractViewPageAssembler extends AbstractPageAssembler {

    public static final String MERGE_TABLES_DIRECTIVE = "<!-- MERGE TABLES ";
    public static final String MERGE_TABLES_CUT_MARK = "<!-- MERGE TABLES CUT -->";
    public static final String DISALLOW_EDITING_TAG = "CMS_DISALLOW_EDITING";

    protected String insertEditLink(String html) {
        if (html == null || html.length() == 0) return "";
        if (parameters.containsKey("EXPORT") || shouldBeReadOnly())
            return html;

        int pos = html.indexOf("<!-- editLink");
        if (pos == -1) return html;
        int end = html.indexOf("-->", pos);
        if (end == -1) return html;

        StringBuffer link = new StringBuffer();
        link.append("<span class='cmsToolbar'><a href='");
        link.append(HTMLUtils.escapeEntities(getEditURI(environment)));
        String target = getEditLinkTarget();
        if (target != null)
            link.append("' target='").append(target);
        link.append("' title='");
        link.append(resources.getHTML("Edit_Hyperlink.Description"));
        link.append("'>");
        link.append(EDIT_ICON_IMG_HTML);
        link.append("</a></span>\n");

        int size=32;
        if (html.lastIndexOf("Med", end) > pos) size = 22;
        else if (html.lastIndexOf("Small", end) > pos) size = 14;
        StringUtils.findAndReplace(link, "##", Integer.toString(size));

        return html.substring(0, pos) + link + html.substring(end+3);
    }
    private static final String EDIT_ICON_IMG_HTML =
        "<img src='/Images/edit##.gif' width='##' height='##' border='0'/>";

    protected boolean shouldBeReadOnly() {
        if (Settings.isReadOnly())
            return true;

        SimpleData d = dataContext.getSimpleValue(DISALLOW_EDITING_TAG);
        if (d != null && d.test())
            return true;

        if (!hasEditPermission())
            return true;

        return false;
    }

    protected String getEditURI(Map env) {
        return CmsContentDispatcher.getSimpleSelfUri(env, true) + "?mode=edit";
    }

    protected String getEditLinkTarget() {
        return "_top";
    }

    @Override
    protected void beforeWritePage(Set headerItems, PageContentTO page) {
        super.beforeWritePage(headerItems, page);

        SnippetInstanceTO prevSnip = null;
        for (Iterator i = page.getSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snippet = (SnippetInstanceTO) i.next();
            if (snippet.getStatus() == SnippetInvoker.STATUS_OK) {
                maybeMergeTables(snippet, prevSnip);
                prevSnip = snippet;
            } else {
                prevSnip = null;
            }
        }

    }

    private void maybeMergeTables(SnippetInstanceTO snippet,
            SnippetInstanceTO prevSnip) {
        if (snippet == null || prevSnip == null)
            return;

        String snipContent = snippet.getGeneratedContent();
        String mergeId = getMergeId(snipContent);
        if (mergeId == null || !mergeId.equals(prevSnip.getInstanceID()))
            return;

        String prevContent = prevSnip.getGeneratedContent();
        if (prevContent == null)
            return;

        int cutMarkPos = snipContent.indexOf(MERGE_TABLES_CUT_MARK);
        int tablePos = prevContent.lastIndexOf("</table>");
        if (cutMarkPos == -1 || tablePos == -1)
            return;

        String newPrevContent = prevContent.substring(0, tablePos);
        String newSnipContent = snipContent.substring(cutMarkPos
            + MERGE_TABLES_CUT_MARK.length());

        prevSnip.setGeneratedContent(newPrevContent);
        snippet.setGeneratedContent(newSnipContent);
    }

    private String getMergeId(String content) {
        if (content == null || !content.startsWith(MERGE_TABLES_DIRECTIVE))
            return null;
        content = content.substring(MERGE_TABLES_DIRECTIVE.length()).trim();
        int endPos = content.indexOf('-');
        return content.substring(0, endPos).trim();
    }


    protected void writeSnippet(Writer out, SnippetInstanceTO snippet)
            throws IOException {

        int status = snippet.getStatus();
        if (status < SnippetInvoker.STATUS_OK)
            ; // Do nothing.

        else if (status == SnippetInvoker.STATUS_OK) {
            String generatedContent = snippet.getGeneratedContent();
            if (snippet.getPageRegion() == PageContentTO.REGION_HEADER)
                generatedContent = insertEditLink(generatedContent);
            out.write(generatedContent);

        } else {
            String key = "View_Page.Errors."
                    + SnippetInvoker.STATUS_RESOURCE_KEYS[status] + "_FMT";
            String id = snippet.getSnippetID();
            String errDump = StringUtils.getStackTrace(snippet
                    .getInvocationException());
            out.write("<!-- ");
            out.write(esc(resources.format(key, id)));
            out.write(esc(errDump));
            out.write(" -->\n");
        }
    }

    protected String getPageTitle(PageContentTO page) {
        if (StringUtils.hasValue(page.getPageTitle()))
            return esc(page.getPageTitle());
        else
            return resources.getHTML("Page_Title.Empty");
    }
}
