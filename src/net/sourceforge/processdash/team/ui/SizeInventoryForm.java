// Copyright (C) 2002-2017 Tuma Solutions, LLC
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

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class SizeInventoryForm extends TinyCGIBase {

    private static final String EXTRA_ROWS_PARAM = "addRows";
    private static final int NUM_EXTRA_ROWS = 5;
    private long highlightTimestamp;
    private StringBuffer expansionText;

    public void writeContents() throws IOException {
        String prefix = getPrefix();
        if (prefix == null) prefix = "/";

        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(prefix);

        String uri = getURI();
        highlightTimestamp = getHighlightTimestamp();

        out.println(HTML_HEADER);
        out.println("<h2>");
        out.print(HTMLUtils.escapeEntities(prefix));
        out.println("</h2><form name='sizes'>");
        out.flush();

        if (!prefix.endsWith("/")) prefix = prefix + "/";
        expansionText = new StringBuffer();

        writeHierarchy(uri, prefix, key, hierarchy);
        if (highlightTimestamp > 0 && expansionText.length() == 0) {
            // if we were asked to highlight, but no matching sections were
            // found, clear the highlight request and draw all sections.
            highlightTimestamp = -1;
            writeHierarchy(uri, prefix, key, hierarchy);
        }

        if (highlightTimestamp <= 0)
            writeHierarchyExpanders(expansionText);
        expansionText = null;

        out.println("</form><script src='/data.js'>" +
            "document.writeln('<p>Problem loading the script!');" +
            "</script>");
        out.println("</body></html>");

        UserNotificationManager.getInstance()
                .removeNotification(getParameter("removeNotification"));
    }

    protected String getURI() {
        String script = (String) env.get("SCRIPT_NAME");
        int pos = script.indexOf(".class");
        return script.substring(0, pos) + ".shtm";
    }

    private long getHighlightTimestamp() {
        String param = (String) parameters.get("showHighlightedRows");
        if (param != null) {
            try {
                return Long.parseLong(param);
            } catch (Exception e) {}
        }
        return -1;
    }

    protected void writeHierarchy
        (String uri, String prefix, PropertyKey key, DashHierarchy hierarchy)
        throws IOException
    {
        // only display a section for this node if it appears to be
        // capable of tracking sized objects.
        String fullPath = key.path();
        if (shouldShowHierarchyNode(fullPath)) {

            String subPath, subPath_;
            if (fullPath.length() <= prefix.length()) {
                subPath = subPath_ = "";
            } else {
                subPath = fullPath.substring(prefix.length());
                subPath_ = subPath + "/";
            }
            String anchor = Integer.toString(abs(subPath.hashCode()));

            StringBuffer url = new StringBuffer();
            url.append(uri)
                .append("?SUB_PATH=").append(HTMLUtils.urlEncode(subPath))
                .append("&SUB_PATH_=").append(HTMLUtils.urlEncode(subPath_))
                .append("&ANCHOR=").append(anchor);

            boolean addExtra =
                subPath.equals(parameters.get(EXTRA_ROWS_PARAM));
            boolean hasRows =
                findAndAppendObjectNumbers(url, fullPath, addExtra);

            if (hasRows) {
                byte[] text =
                    getTinyWebServer().getRequest(url.toString(), true);
                outStream.write(text);
                outStream.flush();
            }

            // generate an expansion link for this node.
            StringBuffer text = new StringBuffer(EXPANSION_SECTION);
            StringUtils.findAndReplace(text, "ANCHOR", anchor);
            StringUtils.findAndReplace
                (text, "U-PATH", HTMLUtils.urlEncode(subPath));
            String display = subPath;
            if (display.length() == 0) display = "/";
            StringUtils.findAndReplace
                (text, "H-PATH", HTMLUtils.escapeEntities(display));
            expansionText.append(text);
        }

        // recurse and draw elements for children
        int numChildren = hierarchy.getNumChildren(key);
        for (int i = 0;   i < numChildren;   i++)
            writeHierarchy(uri, prefix, hierarchy.getChildKey(key, i),
                           hierarchy);
    }

    private boolean shouldShowHierarchyNode(String fullPath) {
        String dataName =
            DataRepository.createDataName(fullPath, OBJ_LIST_NAME);
        if (getDataRepository().getValue(dataName) == null)
            return false;

        if (highlightTimestamp > 0)
            return hasValidHighlight(fullPath);
        else
            return true;
    }

    private boolean hasValidHighlight(String fullPath) {
        String dataName = fullPath + HIGHLIGHT_FLAG_SUFFIX;
        SimpleData pathTimestamp = getDataRepository().getSimpleValue(dataName);
        if (pathTimestamp instanceof DateData) {
            DateData date = (DateData) pathTimestamp;
            return date.getValue().getTime() >= highlightTimestamp;
        } else {
            return false;
        }
    }

    protected void writeHierarchyExpanders(StringBuffer expansionText)
        throws IOException
    {
        if (parameters.containsKey("EXPORT")) return;

        out.print("<div class='doNotPrint'><hr>");
        out.print("<p>To add new objects to the size inventory, click on ");
        out.print("the links below.  Click the link that corresponds to ");
        out.print("the WBS item which is most appropriate for the object ");
        out.print("you wish to add.</p><p>");
        out.print(expansionText.toString());
        out.print("</div>");
    }



    protected boolean findAndAppendObjectNumbers
        (StringBuffer url, String fullPath, boolean addExtraRows)
    {
        boolean addedNumber = false;

        DataRepository data = getDataRepository();
        int rowNum, lastPopulatedRow, i;
        rowNum = lastPopulatedRow = -1;
    ROW:
        while (true) {
            rowNum++;
            for (i = dataElems.length; i-- > 0; ) {
                String dataName = StringUtils.findAndReplace
                    (dataElems[i], "NUM", String.valueOf(rowNum));
                dataName = DataRepository.createDataName(fullPath, dataName);
                if (data.getValue(dataName) != null) {
                    lastPopulatedRow = rowNum;
                    url.append("&n=").append(String.valueOf(rowNum));
                    addedNumber = true;

                    if (highlightTimestamp > 0) {
                        String rowPrefix = DataRepository.chopPath(dataName);
                        if (hasValidHighlight(rowPrefix))
                            url.append("&highlight_").append(
                                String.valueOf(rowNum));
                    }

                    continue ROW;
                }
            }
            // if we haven't seen any data for 20 consecutive rows,
            // we can safely conclude that there is no more data.
            if (rowNum - lastPopulatedRow > 20)
                break ROW;
        }

        if (addExtraRows) {
            addedNumber = true;
            for (i = NUM_EXTRA_ROWS;   i-- > 0; )
                url.append("&n=").append(String.valueOf(++lastPopulatedRow));
        }

        return addedNumber;
    }

    private static final int abs(int x) {
        return (x > 0 ? x : 0 - x);
    }

    private static final String HTML_HEADER =
        "<html><head>\n"
            + "<link rel=stylesheet type='text/css' href='style.css'>\n"
            + "<title>Size Inventory</title>\n"
            + "<style>\n"
            + ".node { font-size: larger; font-weight: bold }\n"
            + ".header { font-weight: bold }\n"
            + "td.spacer { width: 1in }\n"
            + ".highlight td { background-color: #bfb }\n"
            + "@media print { .doNotPrint { display: none } }\n"
            + "</style>\n"
            + "</head><body>\n"
            + "<h1>Size Inventory</h1>\n";

    private static final String OBJ_LIST_NAME = "Local_Sized_Object_List//All";

    private static final String HIGHLIGHT_FLAG_SUFFIX = "//Show_Highlight";

    private static final String [] dataElems = {
        "Sized_Objects/NUM/Description",
        "Sized_Objects/NUM/Sized_Object_Units",
        "Sized_Objects/NUM/Estimated Size",
        "Sized_Objects/NUM/Size" };

    private static final String EXPANSION_SECTION =
        "<a href=\"sizeForm.class?"+EXTRA_ROWS_PARAM+"=U-PATH#ANCHOR\">" +
        "H-PATH</a><br>\n";

}
