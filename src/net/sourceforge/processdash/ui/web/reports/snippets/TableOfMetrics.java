// Copyright (C) 2006-2024 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.snippets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cms.AbstractViewPageAssembler;
import net.sourceforge.processdash.net.cms.SnippetDataEnumerator;
import net.sourceforge.processdash.net.cms.TranslatingAutocompleter;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;

public class TableOfMetrics extends TinyCGIBase {

    private static final String DISPLAY_NAME_ATTR = "DisplayName";

    private static final String DATA_NAME_ATTR = "DataName";

    private static final String ITEM_TYPE = "Metric";

    private static final String HEADING_PARAM = "Heading";

    private static final String LABEL_PARAM = "Label";

    private static final String MERGE_PARAM = "MergePriorTable";

    private static final Resources resources = Resources
            .getDashBundle("Analysis.MetricsTable");

    protected void writeContents() throws IOException {
        if (parameters.containsKey("listMetrics"))
            out.write(StringUtils.join(getListOfMetrics(), "\n"));
        else
            writeTable();
    }

    private Collection getListOfMetrics() {
        Set items = new HashSet();
        DataNameCollector.run(getDataRepository(), getPrefix(), true, true,
                false, items);
        items.addAll(SPECIAL_DATA.keySet());
        return items;
    }


    private void writeTable() throws IOException {
        DataContext dataContext = getDataContext();

        // retrieve the heading and label the user wants displayed
        String heading = getParameter(HEADING_PARAM);
        String label = getParameter(LABEL_PARAM);

        // retrieve the list of columns the user wants to display
        List columns = new ArrayList();
        for (int i = 0; i < COLUMNS.length; i++) {
            if (COLUMNS[i].isShowing(parameters))
                columns.add(COLUMNS[i]);
        }
        if (dataContext.getSimpleValue("Rollup Tag") != null)
            columns.remove(MetricsTableColumn.TO_DATE);
        if (columns.isEmpty()) {
            out.write("<!-- no columns selected;  no table to display -->\n\n");
            return;
        }

        // retrieve the list of metrics the user wants to display
        Map[] metrics = SnippetDataEnumerator.getEnumeratedValues(parameters,
                ITEM_TYPE);
        if (metrics == null) {
            out.write("<!-- no metrics selected;  no table to display -->\n\n");
            return;
        }

        // write out heading if requested
        if (StringUtils.hasValue(heading)) {
            out.write("<h2>");
            out.write(HTMLUtils.escapeEntities(heading));
            out.write("</h2>\n\n");
        }

        String mergeTables = getParameter(MERGE_PARAM);
        if (mergeTables != null) {
            out.write(AbstractViewPageAssembler.MERGE_TABLES_DIRECTIVE);
            out.write(mergeTables);
            out.write(" -->");
        }

        // write the header row of the table
        out.write("<p><table id=\"$$$_data\"><tr><th align=\"left\">");
        if (label != null)
            out.write(HTMLUtils.escapeEntities(label));
        out.write("</th>\n");
        boolean pad = true;
        for (Iterator i = columns.iterator(); i.hasNext();) {
            MetricsTableColumn col = (MetricsTableColumn) i.next();
            col.writeHeader(out, resources, pad);
            pad = false;
        }
        out.write("</tr>\n");

        if (mergeTables != null)
            out.write(AbstractViewPageAssembler.MERGE_TABLES_CUT_MARK);

        // write a table row for each metric
        for (int i = 0; i < metrics.length; i++)
            writeTableRow(metrics[i], columns, dataContext);

        out.write("</table></p>\n\n");
    }

    private void writeTableRow(Map metric, List columns, DataContext data) throws IOException {
        String dataName = (String) metric.get(DATA_NAME_ATTR);
        if (dataName == null || dataName.length() == 0)
            return;

        String display = (String) metric.get(DISPLAY_NAME_ATTR);
        if (display == null || display.length() == 0)
            display = TranslatingAutocompleter.translateDataName(dataName);

        Object dataElem = lookupSpecialDataElement(dataName);

        out.write("<tr><td");
        int indentLevel = countInitialSpaces(display);
        if (indentLevel > 0)
            out.write(" style='padding-left: " + indentLevel + "em'");
        out.write(">");
        display = display.trim();
        boolean usedSpan = false;
        int bracePos = display.indexOf('{');
        if (bracePos > 0 && display.endsWith("}")) {
            String attrs = display.substring(bracePos+1, display.length()-1);
            display = display.substring(0, bracePos);
            out.write("<span ");
            out.write(attrs);
            out.write(">");
            usedSpan = true;
        }
        out.write(HTMLUtils.escapeEntities(display.trim()));
        if (usedSpan)
            out.write("</span>");
        out.write("</td>\n");

        boolean pad = true;
        for (Iterator i = columns.iterator(); i.hasNext();) {
            MetricsTableColumn col = (MetricsTableColumn) i.next();
            col.writeCell(out, data, dataElem, pad);
            pad = false;
        }

        out.write("</tr>\n");
    }

    private int countInitialSpaces(String text) {
        Matcher m = INITIAL_WHITESPACE.matcher(text);
        if (m.find())
            return m.group().length();
        else
            return 0;
    }
    private static final Pattern INITIAL_WHITESPACE = Pattern.compile("^\\s+");

    private Object lookupSpecialDataElement(String dataName) {
        Object result = SPECIAL_DATA.get(dataName);
        if (result != null)
            return result;

        for (int i = 0; i < DataNameCollector.PROBE_DATA.length; i++)
            if (dataName.endsWith(DataNameCollector.PROBE_DATA[i]))
                return new EstOnlyMetric(dataName);

        return dataName;
    }

    private static class EstOnlyMetric implements
            MetricsTableColumn.SpecialDataElement {
        private String dataName;
        public EstOnlyMetric(String dataName) { this.dataName = dataName; }
        public String getDataName(MetricsTableColumn column) {
            if (column == MetricsTableColumn.PLAN)
                return dataName;
            else
                return null;
        }
    }

    private static final MetricsTableColumn[] COLUMNS = {
            MetricsTableColumn.PLAN,
            MetricsTableColumn.ACTUAL,
            MetricsTableColumn.TO_DATE, };

    private static final Map SPECIAL_DATA = new HashMap();

    static {
        SPECIAL_DATA.put("Estimated Time", new MetricsTableColumn.SpecialDataElement() {
            public String getDataName(MetricsTableColumn column) {
                if (column == MetricsTableColumn.PLAN)
                    return "Estimated Time";
                else if (column == MetricsTableColumn.TO_DATE)
                    return "Estimated Time To Date";
                else
                    return null;
            }});

        SPECIAL_DATA.put("Actual Time", new MetricsTableColumn.SpecialDataElement() {
            public String getDataName(MetricsTableColumn column) {
                if (column == MetricsTableColumn.ACTUAL)
                    return "Time";
                else if (column == MetricsTableColumn.TO_DATE)
                    return "Time To Date";
                else
                    return null;
            }});

        SPECIAL_DATA.put("CPI To Date", new MetricsTableColumn.SpecialDataElement() {
            public String getDataName(MetricsTableColumn column) {
                if (column == MetricsTableColumn.TO_DATE)
                    return "CPI To Date";
                else
                    return null;
            }});
    }
}
