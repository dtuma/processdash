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
import java.util.TreeSet;

import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.ui.DataComboBox;
import net.sourceforge.processdash.ui.OptionList;
import net.sourceforge.processdash.ui.web.TinyCGIBase;



/** CGI script for generating data element selection lists.
 */
public class DataNameSelectElem extends TinyCGIBase {

    private String initialPart = null;
    private String finalPart = null;

    protected void writeContents() throws IOException {
        if (initialPart == null) init();

        out.print(initialPart);
        out.print(getParameter("name"));
        out.print(finalPart);
    }

    protected void init() {
        DataRepository data = getDataRepository();
        if (data == null) return;

        TreeSet s = new TreeSet(DataComboBox.getAllDataNames(data));
        s.add("");
        OptionList opt = new OptionList(s);
        String html = opt.getAsHTML(BIZZARE_NAME);
        int pos = html.indexOf(BIZZARE_NAME);
        initialPart = html.substring(0, pos);
        finalPart = html.substring(pos+BIZZARE_NAME.length());
    }

    private static final String BIZZARE_NAME = "!@#$";
}
