// Copyright (C) 2006-2018 Tuma Solutions, LLC
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
import java.io.OutputStream;
import java.io.Writer;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.net.http.TinyCGIException;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

/** Handle a user request to save changes to a page.
 */
public class SavePageHandler extends EditedPageDataParser implements ActionHandler {

    public String service(Writer out, String pageName) throws IOException {
        TinyCGIBase.rejectCrossSiteRequests(environment);
        if (Settings.isReadOnly()) {
            out.write("Location: /dash/snippets/saveError.shtm?err=Read_Only"
                    + "\r\n\r\n");
            return null;
        } else if (!AbstractPageAssembler.hasEditPermission()) {
            throw new TinyCGIException(403, "No permission");
        }

        // read the description of the page from posted form data
        PageContentTO page = parsePostedPageContent();

        // get an output stream for saving the contents
        String persistenceQualifier = page.getMetadataValue(
            PersistenceService.QUALIFIER_ATTRIBUTE_NAME, null);
        OutputStream dest = CmsDefaultConfig.getPersistence().save(
            persistenceQualifier, pageName);

        // serialize the contents to the output stream
        ContentSerializer ser = CmsDefaultConfig.getSerializer();
        ser.format(page, dest);
        dest.close();

        // ask the dispatcher to redirect to the plain viewing page.
        return "";
    }

}
