// Copyright (C) 2006 Tuma Solutions, LLC
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
import java.io.Writer;
import java.util.Map;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.net.cms.AddNewItemPageAssembler;
import net.sourceforge.processdash.util.XMLUtils;

public class MetricsTableColumn {

    public interface SpecialDataElement {

        String getDataName(MetricsTableColumn column);

    }

    private String paramName;

    private String resourceKey;

    private boolean checkedByDefault;

    private String dataPrefix;

    private String dataSuffix;


    public MetricsTableColumn(String paramName, String resourceKey,
            boolean checkedByDefault, String dataPrefix, String dataSuffix) {
        this.paramName = paramName;
        this.resourceKey = resourceKey;
        this.checkedByDefault = checkedByDefault;
        this.dataPrefix = dataPrefix;
        this.dataSuffix = dataSuffix;
    }

    public void writeCheckbox(Writer out, Resources resources, Map parameters)
            throws IOException {
        out.write("<input type=\"checkbox\" class=\"cmsIndent\" name=\"$$$_");
        out.write(paramName);
        out.write("\"");
        if (getBool(parameters, paramName)
                || (checkedByDefault && getBool(parameters,
                        AddNewItemPageAssembler.DEFAULTS_PARAM))) {
            out.write(" checked=\"true\"");
        }
        out.write("/>&nbsp;");
        out.write(resources.getHTML(resourceKey));
        out.write("\n");
    }

    public boolean isShowing(Map parameters) {
        return getBool(parameters, paramName);
    }

    public void writeHeader(Writer out, Resources resources, boolean pad)
            throws IOException {
        out.write("<th");
        if (pad)
            out.write(PADDING_LEFT);
        out.write(">");
        out.write(resources.getHTML(resourceKey));
        out.write("</th>\n");
    }

    public void writeCell(Writer out, DataContext data, Object baseDataName,
            boolean pad) throws IOException {
        out.write("<td");
        if (pad)
            out.write(PADDING_LEFT);
        out.write(">");
        String dataName = getDataName(baseDataName);
        if (dataName != null && data.getValue(dataName) != null) {
            out.write("<input type=\"text\" size=\"7\" name=\"");
            out.write(XMLUtils.escapeAttribute(dataName));
            out.write("\"/>");
        }
        out.write("</td>\n");
    }

    public String getDataName(Object dataName) {
        if (dataName instanceof SpecialDataElement)
            return getDataName((SpecialDataElement) dataName);
        else
            return getDataName(dataName.toString());
    }

    public String getDataName(SpecialDataElement e) {
        return e.getDataName(this);
    }

    public String getDataName(String baseDataName) {
        String metricPrefix = "";
        String metricName = baseDataName;
        int slashPos = baseDataName.lastIndexOf('/');
        if (slashPos != -1) {
            metricPrefix = baseDataName.substring(0, slashPos + 1);
            metricName = baseDataName.substring(slashPos + 1);
        }

        if (metricName.indexOf(dataPrefix) == -1)
            metricName = dataPrefix + metricName;
        if (metricName.indexOf(dataSuffix) == -1)
            metricName = metricName + dataSuffix;

        return metricPrefix + metricName;
    }

    private static boolean getBool(Map parameters, String paramName) {
        Object param = parameters.get(paramName);
        return (param != null && !"".equals(param));
    }

    static final MetricsTableColumn PLAN = new MetricsTableColumn(
            "ShowPlanCol", "Plan", true, "Estimated ", "");

    static final MetricsTableColumn ACTUAL = new MetricsTableColumn(
            "ShowActualCol", "Actual", true, "", "");

    static final MetricsTableColumn ACTUAL_PCT = new MetricsTableColumn(
            "ShowActualPctCol", "Actual_%", false, "%/", "");

    static final MetricsTableColumn TO_DATE = new MetricsTableColumn(
            "ShowToDateCol", "To_Date", true, "", " To Date");

    static final MetricsTableColumn TO_DATE_PCT = new MetricsTableColumn(
            "ShowToDatePctCol", "To_Date_%", true, "%/", " To Date");

    static final String PADDING_LEFT = " style='padding-left: 10px'";
}
