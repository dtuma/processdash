// Copyright (C) 2003-2008 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.reports.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.processdash.data.ListData;
import net.sourceforge.processdash.data.compiler.Compiler;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.i18n.Translator;
import net.sourceforge.processdash.process.ProcessUtil;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.HTMLUtils;
import net.sourceforge.processdash.util.StringMapper;
import net.sourceforge.processdash.util.StringUtils;


public abstract class AnalysisPage extends TinyCGIBase implements StringMapper {

    protected static Resources resources = Resources.getDashBundle("Analysis");


    protected static final String LINE_CHART = "line";
    protected static final String XY_CHART   = "xy";
    protected static final String PIE_CHART  = "pie";
    protected static final String PATH_TO_REPORTS = "../";


    protected static final String PAGE_HEADER = "<html><head>\n"
        + "<link rel=stylesheet type=\"text/css\" href=\"/style.css\">\n"
        + "<script type='text/javascript' src='/lib/overlib.js'></script>\n"
        + "<title>%TITLE%</title>\n"
        + "</head><body>\n"
        + "<h1>%PATH%</h1>\n"
        + "<table><tr>\n"
        + "<td VALIGN=\"BASELINE\"><h2>%TITLE%&nbsp;</h2></td>\n"
        + "<td VALIGN=\"BASELINE\"><i>${More_Detail_Instruction}</i></td>\n"
        + "</tr></table>\n";


    protected Map argGeneratorMap = new HashMap();
    private ProcessUtil processUtil = null;


    public void service(InputStream in, OutputStream out, Map env)
            throws IOException {
        processUtil = null;
        super.service(in, out, env);
    }

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
                    m = getClass().getMethod("write" + chartKey + "Args", (Class[]) null);
                    argGeneratorMap.put(chartKey, m);
                }
            }

            if (m == null)
                writeHTML();
            else
                m.invoke(this, (Object[]) null);

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
        if (prefix != null && prefix.startsWith("/To Date/")
                && prefix.endsWith("/All")) {
            prefix = prefix.substring(9, prefix.length() - 4);
            return resources.format("To_Date_Prefix_FMT", prefix);
        } else
            return prefix;
    }

    protected String escData(String str) {
        return Compiler.escapeLiteral(str);
    }

    protected ProcessUtil getProcessUtil() {
        if (processUtil == null)
            processUtil = new ProcessUtil(getDataContext());
        return processUtil;
    }

    protected String getDefectLogParam() {
        String defectLogParam = (String) env.get("QUERY_STRING");
        if (defectLogParam == null)
            defectLogParam = "";
        else if (PATH_TO_REPORTS.length() > 0)
            defectLogParam = StringUtils.findAndReplace
                (defectLogParam, "qf="+PATH_TO_REPORTS, "qf=");
        defectLogParam = StringUtils.findAndReplace
            (defectLogParam, "&"+INCLUDABLE_PARAM, "");
        return defectLogParam;
    }

    protected ListData getProcessList(String listName) {
        return getProcessUtil().getProcessList(listName);
    }

    protected List getProcessListPlain(String name) {
        return getProcessUtil().getProcessListPlain(name);
    }

    protected String getProcessString(String stringName) {
        return getProcessUtil().getProcessString(stringName);
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

    protected void printRes(String txt) {
        out.println(resources.interpolate(txt, HTMLUtils.ESC_ENTITIES));
    }

    protected String fmtArg(String name, Object value) {
        return name + "=" + HTMLUtils.urlEncode((String) value);
    }

    protected void writeChartHTML(String chartType, String chartKey)
            throws IOException {
        writeChartHTML(chartType, chartKey, null);
    }

    protected void writeChartHTML(String chartType, String chartKey,
            String extraArgs) throws IOException {
        String script = (String) env.get("SCRIPT_NAME");
        int pos = script.lastIndexOf("/reports/");
        script = script.substring(pos+9) + "?type=" + chartKey;
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

        String href = PATH_TO_REPORTS + fullURL + "?" + args;

        String relImageUri = PATH_TO_REPORTS + chartType + ".class?" + args
                + "&qf=small.rpt&html&href=" + HTMLUtils.urlEncode(href);
        String absImageUri = resolveRelativeURI(relImageUri);
        String imageHtml = getRequestAsString(absImageUri);
        out.write(imageHtml);
        out.write(" \n");
    }

    protected void writeSimpleChartArgs(String title, String label, String comments, String dataElem) {
        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
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
        out.println("qf="+PATH_TO_REPORTS+"compProj.rpt");
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

    protected String getSizeMetric() {
        return getProcessUtil().getSizeMetric();
    }

    protected String getSizeAbbrLabel() {
        return getProcessUtil().getSizeAbbrLabel();
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
        return getProcessUtil().getAggrSizeLabel();
    }


    public static final String INCLUDABLE_PARAM = "includable";


    public boolean exporting() {
        return parameters.containsKey("EXPORT");
    }

}
