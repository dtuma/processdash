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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.sourceforge.processdash.data.DataContext;
import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.net.cms.AddNewItemPageAssembler;
import net.sourceforge.processdash.net.cms.AutocompletingListEditor;
import net.sourceforge.processdash.net.cms.SnippetDataEnumerator;
import net.sourceforge.processdash.net.cms.TranslatingAutocompleter;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class TableOfPhaseMetrics extends TinyCGIBase {

    private static final String DISPLAY_NAME_ATTR = "DisplayName";

    private static final String DATA_NAME_ATTR = "DataName";

    private static final String ITEM_TYPE = "Metric";

    private static final String LABEL_PARAM = "Label";

    private static final Resources resources = Resources
            .getDashBundle("Analysis.MetricsPhaseTable");

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

        // write radio buttons allowing the user to select a subset of phases
        out.write("<p><b>");
        out.write(resources.getHTML("Phases.Prompt"));
        out.write("</b><br/>");
        String selectedPhaseList = getSelectedPhaseList();
        for (int i = 0; i < PHASES.length; i++)
            writePhaseRadioButton(selectedPhaseList, PHASES[i][0]);
        out.write("<br/>\n");
        writeShowTotalCheckbox(selectedPhaseList);
        out.write("</p>\n\n");

        // write a section allowing the user to select the metrics to display
        out.write("<b>");
        out.write(resources.getHTML("Metrics_Prompt"));
        out.write("</b><div class='cmsIndent'>");
        AutocompletingListEditor.writeEditor(out, getTinyWebServer(),
                "/dash/snippets/metricSimple.shtm", "$$$_", ITEM_TYPE,
                DATA_NAME_ATTR, null, parameters, null,
                getListOfMetrics(), resources.getHTML("Add_Metric_Prompt"));
        out.write("</div>");
    }

    private void writePhaseRadioButton(String selectedPhaseList, String phaseID) {
        out.write("<input type=\"radio\" name=\"$$$_PhaseRows\" value=\"");
        out.write(phaseID);
        if (phaseID.equals(selectedPhaseList))
            out.write("\" checked=\"true");
        out.write("\" onclick=\"$('$$$_ShowTotalRow').disabled=");
        out.write(ALL_PHASES.equals(phaseID) ? "false" : "true");
        out.write(";\" class=\"cmsIndent\">&nbsp;");
        out.write(resources.getHTML("Phases." + phaseID));
        out.write("\n");
    }

    private void writeShowTotalCheckbox(String selectedPhaseList) {
        boolean useDefaults = parameters.containsKey(
                AddNewItemPageAssembler.DEFAULTS_PARAM);
        out.write("<input type=\"checkbox\" name=\"$$$_ShowTotalRow\"");
        if (useDefaults || parameters.containsKey("ShowTotalRow"))
            out.write(" checked=\"true\"");
        if (!useDefaults && !ALL_PHASES.equals(selectedPhaseList))
            out.write(" disabled=\"true\"");
        out.write(" class=\"cmsIndent\" id=\"$$$_ShowTotalRow\">&nbsp;");
        out.write(resources.getHTML("Show_Total_Row"));
    }

    private Collection getListOfMetrics() {
        List phases = getList(PHASES[0][1]);
        String regex = "(\\Q" + StringUtils.join(phases, "\\E|\\Q")
                + "\\E)/(Estimated )?(.*)( To Date)?";

        Set result = new HashSet();
        DataNameCollector.run(getDataRepository(), getPrefix(),
                Pattern.compile(regex), "$3", true, true, result);
        return result;
    }


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
        if (dataContext.getSimpleValue("Rollup Tag") != null) {
            columns.remove(MetricsTableColumn.TO_DATE);
            columns.remove(MetricsTableColumn.TO_DATE_PCT);
        }
        if (columns.isEmpty()) {
            out.write("<!-- no columns selected;  no table to display -->\n\n");
            return;
        }

        // retrieve the list of phases the user wants to display
        String phaseList = getSelectedPhaseList();
        List phases = null;
        for (int i = 0; i < PHASES.length; i++) {
            if (PHASES[i][0].equals(phaseList)) {
                phases = getList(PHASES[i][1]);
                break;
            }
        }
        if (phases == null)
            // default to all phases
            phases = getList(PHASES[0][1]);

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
            out.write(esc(label));
        out.write("</th>\n");

        if (metrics.length > 1) {
            for (int i = 0; i < metrics.length; i++) {
                out.write("<th");
                if (i == 0) out.write(MetricsTableColumn.PADDING_LEFT);
                out.write(" colspan=\"");
                out.write(Integer.toString(columns.size()));
                out.write("\">");
                String metricName = (String) metrics[i].get(DISPLAY_NAME_ATTR);
                if (metricName == null || metricName.trim().length() == 0) {
                    String dataName = (String) metrics[i].get(DATA_NAME_ATTR);
                    metricName = TranslatingAutocompleter.translateDataName(dataName);
                }
                out.write(esc(metricName));
                out.write("</th>\n");
            }
            out.write("</tr>\n<tr><th></th>");
        }
        for (int j = 0; j < metrics.length; j++) {
            boolean pad = true;
            for (Iterator i = columns.iterator(); i.hasNext();) {
                MetricsTableColumn col = (MetricsTableColumn) i.next();
                col.writeHeader(out, resources, pad);
                pad = false;
            }
        }
        out.write("</tr>\n");

        // write a table row for each process phase
        for (Iterator i = phases.iterator(); i.hasNext();) {
            String phase = (String) i.next();
            writeTableRow(phase, metrics, columns, dataContext);
        }

        if (parameters.containsKey("ShowTotalRow")
                && ALL_PHASES.equals(phaseList))
            writeTableRow(null, metrics, columns, dataContext);

        out.write("</table>\n\n");
    }

    private void writeTableRow(String phase, Map[] metrics, List columns,
            DataContext data) throws IOException {
        out.write("<tr><td>");
        if (phase == null)
            out.write(resources.getHTML("Total"));
        else
            out.write(esc(Translator.translate(phase)));
        out.write("</td>\n");

        String[] extraAttrs = new String[columns.size()];
        Arrays.fill(extraAttrs, "");
        if (metrics.length > 1) {
            extraAttrs[0] = " ";
        }

        for (int m = 0; m < metrics.length; m++) {
            Map metric = metrics[m];
            String dataName = (String) metric.get(DATA_NAME_ATTR);
            if (phase != null)
                dataName = phase + "/" + dataName;
            boolean pad = true;
            for (Iterator i = columns.iterator(); i.hasNext();) {
                MetricsTableColumn col = (MetricsTableColumn) i.next();
                col.writeCell(out, data, dataName, pad);
                pad = false;
            }
        }
        out.write("</tr>\n");
    }

    private String getSelectedPhaseList() {
        String result = getParameter("PhaseRows");
        if (result == null)
            result = ALL_PHASES;
        return result;
    }

    private String esc(String s) {
        return HTMLUtils.escapeEntities(s);
    }

    private List getList(String name) {
        DataContext data = getDataContext();
        SimpleData phaseDataElem = data.getSimpleValue(name);
        if (phaseDataElem instanceof ListData)
            return ((ListData) phaseDataElem).asList();
        else if (phaseDataElem instanceof StringData)
            return ((StringData) phaseDataElem).asList().asList();
        return Collections.EMPTY_LIST;
    }

    private static final MetricsTableColumn[] COLUMNS = {
            MetricsTableColumn.PLAN,
            MetricsTableColumn.ACTUAL,
            MetricsTableColumn.ACTUAL_PCT,
            MetricsTableColumn.TO_DATE,
            MetricsTableColumn.TO_DATE_PCT, };

    private static final String ALL_PHASES = "All";

    private static final String[][] PHASES = {
        { ALL_PHASES, "Phase_List"},
        { "Appraisal", "Appraisal_Phase_List"},
        { "Failure", "Failure_Phase_List" },
        { "Quality", "Quality_Phase_List" },
    };

}
