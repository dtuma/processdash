// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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
