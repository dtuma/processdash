// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
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

package net.sourceforge.processdash.ui.web.reports.analysis;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.SimpleData;
import net.sourceforge.processdash.data.StringData;
import net.sourceforge.processdash.data.compiler.Compiler;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;


public abstract class AnalysisPage extends TinyCGIBase implements StringMapper {

    protected static Resources resources =
        Resources.getTemplateBundle("reports/reports");


    protected static final String LINE_CHART = "line";
    protected static final String XY_CHART   = "xy";
    protected static final String PIE_CHART  = "pie";


    protected static final String PAGE_HEADER = "<html><head>\n"
        + "<link rel=stylesheet type=\"text/css\" href=\"/style.css\">\n"
        + "<title>%TITLE%</title>\n"
        + "</head><body>\n"
        + "<h1>%PATH%</h1>\n"
        + "<table><tr>\n"
        + "<td VALIGN=\"BASELINE\"><h2>%TITLE%&nbsp;</h2></td>\n"
        + "<td VALIGN=\"BASELINE\"><i>${More_Detail_Instruction}</i></td>\n"
        + "</tr></table>\n";


    protected Map argGeneratorMap = new HashMap();



    protected void writeHeader() {
        if (parameters.containsKey("type")) {
            out.print("Content-type: text/plain; charset="+charset+"\r\n\r\n");
            out.flush();
        } else {
            super.writeHeader();
        }
    }

    protected void writeContents() throws IOException {
        try {
            Method m = null;
            String chartKey = getParameter("type");
            if (chartKey != null) {
                m = (Method) argGeneratorMap.get(chartKey);
                if (m == null) {
                    m = getClass().getMethod("write" + chartKey + "Args", null);
                    argGeneratorMap.put(chartKey, m);
                }
            }

            if (m == null)
                writeHTML();
            else
                m.invoke(this, null);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Internal Error");
        }
    }

    protected abstract void writeHTML() throws IOException;

    protected void writeHTMLHeader(String titleKey) throws IOException {
        // write the HTML header
        String header = resources.interpolate
            (PAGE_HEADER, HTMLUtils.ESC_ENTITIES);
        header = StringUtils.findAndReplace
            (header, "%PATH%",
             HTMLUtils.escapeEntities(localizePrefix(getPrefix())));
        header = StringUtils.findAndReplace
            (header, "%TITLE%", resources.getString(titleKey));

        out.write(header);
    }


    public static String localizePrefix(String prefix) {
        if (prefix.startsWith("/To Date/") && prefix.endsWith("/All")) {
            prefix = prefix.substring(9, prefix.length() - 4);
            return resources.format("To_Date_Prefix_FMT", prefix);
        } else
            return prefix;
    }

    protected String escData(String str) {
        return Compiler.escapeLiteral(str);
    }

    protected ListData getProcessList(String listName) {
        String dataName = DataRepository.createDataName(getPrefix(), listName);
        SimpleData val = getDataRepository().getSimpleValue(dataName);
        if (val instanceof ListData)
            return (ListData) val;
        else if (val instanceof StringData)
            return ((StringData) val).asList();
        else
            return new ListData();
    }

    protected List getProcessListPlain(String name) {
        ListData list = getProcessList(name);
        List result = new LinkedList();
        for (int i = 0;  i < list.size();   i++)
            result.add(list.get(i));
        return result;
    }

    protected String getProcessString(String stringName) {
        String dataName = DataRepository.createDataName(getPrefix(), stringName);
        SimpleData val = getDataRepository().getSimpleValue(dataName);
        return val == null ? "" : val.format();
    }

    protected boolean metricIsDefined(String name) {
        String dataName = DataRepository.createDataName(getPrefix(), name);
        return getDataRepository().getValue(dataName) != null;
    }

    protected String interpolate(String text) {
        return StringUtils.interpolate(this, text);
    }
    protected void interpOut(String text) {
        out.write(interpolate(text));
    }

    protected String fmtArg(String name, Object value) {
        return name + "=" + HTMLUtils.urlEncode((String) value);
    }

    protected void writeChartHTML(String chartType, String chartKey) {
        writeChartHTML(chartType, chartKey, null);
    }

    protected void writeChartHTML(String chartType, String chartKey, String extraArgs) {
        String script = (String) env.get("SCRIPT_NAME");
        int pos = script.lastIndexOf('/');
        script = script.substring(pos+1) + "?type=" + chartKey;
        if (extraArgs != null)
            script = script + "&" + extraArgs;
        String args = "qf=" + HTMLUtils.urlEncode(script);

        String fullURL = null;
        if (parameters.containsKey("EXPORT"))
            fullURL = "table.class";
        else {
            fullURL = "full.htm";
            args = args + "&chart=" + chartType;
        }

        out.write("<a href=\"");
        out.write(fullURL);
        out.write("?");
        out.write(args);
        out.write("\"><img src=\"");
        out.write(chartType);
        out.write(".class?");
        out.write(args);
        out.write("&qf=small.rpt\"></a>\n");
    }

    protected void writeSimpleChartArgs(String title, String label, String comments, String dataElem) {
        out.println("qf=compProj.rpt");
        out.print("title=");
        out.println(interpolate(title));
        out.print("d1=");
        out.println(dataElem);

        if (label != null) {
            out.print("h1=");
            out.println(interpolate(label));
        }
        if (comments != null) {
            out.print("headerComment=");
            out.println(interpolate(comments));
        }
    }

    public String getString(String key) {
        boolean translate = false;
        boolean esc = false;
        String result = null;

        if (key.startsWith("tr:")) {
            translate = true;
            key = key.substring(3);
        } else if (key.startsWith("esc:")) {
            esc = true;
            key = key.substring(4);
        }

        if (parameters.containsKey(key))
            result = getParameter(key);
        else
            result = resources.getString(key);

        if (result != null) {
            if (translate)
                result = Translator.translate(result);
            else if (esc)
                result = escData(result);
        }

        return result;
    }

    protected void writeSimpleXYChartArgs(String title, String labelX, String dataX, String labelY, String dataY) {
        out.println("qf=compProj.rpt");
        out.print("title=");
        out.println(interpolate(title));
        out.print("d1=");
        out.println(dataX);
        out.print("d2=");
        out.println(dataY);

        if (labelX != null) {
            out.print("h1=");
            out.println(interpolate(labelX));
        }
        if (labelY != null) {
            out.print("h2=");
            out.println(interpolate(labelY));
        }
    }

    protected String getSizeAbbrLabel() {
        String sizeMetric = getProcessString("SIZE_METRIC_NAME_ABBR");
        String displayName = Translator.translate(sizeMetric);
        return displayName;
    }

    protected String getCumPhaseSum(ListData phases, String thruPhase, String dataElem) {
        StringBuffer result = new StringBuffer();
        result.append("sumFor(\"").append(escData(dataElem)).append("\", \"\u0002");
        for (int i = 0;   i < phases.size();   i++) {
            String phase = (String) phases.get(i);
            result.append(escData(phase)).append('\u0002');
            if (phase.equals(thruPhase))
                break;
        }
        result.append("\")");
        return result.toString();
    }

    protected String getCumPhaseSumPct(ListData phases, String thruPhase, String dataElem) {
        StringBuffer result = new StringBuffer();
        result.append(getCumPhaseSum(phases, thruPhase, dataElem))
            .append(" / [").append(escData(dataElem)).append("]");
        return result.toString();
    }

    protected String getAggrSizeLabel() {
        return Translator.translate
            (getProcessString("AGGR_SIZE_METRIC_NAME_ABBR"));
    }

}
