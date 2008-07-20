// Copyright (C) 2002-2003 Tuma Solutions, LLC
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


import net.sourceforge.processdash.data.ImmutableDoubleData;
import net.sourceforge.processdash.ui.web.TinyCGIBase;


public class ExpandCollapseHierarchy extends TinyCGIBase {

    static int uniqueNumber = 0;

    protected void writeHeader() {

        ImmutableDoubleData expand = ImmutableDoubleData.TRUE;
        String elem = getParameter("expand");
        if (elem == null) {
            expand = ImmutableDoubleData.FALSE;
            elem = getParameter("collapse");
        }

        if (elem != null)
            getDataRepository().userPutValue(elem, expand);

        String referer = (String) env.get("HTTP_REFERER");
        // if there is a hash fragment on the referer url, remove it.
        int pos = referer.lastIndexOf('#');
        if (pos != -1) referer = referer.substring(0, pos);

        String dest;
        pos = referer.lastIndexOf("expRel=");
        if (pos != -1 && referer.indexOf('&', pos) == -1)
            dest = referer.substring(0, pos) + "expRel=" + uniqueNumber++;
        else
            dest = referer + (referer.indexOf('?') == -1 ? "?" : "&") +
                "expRel=" + uniqueNumber++;
        out.print("Location: " + dest + "#exp_"+elem.hashCode() + "\r\n\r\n");
    }

    protected void writeContents() {}

}
