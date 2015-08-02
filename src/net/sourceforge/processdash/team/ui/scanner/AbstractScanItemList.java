// Copyright (C) 2015 Tuma Solutions, LLC
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

package net.sourceforge.processdash.team.ui.scanner;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.api.PDashData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.snippet.SnippetEnvironment;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public abstract class AbstractScanItemList extends TinyCGIBase {

    private String listID;

    private static final Resources resources = Resources
            .getDashBundle("Analysis.Scanner");

    protected AbstractScanItemList(String listID) {
        this.listID = listID;
    }

    @Override
    protected void writeContents() throws IOException {
        List<Object[]> items = getItems();
        filterItems(items);
        if (items.isEmpty()) {
            out.write("<!-- no " + listID + " items were found -->\n");
            return;
        }

        Resources res = (Resources) env.get(SnippetEnvironment.RESOURCES);
        boolean readOnly = Settings.isReadOnly();
        out.write("<html><body>\n<div class=\"scanItemList " + listID + "\">");
        out.write("<p>" + res.getHTML("Header") + "</p>\n");
        out.write(readOnly ? "<ul>\n" : "<table style=\"margin-left:1cm\">");
        String clearItemTooltip = resources.getHTML("Clear_Item_Tooltip");

        for (Object[] oneItem : items) {
            String itemText = res.format("Item_FMT", oneItem);
            String itemHtml = HTMLUtils.escapeEntities(itemText);
            itemHtml = StringUtils.findAndReplace(itemHtml, " -- ",
                "&nbsp;&mdash;&nbsp;");
            if (readOnly) {
                out.write("<li>");
            } else {
                out.write("<tr><td valign=\"top\">");
                out.write("<input type=\"checkbox\" name=\"[" + listID + "/");
                out.write(HTMLUtils.escapeEntities(getItemID(oneItem)));
                out.write("]d\" title=\"");
                out.write(clearItemTooltip);
                out.write("\"></td><td>");
            }
            out.write(itemHtml);
            out.write(readOnly ? "</li>\n" : "</td></tr>\n");
        }

        out.write(readOnly ? "</ul>\n" : "</table>\n");
        out.write("</div>\n</body></html>\n");
    }

    protected abstract List<Object[]> getItems();

    private void filterItems(List<Object[]> items) {
        PDashData data = getPdash().getData().getChild(listID);
        for (Iterator<Object[]> i = items.iterator(); i.hasNext();) {
            Object[] oneItem = i.next();
            if (data.getTest(getItemID(oneItem)))
                i.remove();
        }
    }

    protected String getItemID(Object[] oneItem) {
        return (String) oneItem[0];
    }

}
