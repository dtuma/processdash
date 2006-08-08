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
import java.util.Map;

import net.sourceforge.processdash.util.StringUtils;


public abstract class AbstractViewPageAssembler extends AbstractPageAssembler {

    protected void writeEditLink(Writer out) throws IOException {
        writeEditLink(out, "32", "_top");
    }

    protected void writeEditLink(Writer out, String iconSize, String target)
            throws IOException {
        if (parameters.get("EXPORT") == null) {
            out.write("<span class='cmsToolbar'><a href='");
            out.write(getEditURI(environment));
            if (target != null)
                out.write("' target='" + target);
            out.write("' title='");
            out.write(resources.getHTML("Edit_Hyperlink.Description"));
            out.write("'>");
            out.write(StringUtils.findAndReplace(EDIT_ICON_IMG_HTML, "##",
                    iconSize));
            out.write("</a></span>\n");
        }
    }
    private static final String EDIT_ICON_IMG_HTML =
        "<img src='/Images/edit##.gif' width='##' height='##' border='0'/>";

    protected String getEditURI(Map env) {
        // TODO: do we need to preserve other query parameter values?
        return CmsContentDispatcher.getSimpleSelfUri(env, true) + "?mode=edit";
    }

    protected void writeSnippet(Writer out, SnippetInstanceTO snippet)
            throws IOException {

        int status = snippet.getStatus();
        if (status == -1)
            ; // Hmmm...do nothing.

        else if (status == SnippetInvoker.STATUS_OK)
            out.write(snippet.getGeneratedContent());

        else {
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
