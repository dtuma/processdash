// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.process.ui;

import java.io.IOException;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.process.DefectTypeStandard;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class SelectDefectTypeStandard extends TinyCGIBase {

    private static final Resources resources = Resources
            .getDashBundle("Defects");

    protected void writeContents() throws IOException {
        String noStandardSelectedText = getParameter("None_Text");
        if (noStandardSelectedText == null)
            noStandardSelectedText = resources.getString("Log.Inherit_Type");

        out.println("<select name='[Defect Type Standard]s'>");
        out.print("<option value=' '>");
        out.print(HTMLUtils.escapeEntities(noStandardSelectedText));
        out.println("</option>");

        String[] standardNames = DefectTypeStandard
                .getDefinedStandards(getDataRepository());
        for (int i = 0; i < standardNames.length; i++) {
            out.print("<option>");
            out.print(HTMLUtils.escapeEntities(standardNames[i]));
            out.println("</option>");
        }

        out.println("</select>");
    }

}
