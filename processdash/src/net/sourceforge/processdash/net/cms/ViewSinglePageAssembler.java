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
import java.util.Map;
import java.util.Set;

import net.sourceforge.processdash.ui.web.reports.analysis.AnalysisPage;
import net.sourceforge.processdash.util.StringUtils;

/** Page assembler that can render HTML for viewing a page of CMS content.
 */
public class ViewSinglePageAssembler extends AbstractSinglePageAssembler {

    protected void writePage(Writer out, Set headerItems, PageContentTO page)
            throws IOException {

        out.write("<html>\n");
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
        out.write("<body>\n");

        out.write("<h1>");

        out.write("<span class='cmsToolbar'><a href='");
        out.write(getEditURI(environment));
        out.write("' title='");
        out.write(resources.getHTML("Edit_Hyperlink.Description"));
        out.write("'><img src='/Images/edit22.gif' " +
                        "width='22' height='22' border='0'/></a>");
        out.write("</span>\n");

        out.write(esc(AnalysisPage.localizePrefix(prefix)));
        out.write("</h1>\n");

        out.write("<h2>");
        out.write(pageTitle);
        out.write("</h2>\n\n");

        for (Iterator i = page.getContentSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            writeSnippet(out, snip);
        }

        out.write("<script src='/data.js' type='text/javascript'/>\n");
        out.write("</body>\n");
        out.write("</html>\n");
    }




    private String getEditURI(Map env) {
        // TODO: do we need to preserve other query parameter values?
        return CmsContentDispatcher.getSimpleSelfUri(env, true) + "?mode=edit";
    }


    private void writeSnippet(Writer out, SnippetInstanceTO snippet)
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

    private String getPageTitle(PageContentTO page) {
        if (page.getPageTitle() != null)
            return esc(page.getPageTitle());
        else
            return resources.getHTML("Page_Title.Empty");
    }

}
