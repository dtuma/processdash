// Copyright (C) 2002-2020 Tuma Solutions, LLC
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.hier.DashHierarchy;
import net.sourceforge.processdash.hier.PropertyKey;
import net.sourceforge.processdash.net.http.HTMLPreprocessor;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.team.TeamDataConstants;
import net.sourceforge.processdash.ui.UserNotificationManager;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;


public class SizeInventoryForm extends TinyCGIBase {

    private static final String EXTRA_ROWS_PARAM = "addRows";
    private static final int NUM_EXTRA_ROWS = 5;
    private boolean newSizeData, customSizeMetrics;
    private List<String> sizeMetrics;
    private Map requestEnv;
    private long highlightTimestamp;
    private StringBuffer expansionText;

    public void writeContents() throws IOException {
        String prefix = getPrefix();
        if (prefix == null) prefix = "/";

        DashHierarchy hierarchy = getPSPProperties();
        PropertyKey key = hierarchy.findExistingKey(prefix);

        requestEnv = new HashMap();
        loadProjectSizeMetadata();
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

    private void loadProjectSizeMetadata() {
        DataContext data = getDataContext();
        SimpleData flag = data.getSimpleValue(TeamDataConstants.WBS_SIZE_DATA_NAME);
        newSizeData = (flag != null && flag.test());
        flag = data.getSimpleValue(TeamDataConstants.WBS_CUSTOM_SIZE_DATA_NAME);
        customSizeMetrics = (flag != null && flag.test());

        ProcessUtil proc = new ProcessUtil(data);
        if (customSizeMetrics) {
            sizeMetrics = proc.getProcessListPlain("Size_Metric_ID_List");
            List names = proc.getProcessListPlain("Size_Metric_Name_List");
            List keys = new ArrayList();
            for (int i = 0; i < sizeMetrics.size(); i++) {
                String key = "SizeMetric_" + i;
                requestEnv.put(key + "_ID", sizeMetrics.get(i));
                requestEnv.put(key + "_Name", names.get(i));
                keys.add(key);
            }
            requestEnv.put("Size_Metric_List", keys);

        } else {
            sizeMetrics = proc.getProcessListPlain("Custom_Size_Metric_List");
            sizeMetrics.add(0, "LOC");
            sizeMetrics.add("DLD Lines");
        }
    }

    protected String getURI() {
        String script = (String) env.get("SCRIPT_NAME");
        int pos = script.indexOf(".class");
        return script.substring(0, pos) //
                + (customSizeMetrics ? "3" : (newSizeData ? "2" : "")) + ".shtm";
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
            StringBuffer rowNums = new StringBuffer();
            findAndAppendObjectNumbers(url, rowNums, fullPath, addExtra);

            if (rowNums.length() > 0) {
                Object replacements = Collections.singletonMap("#####",
                    rowNums.substring(1));
                requestEnv.put(HTMLPreprocessor.REPLACEMENTS_PARAM,
                    replacements);
                byte[] text = getTinyWebServer().getRequest(url.toString(),
                    true, requestEnv);
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
                DataRepository.createDataName(fullPath, "PSP Project");
        if (newSizeData && getDataRepository().getValue(dataName) != null)
            return true;

        dataName = DataRepository.createDataName(fullPath, OBJ_LIST_NAME);
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



    protected void findAndAppendObjectNumbers(StringBuffer url,
            StringBuffer rowNums, String fullPath, boolean addExtraRows)
    {
        // when we're using new style size data, we don't need to check for
        // object rows. We just generate a section if there are data values for
        // any of the known size metrics
        if (newSizeData) {
            url.insert(0, HTMLUtils.urlEncodePath(fullPath) + "/");
            if (addExtraRows)
                rowNums.append("tt");
            else if (hasNewSizeData(fullPath))
                rowNums.append("t");
            return;
        }

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
                    rowNums.append(",").append(String.valueOf(rowNum));

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
            for (i = NUM_EXTRA_ROWS;   i-- > 0; )
                rowNums.append(",").append(String.valueOf(++lastPopulatedRow));
        }
    }

    private boolean hasNewSizeData(String fullPath) {
        for (String metric : sizeMetrics) {
            for (String suffix : newDataSuffixes) {
                String dataName = fullPath + newDataPrefix + metric + suffix;
                SimpleData sd = getDataRepository().getSimpleValue(dataName);
                if (sd != null && sd.test())
                    return true;
            }
        }
        return false;
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

    private static final String newDataPrefix = "/Sized_Objects/";
    private static final String[] newDataSuffixes = { 
        "/Plan Size", "/Actual Size" };
    
    private static final String EXPANSION_SECTION =
        "<a href=\"sizeForm.class?"+EXTRA_ROWS_PARAM+"=U-PATH#ANCHOR\">" +
        "H-PATH</a><br>\n";

}
