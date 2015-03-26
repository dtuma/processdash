// Copyright (C) 2009 Tuma Solutions, LLC
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

public class HyperlinkListEditor extends AbstractListEditor {

    @Override
    protected void writeItemAdditionGui(String namespace, String itemType)
            throws IOException {
        String newItemPrompt = getRequiredParam("newItemPrompt");

        // write the hyperlink
        out.write("<a href=\"#\" onclick=\"DashCMS.addItemToList('");
        out.write(namespace);
        out.write(itemType);
        out.write("'); return false;\">");
        out.write(newItemPrompt);
        out.write("</a>");
    }

}
