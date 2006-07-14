// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.net.cms.AutocompletingListEditor;
import net.sourceforge.processdash.net.cms.SnippetDataEnumerator;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class TableOfMetrics extends TinyCGIBase {

    private static final String DISPLAY_NAME_ATTR = "DisplayName";

    private static final String DATA_NAME_ATTR = "DataName";

    private static final String ITEM_TYPE = "Metric";

    private static final String LABEL_PARAM = "Label";

    private static final Resources resources = Resources
            .getDashBundle("Analysis.MetricsTable");

    protected void writeContents() throws IOException {
        String mode = getParameter("mode");
        if ("edit".equalsIgnoreCase(mode))
            writeEditor();
        else
            writeTable();
    }

    private void writeEditor() throws IOException {
        // write a field to let the user give the table a label.
        out.write("<b>");
        out.write(resources.getHTML("Label_Prompt"));
        out.write("</b>&nbsp;<input type=\"text\" name='$$$_Label' value=\"");
        out.write(XMLUtils.escapeAttribute(getParameter(LABEL_PARAM)));
        out.write("\" size=\"50\"/></p>\n\n");

        // write checkboxes allowing the user to select the columns to show
        out.write("<p><b>");
        out.write(resources.getHTML("Columns_Prompt"));
        out.write("</b><br/>");
        for (int i = 0; i < COLUMNS.length; i++)
            COLUMNS[i].writeCheckbox(out, resources, parameters);
        out.write("</p>\n\n");

        // write a section allowing the user to select the metrics to display
        out.write("<b>");
        out.write(resources.getHTML("Metrics_Prompt"));
        out.write("</b><div class='cmsIndent'>");
        AutocompletingListEditor.writeEditor(out, getTinyWebServer(),
                "/dash/snippets/metricSimple.shtm", "$$$_", ITEM_TYPE,
                DATA_NAME_ATTR, DISPLAY_NAME_ATTR, parameters, null,
                getListOfMetrics(), resources.getHTML("Add_Metric_Prompt"));
        out.write("</div>");
    }

    private Collection getListOfMetrics() {
        List working = new ArrayList();
        DataNameCollector.run(getDataRepository(), getPrefix(), null, null,
                true, true, working);

        Set items = new HashSet(working.size());
        for (Iterator i = working.iterator(); i.hasNext();) {
            String dataName = (String) i.next();
            items.add(REMOVE_QUALIFIERS.matcher(dataName).replaceAll(""));
        }
        items.addAll(SPECIAL_DATA.keySet());
        return items;
    }

    private static final String[] PROBE_DATA = { "/Beta0", "/Beta1", "/Range",
            "/Interval Percent", "/R Squared", "/LPI", "/UPI" };

    private static final Pattern REMOVE_QUALIFIERS = Pattern
            .compile("(Estimated (?!.*(" + getProbeStrings() + ")$)| To Date)");


    private void writeTable() throws IOException {
        DataContext dataContext = getDataContext();

        // retrieve the label the user wants displayed
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

        // write the header row of the table
        out.write("<table><tr><th align=\"left\">");
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

        // write a table row for each metric
        for (int i = 0; i < metrics.length; i++)
            writeTableRow(metrics[i], columns, dataContext);

        out.write("</table>\n\n");
    }

    private void writeTableRow(Map metric, List columns, DataContext data) throws IOException {
        String display = (String) metric.get(DISPLAY_NAME_ATTR);
        String dataName = (String) metric.get(DATA_NAME_ATTR);
        if (dataName == null || dataName.length() == 0)
            return;
        if (display == null || display.length() == 0)
            display = Translator.translate(dataName);
        Object dataElem = lookupSpecialDataElement(dataName);

        out.write("<tr><td>");
        out.write(HTMLUtils.escapeEntities(display));
        out.write("</td>\n");

        boolean pad = true;
        for (Iterator i = columns.iterator(); i.hasNext();) {
            MetricsTableColumn col = (MetricsTableColumn) i.next();
            col.writeCell(out, data, dataElem, pad);
            pad = false;
        }

        out.write("</tr>\n");
    }

    private Object lookupSpecialDataElement(String dataName) {
        Object result = SPECIAL_DATA.get(dataName);
        if (result != null)
            return result;

        for (int i = 0; i < PROBE_DATA.length; i++)
            if (dataName.endsWith(PROBE_DATA[i]))
                return new EstOnlyMetric(dataName);

        return dataName;
    }

    private static String getProbeStrings() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < PROBE_DATA.length; i++)
             result.append('|').append(PROBE_DATA[i]);
        return result.substring(1);
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
    }
}
