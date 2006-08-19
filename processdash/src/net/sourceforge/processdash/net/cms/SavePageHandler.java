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
import java.io.OutputStream;
import java.io.Writer;

import net.sourceforge.processdash.Settings;

/** Handle a user request to save changes to a page.
 */
public class SavePageHandler extends EditedPageDataParser implements ActionHandler {

    public String service(Writer out, String pageName) throws IOException {
        if (Settings.isReadOnly()) {
            out.write("Location: /dash/snippets/saveError.shtm?err=Read_Only"
                    + "\r\n\r\n");
            return null;
        }

        // read the description of the page from posted form data
        PageContentTO page = parsePostedPageContent();

        // get an output stream for saving the contents
        OutputStream dest = CmsDefaultConfig.getPersistence().save(pageName);

        // serialize the contents to the output stream
        ContentSerializer ser = CmsDefaultConfig.getSerializer();
        ser.format(page, dest);
        dest.close();

        // ask the dispatcher to redirect to the plain viewing page.
        return "";
    }

}
