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

/** Page assembler that can render HTML for viewing a page of CMS content.
 */
public class ViewSinglePageAssembler extends AbstractViewPageAssembler {

    protected void writePage(Writer out, Set headerItems, PageContentTO page)
            throws IOException {

        //out.write(HTML_TRANSITIONAL_DOCTYPE);
        out.write("<html>\n");
        writeHead(out, headerItems, page);
        out.write("<body>\n");
        out.write("<div><form>\n\n");

        for (Iterator i = page.getSnippets().iterator(); i.hasNext();) {
            SnippetInstanceTO snip = (SnippetInstanceTO) i.next();
            writeSnippet(out, snip);
        }

        out.write("</form></div>\n\n");
        out.write("<script src='/data.js' type='text/javascript'> </script>\n");
        out.write("</body>\n");
        out.write("</html>\n");
    }

}
