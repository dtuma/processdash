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


import net.sourceforge.processdash.ui.web.TinyCGIBase;
import pspdash.data.ImmutableDoubleData;


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
