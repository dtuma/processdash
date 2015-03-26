// Copyright (C) 2002-2014 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.probe.wizard;

import java.util.Date;

import net.sourceforge.processdash.data.DateData;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.util.ResultSet;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.util.StringUtils;


public class HistDataPage extends WizardPage {


    public static final String SUBSET_PREFIX_PARAM = "subsetPrefix";
    private static final String TASK_FIELD = "TaskName";
    private static final String EXCLUDE_FIELD = "Exclude";


    public void writeHTMLContents() {
        writeStepTitle(resources.getString("HistData.Title"));

        // TODO: support some sort of subset prefix parameter (instead of the
        // hardcoded null in the line below)?
        ProbeData histData = new ProbeData(data, prefix, null);
        printDataTable(histData, true);

        out.print("<p>");
        out.print(resources.getHTML("HistData.Footer"));
        out.println("</p>");
    }


    public boolean parseFormData() {
        // From the posted data, create the effective list of projects.
        ListData probeList = new ListData();
        int r = 1;
        while (true) {
            String taskName = (String) params.get(TASK_FIELD+r);
            if (taskName == null) break;
            if (params.get(EXCLUDE_FIELD+r) == null)
                probeList.add(taskName);
            r++;
        }

        // save that list to the PROBE_SUBSET for this project.
        putValue(ProbeData.PROBE_LIST_NAME, probeList);
        data.waitForCalculations();
        return true;
    }


    public boolean writeReportSection() {
        writeSectionTitle(resources.getString("HistData.Short_Title"));
        ProbeData histData = new ProbeData(data, prefix, null);
        histData.setReportMode(true);
        Date cutoffDate = DateData.valueOf(getValue("Planning/Completed"));
        if (cutoffDate == null)
            cutoffDate = DateData.valueOf(getValue("Completed"));
        if (cutoffDate != null)
            histData.discardExcludedProjectsOnOrAfter(cutoffDate);
        printDataTable(histData, false);
        return true;
    }



    private void printDataTable(ProbeData histData, boolean full) {
        ResultSet resultSet = histData.getResultSet();
        int numRows = resultSet.numRows();

        if (numRows == 0) {
            out.print("<p>");
            out.print(resources.getHTML("HistData.No_Data_Message"));
            out.println("</p>");
            return;
        }

        if (full) {
            out.print("<p>");
            out.print(resources.getString("HistData.Data_Table_Header_HTML"));
            out.print("</p>");
        }

        String timeFootnote = getTimeFootnote(histData);
        out.print("<table id='histData' border style='margin-left:1cm'><tr><th>");
        out.print(resources.getHTML("Project_Task"));
        for (int c = 1;  c <= ProbeData.EXCLUDE;   c++) {
            out.print("</th><th>");
            String colName = resultSet.getColName(c);
            String displayName;
            try {
                String key = "HistData.Columns."+colName.replace(' ', '_');
                displayName = resources.getString(key);
            } catch (Exception e) {
                displayName = Translator.translate(colName);
            }
            out.print(esc(displayName));
            if (c == ProbeData.EST_TIME || c == ProbeData.ACT_TIME)
                out.print(timeFootnote);
            else if (c == ProbeData.EXCLUDE && !histData.isReportMode())
                out.print(EXCLUDE_ALL_CHECKBOX);
        }
        out.println("</th></tr>");

        for (int r = 1;   r <= resultSet.numRows();   r++) {
            out.print("<tr>");
            out.print("<td nowrap>");
            out.print(esc(resultSet.getRowName(r)));
            for (int c = 1;   c < ProbeData.EXCLUDE;   c++) {
                out.print("</td><td align=center>");
                out.print(resultSet.format(r, c));
            }
            out.println("<td align=center>");
            if (histData.isReportMode()) {
                if (resultSet.getData(r, ProbeData.EXCLUDE) != null)
                    out.print("<b style='font-family: sans-serif'>X</b>");
                else
                    out.print("&nbsp;");
            } else {
                printField(TASK_FIELD+r, histData.getRowId(r));
                out.println("<input type=checkbox name='"+EXCLUDE_FIELD+r+"'");
                if (resultSet.getData(r, ProbeData.EXCLUDE) != null)
                    out.print(" checked");
                out.print(">");
            }
            out.println("</td></tr>");
        }
        out.print("</table>\n");

        if (full) {
            out.print("<p style='margin-left:1cm'><font size=-1><i>");
            out.print(Tutorial.annotate(resources.getString
                                        ("HistData.Data_Table_Footer_HTML")));
            out.print("</i></font></p>");
        }
    }

    private String getTimeFootnote(ProbeData histData) {
        String workflowName = histData.getDatabaseWorkflowName();
        if (!StringUtils.hasValue(workflowName))
            return "";

        String tooltip = resources.format(
            "HistData.Columns.Workflow_Hours_Tooltip_FMT", workflowName);
        return "<span title='" + esc(tooltip) + "' class='doNotPrint'>*</span>";
    }

    private static final String EXCLUDE_ALL_CHECKBOX = "<br><input "
            + "type='checkbox' name='excludeAll' id='excludeAll' "
            + "onclick='toggleAll(this); return true;' />\n"
            + "<script type='text/javascript'>\n"
            + "function toggleAll(checkbox) {\n"
            + "   var checked = checkbox.checked;\n"
            + "   var table = document.getElementById('histData');\n"
            + "   var fields = table.getElementsByTagName('input');\n"
            + "   for (var i = 0; i < fields.length; i++) {\n"
            + "      if (fields[i].type == 'checkbox')\n"
            + "         fields[i].checked = checked;\n"
            + "    }\n"
            + "}\n"
            + "</script>";

}
