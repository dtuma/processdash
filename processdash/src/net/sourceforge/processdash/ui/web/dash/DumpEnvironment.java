// Copyright (C) 2003 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.dash;


import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.processdash.ui.web.TinyCGIBase;



public class DumpEnvironment extends TinyCGIBase {

    protected void writeContents() throws IOException {
        out.write("<HTML><body><table><tr><td>Name</td><td>Value</td></tr>\n");
        Iterator i = env.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            out.write("<tr><td>" + e.getKey() + "</td><td>" +
                      e.getValue() + "</td></tr>\n");
        }
        out.write("</table></body></html>");
    }
}
