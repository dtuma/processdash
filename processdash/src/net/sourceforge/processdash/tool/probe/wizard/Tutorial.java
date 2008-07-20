// Copyright (C) 2002 Tuma Solutions, LLC
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

import java.io.IOException;
import java.lang.reflect.Method;

import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.FormatUtil;
import net.sourceforge.processdash.util.HTMLUtils;


public class Tutorial extends TinyCGIBase {

    public static final String URL = "tutorial";

    /** this parameter should be bound to one of the _TUT values below */
    public static final String TUTORIAL_PARAM = "tut";

    // the following parameters should be appropriate localized strings
    public static final String PURPOSE_PARAM = "purpose";
    public static final String LETTER_PARAM = "letter";
    public static final String X_PARAM = "x";
    public static final String Y_PARAM = "y";
    public static final String INPUT_PARAM = "in";
    public static final String OUTPUT_PARAM = "out";
    public static final String PERCENT_PARAM = "pct";

    static final Resources resources = Resources.getDashBundle
        ("PROBE.Wizard.Tutorial");
    static final TutorialAnnotator annotator = new TutorialAnnotator();


    protected void writeContents() throws IOException {
        try {
            String which = getParameter(TUTORIAL_PARAM);
            String methodName = "write" + which + "Tutorial";
            Method m = Tutorial.class.getMethod(methodName, (Class[]) null);
            m.invoke(this, (Object[]) null);
        } catch (Throwable t) {
            IOException ioe = new IOException("No such tutorial");
            ioe.initCause(t);
            throw ioe;
        }
    }

    private void writeTutorialHeader(String titleText) {
        out.print("<html><head><title>");
        out.print(HTMLUtils.escapeEntities(titleText));
        out.print("</title>");
        out.println("<link rel=stylesheet type='text/css' href='style.css'>");
        out.println("<style>");
        out.println("SPAN { font-weight: bold; font-style: italic }");
        out.println("</style></head><body>");
    }

    public static final String OUTLIER_TUT = "Outlier";
    public void writeOutlierTutorial() {
        writeTutorialHeader(resources.getString("Outlier_Title"));
        out.println(resources.getString("Outlier_HTML"));
        out.println("</body></html>");
    }
    public static String getOutlierLink() {
        return getLink(URL + "?" + TUTORIAL_PARAM + "=" + OUTLIER_TUT);
    }

    public static final String REGRESS_TUT = "Regress";
    public void writeRegressTutorial() {
        writeLinearTutorial("Regress_HTML_FMT", "regress.gif");
    }
    public static String getRegressLink
        (String purpose, String letter, String x, String y,
         String in, String out, double percent)
    {
        return getLinearLink(REGRESS_TUT, purpose, letter, x, y,
                             in, out, percent);
    }

    public static final String AVERAGE_TUT = "Average";
    public void writeAverageTutorial() {
        writeLinearTutorial("Average_HTML_FMT", "average.gif");
    }
    public static String getAverageLink
        (String purpose, String letter, String x, String y,
         String in, String out)
    {
        return getLinearLink(AVERAGE_TUT, purpose, letter, x, y, in, out, 0);
    }

    private void writeLinearTutorial(String resKey, String imageHref) {
        String purpose = getParameter(PURPOSE_PARAM);
        String letter = getParameter(LETTER_PARAM);
        String methodName = resources.format("Method_FMT", letter, purpose);
        writeTutorialHeader(methodName);

        out.println("<img width=190 height=190 align=right src=\"" +
                    imageHref + "\">");
        out.println(resources.format
                    (resKey,
                     new Object[] {
                         "<b>"+methodName+"</b>",
                         "<span>" + getParameter(X_PARAM) + "</span>",
                         "<span>" + getParameter(Y_PARAM) + "</span>",
                         "<span>" + getParameter(INPUT_PARAM) + "</span>",
                         "<span>" + getParameter(OUTPUT_PARAM) + "</span>",
                         getParameter(PERCENT_PARAM)
                     }));
        out.println("</body></html>");
    }

    private static String getLinearLink
        (String tutKey, String purpose, String letter, String x, String y,
         String in, String out, double percent)
    {
        StringBuffer result = new StringBuffer();
        result.append(URL).append("?")
            .append(TUTORIAL_PARAM).append("=").append(tutKey);
        addParam(result, PURPOSE_PARAM, purpose);
        addParam(result, LETTER_PARAM, letter);
        addParam(result, X_PARAM, x);
        addParam(result, Y_PARAM, y);
        addParam(result, INPUT_PARAM, in);
        addParam(result, OUTPUT_PARAM, out);
        if (percent != 0)
            addParam(result, PERCENT_PARAM, FormatUtil.formatPercent(percent));

        return getLink(result.toString());
    }

    private static final String[] PARAM_LIST =
        { "Beta0", "Beta1", "R_Squared", "Significance" };
    public static final String PARAMS_TUT = "Params";
    public void writeParamsTutorial() {
        writeTutorialHeader(resources.getHTML("Parameters.Title"));
        out.println
            ("<p><img width=190 height=190 align=right src=\"params.gif\">");
        out.print(resources.getString("Parameters.Header_HTML"));
        out.println("</p>");

        for (int i = 0; i < PARAM_LIST.length; i++) {
            String paramName = resources.getHTML(PARAM_LIST[i]);
            String paramDefn = resources.getString
                ("Parameters.Definition." + PARAM_LIST[i] +"_HTML");
            out.print("<p>");
            out.print(resources.format("Parameters.Item_FMT",
                                       "<span>"+paramName+"</span>",
                                       paramDefn));
            out.println("</p>");
        }

        out.print("<p>");
        out.print(resources.getString("Parameters.Footer_HTML"));
        out.println("</p></body></html>");
    }
    public static String getParamsLink() {
        return getLink(URL + "?" + TUTORIAL_PARAM + "=" + PARAMS_TUT);
    }

    public static final String MANUAL_TUT = "Manual";
    public void writeManualTutorial() {
        String purpose = getParameter(PURPOSE_PARAM);
        String methodName = resources.format("Method_FMT", "D", purpose);
        writeTutorialHeader(methodName);
        out.println(resources.format("Manual_HTML_FMT", purpose));
        out.println("</body></html>");
    }
    public static String getManualLink(String purpose) {
        StringBuffer result = new StringBuffer();
        result.append(URL).append("?")
            .append(TUTORIAL_PARAM).append("=").append(MANUAL_TUT);
        addParam(result, PURPOSE_PARAM, purpose);
        return getLink(result.toString());
    }


    private static void addParam(StringBuffer url, String name, String val) {
        url.append("&").append(HTMLUtils.urlEncode(name))
            .append("=").append(HTMLUtils.urlEncode(val));
    }
    private static String getLink(String url) {
        return "<a href=\"" + url +
            "\" target='popup' onClick='popup();' class='plain'>";
    }

    static {
        String link = getParamsLink();
        for (int i = 0; i < PARAM_LIST.length; i++) {
            String param = PARAM_LIST[i];
            String paramName = resources.getHTML(param);
            annotator.addAnnotation(paramName, link + paramName + "</a>");
        }
        link = getOutlierLink();
        String outlierText = resources.getHTML("outlier");
        annotator.addAnnotation(outlierText, link + outlierText + "</a>");
    }

    public static String annotate(String html) {
        return annotator.markup(html);
    }
}
