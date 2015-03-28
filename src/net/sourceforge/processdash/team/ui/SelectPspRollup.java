// Copyright (C) 2001-2012 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
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

package net.sourceforge.processdash.team.ui;

import java.io.IOException;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.team.sync.HierarchySynchronizer;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;

public class SelectPspRollup extends TinyCGIBase {

    protected void writeContents() throws IOException {
        String selector = getRollupSelector(getDataContext(), //
            "[" + HierarchySynchronizer.PSP_SUBSET + "]s", null);

        out.write("<!--#set var=\"PSP_ROLLUP_SELECTOR\" inline=t -->");
        if (selector != null)
            out.write(selector);
        out.write("<!--#endset-->");
    }


    public static String getRollupSelector(DataContext ctx,
            String formElemName, String defaultRollupName) {
        ListData rollups = ListData.asListData(ctx
                .getSimpleValue(PSP_ROLLUP_LIST));
        if (rollups == null || rollups.size() < 2)
            return null;

        if (defaultRollupName == null) {
            SimpleData d = ctx.getSimpleValue(HierarchySynchronizer.PSP_SUBSET);
            if (d != null)
                defaultRollupName = d.format();
            else
                defaultRollupName = PSP_DEFAULT_ROLLUP;
        }

        StringBuilder result = new StringBuilder();
        result.append("<select name=\"" + formElemName + "\">");
        for (int i = 0; i < rollups.size(); i++) {
            String oneRollup = (String) rollups.get(i);
            String oneRollupHtml = HTMLUtils.escapeEntities(oneRollup);
            result.append("<option");
            if (oneRollup.equals(defaultRollupName))
                result.append(" selected=\"selected\"");
            result.append(" value=\"").append(oneRollupHtml).append("\">")
                    .append(oneRollupHtml);
        }
        result.append("</select>");
        return result.toString();
    }

    private static final String PSP_ROLLUP_LIST = "/PSP/Rollup Instance List";
    private static final String PSP_DEFAULT_ROLLUP = "/To Date/PSP/All";

}
